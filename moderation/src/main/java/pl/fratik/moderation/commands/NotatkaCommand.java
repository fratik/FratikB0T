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
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ReasonUtils;

import java.time.Instant;

public class NotatkaCommand extends ModerationCommand {

    private final CaseDao caseDao;

    public NotatkaCommand(CaseDao casesDao) {
        super(true);
        this.caseDao = casesDao;
        name = "notatka";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS);
        cooldown = 5;
        usage = "<osoba:user> <notatka:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member uzytkownik = context.getArguments().get("osoba").getAsMember();
        String powod = context.getArguments().get("notatka").getAsString();
        if (uzytkownik == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }
        if (uzytkownik.equals(context.getMember())) {
            context.replyEphemeral(context.getTranslated("notatka.cant.note.yourself"));
            return;
        }
        if (uzytkownik.isOwner()) {
            context.replyEphemeral(context.getTranslated("notatka.cant.note.owner"));
            return;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("notatka.user.cant.interact"));
            return;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("notatka.bot.cant.interact"));
            return;
        }
        context.defer(false);
        Case aCase = new Case.Builder(uzytkownik, Instant.now(), Kara.NOTATKA).setIssuerId(context.getSender().getIdLong()).build();
        ReasonUtils.parseFlags(aCase, powod);
        caseDao.createNew(null, aCase, false);
        context.sendMessage(context.getTranslated("notatka.success", UserUtil.formatDiscrim(uzytkownik)));
    }
}
