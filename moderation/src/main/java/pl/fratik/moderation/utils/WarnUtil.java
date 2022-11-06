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

package pl.fratik.moderation.utils;

import com.google.common.collect.MapMaker;
import io.sentry.Sentry;
import io.sentry.event.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import pl.fratik.core.Globals;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.listeners.ModLogListener;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class WarnUtil {
    private static final Map<String, ReentrantLock> locks = new MapMaker().weakValues().makeMap();
    // weak — kiedy ReentrantLock przestanie być używany, może być śmiało usunięty

    private WarnUtil() {}

    public static void takeAction(ModLogListener listener, GuildDao guildDao, CaseDao caseDao, Member member, MessageChannel channel, Language lang, Tlumaczenia tlumaczenia) {
        ReentrantLock lock = locks.computeIfAbsent(member.getGuild().getId() + "." + member.getId(), i -> new ReentrantLock());
        lock.lock();
        try {
            int cases = countCases(caseDao.getCasesByMember(member), member.getId());
            if (cases < 0) {
                channel.sendMessage(tlumaczenia.get(lang, "modlog.take.action.unexpected.error")).queue();
                return;
            }
            GuildConfig gc = guildDao.get(member.getGuild());
            if (cases == gc.getWarnyNaKick()) {
                Case nc = new Case.Builder(member, Instant.now(), Kara.KICK).setIssuerId(Globals.clientId)
                        .setReasonKey("modlog.auto.kick.reason", Integer.toString(cases)).build();
                listener.getKnownCases().put(ModLogListener.generateKey(member), nc);
                boolean errored = false;
                try {
                    member.getGuild().kick(member).reason(tlumaczenia.get(lang, "modlog.auto.kick.audit.reason", cases)).complete();
                    channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.kick.notice",
                            member.getUser().getAsTag(), cases)).complete();
                } catch (Exception e) {
                    errored = true;
                }
                if (errored) {
                    channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.kick.cant",
                            member.getUser().getAsTag(), cases)).complete();
                    return;
                }
            }
            if (cases == gc.getWarnyNaTymczasowegoBana()) {
                Case nc = new Case.Builder(member, Instant.now(), Kara.BAN).setIssuerId(Globals.clientId)
                        .setReasonKey("modlog.auto.tempban.reason", Integer.toString(cases)).build();
                Calendar cal = Calendar.getInstance();
                cal.setTime(Date.from(Instant.from(Objects.requireNonNull(nc.getTimestamp()))));
                cal.add(Calendar.DAY_OF_MONTH, gc.getDlugoscTymczasowegoBanaZaWarny());
                nc.setValidTo(cal.toInstant());
                listener.getKnownCases().put(ModLogListener.generateKey(member), nc);
                boolean errored = false;
                try {
                    member.getGuild().ban(member.getUser(), 0, TimeUnit.MILLISECONDS).reason(tlumaczenia.get(lang,
                            "modlog.auto.tempban.audit.reason", cases,
                            gc.getDlugoscTymczasowegoBanaZaWarny())).complete();
                    channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.tempban.notice",
                            member.getUser().getAsTag(),
                            gc.getDlugoscTymczasowegoBanaZaWarny(), cases)).complete();
                } catch (Exception e) {
                    errored = true;
                }
                if (errored) {
                    channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.tempban.cant",
                            member.getUser().getAsTag(),
                            gc.getDlugoscTymczasowegoBanaZaWarny(), cases)).complete();
                }
            }
            if (cases == gc.getWarnyNaBan()) {
                Case nc = new Case.Builder(member, Instant.now(), Kara.BAN).setIssuerId(Globals.clientId)
                        .setReasonKey("modlog.auto.ban.reason", Integer.toString(cases)).build();
                listener.getKnownCases().put(ModLogListener.generateKey(member), nc);
                boolean errored = false;
                try {
                    member.getGuild().ban(member, 0, TimeUnit.MILLISECONDS).reason(tlumaczenia.get(lang,
                            "modlog.auto.ban.audit.reason", cases)).complete();
                    channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.ban.notice",
                            member.getUser().getAsTag(), cases)).complete();
                } catch (Exception e) {
                    errored = true;
                }
                if (errored) {
                    channel.sendMessage(tlumaczenia.get(lang, "modlog.auto.ban.cant",
                            member.getUser().getAsTag(), cases)).complete();
                }
                if (gc.isResetujOstrzezeniaPrzyBanie()) {
                    Case ca = new Case.Builder(member, nc.getTimestamp(), Kara.UNWARN).setIleRazy(cases)
                            .setIssuerId(Globals.clientId).setReasonKey("modlog.auto.reset").build();
                    caseDao.createNew(null, ca, false);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static int countCases(List<Case> mcases, String userId) {
        int cases = 0;
        List<Case> warnCases = mcases.stream().filter(c -> c.getType().equals(Kara.WARN)).collect(Collectors.toList());
        List<Case> unwarnCases = mcases.stream().filter(c -> c.getType().equals(Kara.UNWARN)).collect(Collectors.toList());
        // powody sie nie liczą, ważne by była dobra liczba
        for (Case c : warnCases) cases += c.getIleRazy();
        for (Case c : unwarnCases) cases -= c.getIleRazy();
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
