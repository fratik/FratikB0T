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
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ReasonUtils;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

public class WarnCommand extends ModerationCommand {

    private final CaseDao caseDao;

    public WarnCommand(CaseDao caseDao) {
        super(true);
        this.caseDao = caseDao;
        name = "warn";
        usage = "<osoba:user> [powod:string]";
        permissions = DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS, Permission.BAN_MEMBERS);
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Member uzytkownik = context.getArguments().get("osoba").getAsMember();
        String powod = context.getArgumentOr("powod", context.getTranslated("warn.reason.default"), OptionMapping::getAsString);
        if (uzytkownik == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }
        if (uzytkownik.equals(context.getMember())) {
            context.replyEphemeral(context.getTranslated("warn.cant.warn.yourself"));
            return;
        }
        if (uzytkownik.getUser().isBot()) {
            context.replyEphemeral(context.getTranslated("warn.no.bot"));
            return;
        }
        if (uzytkownik.isOwner()) {
            context.replyEphemeral(context.getTranslated("warn.cant.warn.owner"));
            return;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("warn.user.cant.interact"));
            return;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.replyEphemeral(context.getTranslated("warn.bot.cant.interact"));
            return;
        }
        context.defer(false);
        TemporalAccessor timestamp = Instant.now();
        Case aCase = new Case.Builder(uzytkownik, timestamp, Kara.WARN).setIssuerId(context.getSender().getIdLong()).build();
        DurationUtil.Response durationResp;
        try {
            durationResp = DurationUtil.parseDuration(powod);
        } catch (IllegalArgumentException e) {
            context.sendMessage(context.getTranslated("warn.max.duration"));
            return;
        }
        powod = durationResp.getTekst();
        aCase.setValidTo(durationResp.getDoKiedy());
        ReasonUtils.parseFlags(aCase, powod);
        caseDao.createNew(null, aCase, false, context.getChannel(), context.getLanguage());
        context.sendMessage(context.getTranslated("warn.success", UserUtil.formatDiscrim(uzytkownik),
                WarnUtil.countCases(caseDao.getCasesByMember(uzytkownik), uzytkownik.getId())));
    }
}
