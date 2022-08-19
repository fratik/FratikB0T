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
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.util.DurationUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.ReasonUtils;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class WarnCommand extends ModerationCommand {

    private final CaseDao caseDao;

    public WarnCommand(CaseDao caseDao) {
        super(true);
        this.caseDao = caseDao;
        name = "warn";
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        permissions.add(Permission.BAN_MEMBERS);
        permissions.add(Permission.KICK_MEMBERS);
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("uzytkownik", "member");
        hmap.put("powod", "string");
        hmap.put("[...]", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false, false});
        aliases = new String[] {"ostrzez", "dajostrzezenie"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String powod;
        Member uzytkownik = (Member) context.getArgs()[0];
        if (context.getArgs().length > 1 && context.getArgs()[1] != null)
            powod = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                    .map(e -> e == null ? "" : e).map(Objects::toString).collect(Collectors.joining(uzycieDelim));
        else powod = context.getTranslated("warn.reason.default");
        if (uzytkownik.equals(context.getMember())) {
            context.reply(context.getTranslated("warn.cant.warn.yourself"));
            return false;
        }
        if (uzytkownik.getUser().isBot()) {
            context.reply(context.getTranslated("warn.no.bot"));
            return false;
        }
        if (uzytkownik.isOwner()) {
            context.reply(context.getTranslated("warn.cant.warn.owner"));
            return false;
        }
        if (!context.getMember().canInteract(uzytkownik)) {
            context.reply(context.getTranslated("warn.user.cant.interact"));
            return false;
        }
        if (!context.getGuild().getSelfMember().canInteract(uzytkownik)) {
            context.reply(context.getTranslated("warn.bot.cant.interact"));
            return false;
        }
        TemporalAccessor timestamp = Instant.now();
        Case aCase = new Case.Builder(uzytkownik, timestamp, Kara.WARN).setIssuerId(context.getSender().getIdLong()).build();
        DurationUtil.Response durationResp;
        try {
            durationResp = DurationUtil.parseDuration(powod);
        } catch (IllegalArgumentException e) {
            context.reply(context.getTranslated("warn.max.duration"));
            return false;
        }
        powod = durationResp.getTekst();
        aCase.setValidTo(durationResp.getDoKiedy());
        ReasonUtils.parseFlags(aCase, powod);
        caseDao.createNew(null, aCase, false, context.getTextChannel(), context.getLanguage());
        context.reply(context.getTranslated("warn.success", UserUtil.formatDiscrim(uzytkownik),
                WarnUtil.countCases(caseDao.getCasesByMember(uzytkownik), uzytkownik.getId())));
        return true;
    }
}
