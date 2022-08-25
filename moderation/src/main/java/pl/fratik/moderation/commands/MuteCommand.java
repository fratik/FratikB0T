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
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.listeners.ModLogListener;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;

public class MuteCommand extends ModerationCommand {
    private final ModLogListener modLogListener;

    public MuteCommand(ModLogListener modLogListener) {
        super(true);
        this.modLogListener = modLogListener;
        name = "mute";
        usage = "<osoba:user> [powod:string]";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String powod;
        Instant muteDo;
        Member uzytkownik = context.getArguments().get("osoba").getAsMember();
        if (uzytkownik == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }
        if (uzytkownik.equals(context.getMember())) {
            context.replyEphemeral(context.getTranslated("mute.cant.mute.yourself"));
            return;
        }
        if (uzytkownik.getUser().isBot()) {
            context.replyEphemeral(context.getTranslated("mute.no.bot"));
            return;
        }
        if (uzytkownik.isOwner()) {
            context.replyEphemeral(context.getTranslated("mute.cant.mute.owner"));
            return;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("mute.cant.interact"));
            return;
        }
        if (uzytkownik.isTimedOut()) {
            context.replyEphemeral(context.getTranslated("mute.already.muted"));
            return;
        }
        powod = context.getArgumentOr("powod", context.getTranslated("mute.reason.default"), OptionMapping::getAsString);
        DurationUtil.Response durationResp;
        try {
            durationResp = DurationUtil.parseDurationForMute(powod);
        } catch (IllegalArgumentException e) {
            context.replyEphemeral(context.getTranslated("mute.max.duration"));
            return;
        }
        powod = durationResp.getTekst();
        muteDo = durationResp.getDoKiedy();
        context.defer(false);
        Case aCase = new Case.Builder(uzytkownik, Instant.now(), Kara.MUTE)
                .setIssuerId(context.getSender().getIdLong()).build();
        ReasonUtils.parseFlags(aCase, powod);
        aCase.setValidTo(muteDo);
        modLogListener.getKnownCases().put(ModLogListener.generateKey(uzytkownik), aCase);
        try {
            context.getGuild().timeoutUntil(uzytkownik, muteDo).complete();
            context.sendMessage(context.getTranslated("mute.success", UserUtil.formatDiscrim(uzytkownik)));
        } catch (Exception ignored) {
            context.sendMessage(context.getTranslated("mute.fail"));
        }
    }
}
