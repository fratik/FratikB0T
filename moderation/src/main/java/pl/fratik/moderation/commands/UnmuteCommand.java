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
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.listeners.ModLogListener;

public class UnmuteCommand extends ModerationCommand {

    private final ModLogListener modLogListener;

    public UnmuteCommand(ModLogListener modLogListener) {
        super(true);
        this.modLogListener = modLogListener;
        name = "unmute";
        usage = "<osoba:user> [powod:string]";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) { //todo przebudować
        Member uzytkownik = context.getArguments().get("osoba").getAsMember();
        String powod = context.getArgumentOr("powod", context.getTranslated("unmute.reason.default"), OptionMapping::getAsString);
        if (uzytkownik == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }
        if (uzytkownik.equals(context.getMember())) {
            context.replyEphemeral(context.getTranslated("unmute.cant.unmute.yourself"));
            return;
        }
        if (uzytkownik.isOwner()) {
            context.replyEphemeral(context.getTranslated("unmute.cant.unmute.owner"));
            return;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("unmute.cant.interact"));
            return;
        }
        if (!uzytkownik.isTimedOut()) {
            context.replyEphemeral(context.getTranslated("unmute.not.muted"));
            return;
        }
        context.defer(false);
//        Case aCase = new Case.Builder(uzytkownik, Instant.now(), Kara.UNMUTE)
//                .setIssuerId(context.getSender().getIdLong()).build();
//        ReasonUtils.parseFlags(aCase, powod);
//        modLogListener.getKnownCases().put(ModLogListener.generateKey(uzytkownik), aCase);
        try {
            uzytkownik.removeTimeout().complete();
            context.sendMessage(context.getTranslated("unmute.success", UserUtil.formatDiscrim(uzytkownik)));
        } catch (Exception ignored) {
            context.sendMessage(context.getTranslated("unmute.fail"));
        }
    }
}
