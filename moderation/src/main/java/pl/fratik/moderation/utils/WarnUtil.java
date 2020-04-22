/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
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
        int cases = countCases(cr, member.getId());
        if (cases < 0) {
            String prefix = managerKomend.getPrefixes(member.getGuild()).get(0);
            channel.sendMessage(tlumaczenia.get(lang, "modlog.take.action.unexpected.error", prefix, prefix)).queue();
            return;
        }
        GuildConfig gc = guildDao.get(member.getGuild());
        if (cases == gc.getWarnyNaKick()) {
            Case nc = new CaseBuilder(member.getGuild()).setKara(Kara.KICK)
                    .setTimestamp(Instant.now()).setUser(member.getUser()).createCase();
            nc.setIssuerId(member.getJDA().getSelfUser());
            nc.setReason(tlumaczenia.get(lang, "modlog.auto.kick.reason", cases));
            List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(member.getGuild(), new ArrayList<>());
            caseList.add(nc);
            ModLogListener.getKnownCases().put(member.getGuild(), caseList);
            boolean errored = false;
            try {
                member.getGuild().kick(member).reason(tlumaczenia.get(lang,
                        "modlog.auto.kick.audit.reason", cases)).complete();
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.kick.notice",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        cases, managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            } catch (Exception e) {
                errored = true;
            }
            if (errored) {
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.kick.cant",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        cases, managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
                return;
            }
        }
        if (cases == gc.getWarnyNaTymczasowegoBana()) {
            Case nc = new CaseBuilder(member.getGuild()).setTimestamp(Instant.now()).setKara(Kara.BAN)
                    .setUser(member.getUser()).createCase();
            nc.setIssuerId(member.getJDA().getSelfUser());
            nc.setReason(tlumaczenia.get(lang, "modlog.auto.tempban.reason", cases));
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
                        "modlog.auto.tempban.audit.reason", cases,
                        gc.getDlugoscTymczasowegoBanaZaWarny())).complete();
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.tempban.notice",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        gc.getDlugoscTymczasowegoBanaZaWarny(), cases,
                        managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            } catch (Exception e) {
                errored = true;
            }
            if (errored) {
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.tempban.cant",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        gc.getDlugoscTymczasowegoBanaZaWarny(), cases,
                        managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            }
        }
        if (cases == gc.getWarnyNaBan()) {
            Case nc = new CaseBuilder(member.getGuild()).setKara(Kara.BAN)
                    .setTimestamp(Instant.now()).setUser(member.getUser()).createCase();
            nc.setIssuerId(member.getJDA().getSelfUser());
            nc.setReason(tlumaczenia.get(lang, "modlog.auto.ban.reason", cases));
            List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(member.getGuild(), new ArrayList<>());
            caseList.add(nc);
            ModLogListener.getKnownCases().put(member.getGuild(), caseList);
            boolean errored = false;
            try {
                member.getGuild().ban(member, 0).reason(tlumaczenia.get(lang,
                        "modlog.auto.ban.audit.reason", cases)).complete();
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.ban.notice",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        cases, managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            } catch (Exception e) {
                errored = true;
            }
            if (errored) {
                channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.ban.cant",
                        member.getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"),
                        cases, managerKomend.getPrefixes(member.getGuild()).get(0))).complete();
            }
            if (gc.isResetujOstrzezeniaPrzyBanie()) {
                Case ca = new CaseBuilder(member.getGuild()).setKara(Kara.UNWARN)
                        .setTimestamp(Instant.now()).setUser(member.getUser()).setIleRazy(cases).createCase();
                ca.setIssuerId(member.getJDA().getSelfUser());
                ca.setReason(tlumaczenia.get(lang, "modlog.auto.reset"));
                TextChannel mlogchan = null;
                if (gc.getModLog() != null && !gc.getModLog().isEmpty())
                    mlogchan = channel.getJDA().getTextChannelById(gc.getModLog());
                if (mlogchan != null && mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                        Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
                    MessageEmbed embed = ModLogBuilder.generate(ca.getType(), member.getUser(),
                            member.getJDA().getSelfUser().getAsTag(), ca.getReason(), ca.getType().getKolor(),
                            ca.getCaseId(), ca.isValid(), ca.getValidTo(), ca.getTimestamp(), ca.getIleRazy(),
                            lang, member.getGuild());
                    mlogchan.sendMessage(embed).queue(message -> {
                        ca.setMessageId(message.getId());
                        CaseRow caseRow = casesDao.get(member.getGuild());
                        caseRow.getCases().add(ca);
                        casesDao.save(caseRow);
                    });
                } else {
                    CaseRow caseRow = casesDao.get(member.getGuild());
                    caseRow.getCases().add(ca);
                    casesDao.save(caseRow);
                }
            }
        }
    }

    public static int countCases(CaseRow cr, String userId) {
        List<Case> mcases = cr.getCases().stream().filter(c -> c.getUserId().equals(userId)).collect(Collectors.toList());
        int cases = 0;
        List<Case> warnCases = mcases.stream().filter(c -> c.getType().equals(Kara.WARN)).collect(Collectors.toList());
        List<Case> unwarnCases = mcases.stream().filter(c -> c.getType().equals(Kara.UNWARN)).collect(Collectors.toList());
        // powody sie nie liczą, ważne by była dobra liczba
        for (Case c : warnCases) {
            if (c.getIleRazy() == null) cases++;
            else cases += c.getIleRazy();
        }
        for (Case c : unwarnCases) {
            if (c.getIleRazy() == null) cases--;
            else cases -= c.getIleRazy();
        }
        if (cases < 0) {
            Sentry.getContext().setUser(new User(userId, null, null, null));
            Sentry.getContext().addExtra("warnCases", warnCases);
            Sentry.getContext().addExtra("unwarnCases", unwarnCases);
            Sentry.capture(new AssertionError("cases < 0"));
            Sentry.clearContext();
        }
        return cases;
    }
}
