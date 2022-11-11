/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.listeners.ModLogListener;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class BanCommand extends ModerationCommand {

    private final ModLogListener modLogListener;

    public BanCommand(ModLogListener modLogListener) {
        super(true);
        this.modLogListener = modLogListener;
        name = "ban";
        usage = "<osoba:user> [powod:string]";
        permissions = DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String powod = context.getArgumentOr("powod", context.getTranslated("ban.reason.default"), OptionMapping::getAsString);
        User uzytkownik = context.getArguments().get("osoba").getAsUser();
        if (uzytkownik.equals(context.getSender())) {
            context.replyEphemeral(context.getTranslated("ban.cant.ban.yourself"));
            return;
        }
        context.defer(false);
        Member uzMem;
        try {
            uzMem = context.getGuild().retrieveMemberById(uzytkownik.getId()).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) uzMem = null;
            else throw e;
        }
        if (uzMem != null) {
            if (uzMem.isOwner()) {
                context.sendMessage(context.getTranslated("ban.cant.ban.owner"));
                return;
            }
            if (!context.getMember().canInteract(uzMem)) {
                context.sendMessage(context.getTranslated("ban.cant.interact"));
                return;
            }
        }
        try {
            context.getGuild().retrieveBan(uzytkownik).complete();
            context.sendMessage(context.getTranslated("ban.already.banned"));
            return;
        } catch (ErrorResponseException e) {
            // użytkownik nie ma bana
        }
        DurationUtil.Response durationResp;
        try {
            durationResp = DurationUtil.parseDuration(powod);
        } catch (IllegalArgumentException e) {
            context.sendMessage(context.getTranslated("ban.max.duration"));
            return;
        }
        powod = durationResp.getTekst();
        Instant banDo = durationResp.getDoKiedy();
        Case aCase = new Case.Builder(context.getGuild(), uzytkownik, Instant.now(), Kara.BAN).build();
        aCase.setIssuerId(context.getSender().getIdLong());
        ReasonUtils.parseFlags(aCase, powod);
        aCase.setValidTo(banDo);
        modLogListener.getKnownCases().put(ModLogListener.generateKey(uzytkownik, context.getGuild()), aCase);
        try {
            context.getGuild().ban(uzytkownik, 0, TimeUnit.MILLISECONDS).reason(aCase.getReason(context)).complete();
            context.sendMessage(context.getTranslated("ban.success", UserUtil.formatDiscrim(uzytkownik)));
        } catch (HierarchyException e) {
            context.sendMessage(context.getTranslated("ban.failed.hierarchy"));
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("ban.failed"));
        }
    }
}
