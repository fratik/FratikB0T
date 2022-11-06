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

package pl.fratik.core.service;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractScheduledService;
import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.Akcja;
import pl.fratik.core.entity.Schedule;
import pl.fratik.core.entity.ScheduleDao;
import pl.fratik.core.event.ScheduleEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.StringUtil;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ScheduleService extends AbstractScheduledService {

    private final ShardManager shardManager;
    private final ScheduleDao scheduleDao;
    private final Tlumaczenia tlumaczenia;
    private final EventBus eventBus;

    public ScheduleService(ShardManager shardManager, ScheduleDao scheduleDao, Tlumaczenia tlumaczenia, EventBus eventBus) {
        this.shardManager = shardManager;
        this.scheduleDao = scheduleDao;
        this.tlumaczenia = tlumaczenia;
        this.eventBus = eventBus;
    }

    @Override
    protected void runOneIteration() {
        if (shardManager.getShards().stream().anyMatch(s -> s.getStatus() != JDA.Status.CONNECTED)) return;
        for (Schedule sch : scheduleDao.getAll()) {
            if (Instant.now().toEpochMilli() < sch.getData()) continue;
            try {
                if (sch.getAkcja() == Akcja.REMIND) {
                    Schedule.Przypomnienie przypomnienie = (Schedule.Przypomnienie) sch.getContent();
                    if (przypomnienie == null || przypomnienie.getOsoba() == null) continue;
                    User persona = shardManager.retrieveUserById(przypomnienie.getOsoba()).complete();
                    PrivateChannel dm = persona.openPrivateChannel().complete();
                    Language jezyk = tlumaczenia.getLanguage(persona);
                    StringBuilder sb = new StringBuilder(tlumaczenia.get(jezyk, "reminder.message",
                            StringUtil.escapeMarkdown(przypomnienie.getTresc()))).append("\n");
                    if (przypomnienie.getMurl().isEmpty()) {
                        sb.append(tlumaczenia.get(jezyk, "reminder.message.nodata"));
                    } else {
                        int dodano = 0;
                        for (String url : przypomnienie.getMurl()) {
                            sb.append("<").append(url).append(">");
                            dodano++;
                            if (dodano == 3) break;
                            if (przypomnienie.getMurl().indexOf(url) != przypomnienie.getMurl().size() - 1) {
                                sb.append("\n");
                            }
                        }
                    }
                    if (przypomnienie.getMurl().size() > 3) {
                        sb.append("\n").append(tlumaczenia.get(jezyk, "reminder.message.more",
                                przypomnienie.getMurl().size() - 3));
                    }
                    dm.sendMessage(sb.toString()).queue();
                }
                if (sch.getAkcja() == Akcja.EVENT) {
                    eventBus.post(new ScheduleEvent(sch.getScheduledBy(), sch.getContent()));
                }
            } catch (Exception e) {
                Sentry.capture(new EventBuilder().withLevel(Event.Level.ERROR).withMessage(e.getMessage())
                        .withExtra("schedule", sch).withSentryInterface(new ExceptionInterface(e)));
                LoggerFactory.getLogger(ScheduleService.class).error("Błąd w ScheduleService!", e);
                Sentry.capture(e);
            }
            scheduleDao.delete(String.valueOf(sch.getId()));
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(15, 15, TimeUnit.SECONDS);
    }
}
