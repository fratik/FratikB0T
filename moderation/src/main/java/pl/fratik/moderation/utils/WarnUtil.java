/*
 * Copyright (C) 2019 FratikB0T Contributors
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

package pl.fratik.moderation.utils;

import io.sentry.Sentry;
import io.sentry.event.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseBuilder;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.listeners.ModLogListener;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WarnUtil {
    private WarnUtil() {}

    public static synchronized void takeAction(GuildDao guildDao, CasesDao casesDao, Member member, MessageChannel channel, Language lang, Tlumaczenia tlumaczenia, ManagerKomend managerKomend) {
        CaseRow cr = casesDao.get(member.getGuild());
        List<Case> mcases = cr.getCases().stream().filter(c -> c.getUserId().equals(member.getUser().getId())).collect(Collectors.toList());
        List<Case> warnCases = mcases.stream().filter(c -> c.getType().equals(Kara.WARN)).collect(Collectors.toList());
        List<Case> unwarnCases = mcases.stream().filter(c -> c.getType().equals(Kara.UNWARN)).collect(Collectors.toList());
        // powody sie nie liczą, ważne by była dobra liczba
        try {
            if (!unwarnCases.isEmpty()) {
                warnCases.subList(0, unwarnCases.size()).clear();
            }
        } catch (Exception e) {
            Sentry.getContext().setUser(new User(member.getId(), member.getUser().getAsTag(), null, null));
            Sentry.getContext().addExtra("warnCases", warnCases);
            Sentry.getContext().addExtra("unwarnCases", unwarnCases);
            Sentry.capture(e);
            Sentry.clearContext();
            String prefix = managerKomend.getPrefixes(member.getGuild()).get(0);
            channel.sendMessage(tlumaczenia.get(lang, "modlog.take.action.unexpected.error", prefix, prefix)).queue();
            return;
        }
        GuildConfig gc = guildDao.get(member.getGuild());
        if (warnCases.size() == gc.getWarnyNaKick()) {
            Case nc = new CaseBuilder(member.getGuild()).setKara(Kara.KICK)
                    .setTimestamp(Instant.now()).setUser(member.getUser()).createCase();
            nc.setIssuerId(member.getJDA().getSelfUser());
            nc.setReason(tlumaczenia.get(lang, "modlog.auto.kick.reason", warnCases.size()));
            List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(member.getGuild(), new ArrayList<>());
            caseList.add(nc);
            ModLogListener.getKnownCases().put(member.getGuild(), caseList);
            boolean errored = false;
            try {
                member.getGuild().kick(member).reason(tlumaczenia.get(lang,
                        "modlog.auto.kick.audit.reason", warnCases.size())).complete();
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.kick.notice",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        warnCases.size(), managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            } catch (Exception e) {
                errored = true;
            }
            if (errored) {
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.kick.cant",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        warnCases.size(), managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
                return;
            }
        }
        if (warnCases.size() == gc.getWarnyNaTymczasowegoBana()) {
            Case nc = new CaseBuilder(member.getGuild()).setKara(Kara.TIMEDBAN)
                    .setTimestamp(Instant.now()).setUser(member.getUser()).createCase();
            nc.setIssuerId(member.getJDA().getSelfUser());
            nc.setReason(tlumaczenia.get(lang, "modlog.auto.tempban.reason", warnCases.size()));
            Calendar cal = Calendar.getInstance();
            cal.setTime(Date.from(Instant.from(Objects.requireNonNull(nc.getTimestamp()))));
            cal.add(Calendar.DAY_OF_MONTH, gc.getDlugoscTymczasowegoBanaZaWarny());
            nc.setValidTo(cal.toInstant());
            List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(member.getGuild(), new ArrayList<>());
            caseList.add(nc);
            ModLogListener.getKnownCases().put(member.getGuild(), caseList);
            boolean errored = false;
            try {
                member.getGuild().ban(member, 0).reason(tlumaczenia.get(lang,
                        "modlog.auto.tempban.audit.reason", warnCases.size(),
                        gc.getDlugoscTymczasowegoBanaZaWarny())).complete();
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.tempban.notice",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        gc.getDlugoscTymczasowegoBanaZaWarny(), warnCases.size(),
                        managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            } catch (Exception e) {
                errored = true;
            }
            if (errored) {
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.tempban.cant",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        gc.getDlugoscTymczasowegoBanaZaWarny(), warnCases.size(),
                        managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            }
        }
        if (warnCases.size() == gc.getWarnyNaBan()) {
            Case nc = new CaseBuilder(member.getGuild()).setKara(Kara.BAN)
                    .setTimestamp(Instant.now()).setUser(member.getUser()).createCase();
            nc.setIssuerId(member.getJDA().getSelfUser());
            nc.setReason(tlumaczenia.get(lang, "modlog.auto.ban.reason", warnCases.size()));
            List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(member.getGuild(), new ArrayList<>());
            caseList.add(nc);
            ModLogListener.getKnownCases().put(member.getGuild(), caseList);
            boolean errored = false;
            try {
                member.getGuild().ban(member, 0).reason(tlumaczenia.get(lang,
                        "modlog.auto.ban.audit.reason", warnCases.size())).complete();
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.ban.notice",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        warnCases.size(), managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            } catch (Exception e) {
                errored = true;
            }
            if (errored) {
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.ban.cant",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        warnCases.size(), managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            }
        }
    }
}
