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

package pl.fratik.api;

import io.sentry.Sentry;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Statyczne;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.stats.GuildStats;
import pl.fratik.stats.Module;
import pl.fratik.stats.entity.CommandCountStats;
import pl.fratik.stats.entity.GuildCountStats;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SocketManager {

    private static final Logger logger = LoggerFactory.getLogger(SocketManager.class);

    private final ShardManager shardManager;
    private final Module stats;

    private final Cache<List<GuildCountStats>> cacheGuilds;
    private final Cache<List<CommandCountStats>> cacheCommands;

    private final Map<String, Method> events = new HashMap<>();

    private Socket socket = null;

    public SocketManager(ShardManager shardManager, RedisCacheManager redisCacheManager, Module stats) {
        this.shardManager = shardManager;
        this.stats = stats;
        cacheGuilds = redisCacheManager.new CacheRetriever<List<GuildCountStats>>(){}.getCache();
        cacheCommands = redisCacheManager.new CacheRetriever<List<CommandCountStats>>(){}.getCache();
        for (Method method : getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(SocketEvent.class)) {
                    SocketEvent socketEvent = method.getAnnotation(SocketEvent.class);
                    String name = socketEvent.eventName().isEmpty() ? method.getName() : socketEvent.eventName();
                    if (events.containsKey(name)) {
                        logger.error("Nazwa {} jest zdublowana!", name);
                        continue;
                    }
                    events.put(name, method);
                }
            } catch (Exception e) {
                logger.error("Nie udało się zarejestrować eventu", e);
                Sentry.capture(e);
            }
        }
        logger.info("Zarejestrowano {} socket eventów!", events.size());
    }

    public void start() {
        if (socket != null) socket.disconnect();
        try {
            socket = IO.socket(Ustawienia.instance.socketAdress).open();
            socket.io().reconnectionDelay(5_000);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    connect(this, (Ack) args[args.length - 1]);
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    disconnect(this, (Ack) args[args.length - 1]);
                }
            });

            socket.on(Socket.EVENT_MESSAGE, args -> {
                try {
                    Method method = events.get(args[0]);
                    if (method == null) {
                        logger.warn("Nie znalazłem handlera dla eventu " + method);
                        return;
                    }
                    method.invoke(args[0], args[1]);
                } catch (Exception ex) {
                    logger.error("Wystąpił błąd przy odbieraniu socketa", ex);
                    Sentry.capture(ex);
                }
            });

        } catch (Exception e) {
            Sentry.capture(e);
            logger.error("Nie udało się skonfigurować socketa!", e);
        }
    }

    @SocketEvent
    public void connect(Emitter.Listener e, Ack ack) {
        logger.info("Połączono do serwera socketów");
        socket.emit("registerFbot");
    }

    @SocketEvent
    public void disconnect(Emitter.Listener e, Ack ack) {
        logger.warn("Odłączono od serwera socketów");
    }

    @SocketEvent
    public void retrieveStats(Emitter.Listener e, Ack ack) {
        int guilds = shardManager.getGuilds().size();
        int members = shardManager.getGuilds().stream().map(Guild::getMemberCount).reduce(Integer::sum)
                .orElse(0);
        int textChannels = shardManager.getTextChannels().size();
        int voiceChannels = shardManager.getVoiceChannels().size();
        List<GuildCountStats> tmpGcs;
        List<CommandCountStats> tmpCcs;
        JDA szard = shardManager.getShardById(0);
        if (szard == null) throw new IllegalStateException("bot nie załadowany poprawnie");
        List<GuildCountStats> kesz = cacheGuilds.getIfPresent(szard.getSelfUser().getId());
        List<CommandCountStats> kesz2 = cacheCommands.getIfPresent(szard.getSelfUser().getId());
        if (kesz != null) {
            tmpGcs = new ArrayList<>();
            kesz.sort(Comparator.comparingLong(GuildCountStats::getDate).reversed());
            for (int i = 0; i < kesz.size(); i++) {
                tmpGcs.add(kesz.get(i));
                if (i == 29) break;
            }
        } else {
            tmpGcs = stats.getGuildCountStatsDao().getAll();
            cacheGuilds.put(szard.getSelfUser().getId(), tmpGcs);
        }
        if (kesz2 != null) {
            tmpCcs = kesz2;
        } else {
            tmpCcs = stats.getCommandCountStatsDao().getAll();
            cacheCommands.put(szard.getSelfUser().getId(), tmpCcs);
        }
        List<GuildStats.Wrappers.GuildCountWrapper> gcs = tmpGcs.stream().map(GuildStats.Wrappers.GuildCountWrapper::new)
                .sorted(Comparator.comparingLong(GuildStats.Wrappers.GuildCountWrapper::getDate).reversed()).peek(o -> {
                    if (o.getDate() == stats.getCurrentStorageDate()) o.setCount(shardManager.getGuilds().size());
                }).collect(Collectors.toList());
        List<GuildStats.Wrappers.CommandStatsWrapper> ccs = tmpCcs.stream().map(GuildStats.Wrappers.CommandStatsWrapper::new)
                .sorted(Comparator.comparingLong(GuildStats.Wrappers.CommandStatsWrapper::getDate).reversed()).peek(cecees -> {
                    if (cecees.getDate() == stats.getCurrentStorageDate())
                        cecees.setCount(cecees.getCount() + stats.getKomend());
                }).collect(Collectors.toList());
        int guildsSummmary = 0;
        int commandsSummary = 0;
        int lastGuildCount = 0;
        for (int i = 0; i < gcs.size(); i++) {
            if (i > 0) {
                guildsSummmary += lastGuildCount - gcs.get(i).getCount();
            }
            lastGuildCount = gcs.get(i).getCount();
            commandsSummary += CommonUtil.supressException(ccs::get, i) == null ? 0 : ccs.get(i).getCount();
            if (i == 29) break;
        }
        GuildStats.Wrappers.Stats srats = new GuildStats.Wrappers.Stats(guilds, (double) guildsSummmary / Math.min(gcs.size(), 30), ccs.get(0).getCount(),
                commandsSummary, (double) commandsSummary / Math.min(ccs.size(), 30), members,
                textChannels, voiceChannels,
                Instant.now().toEpochMilli() - (Statyczne.startDate.toInstant().getEpochSecond() * 1000));

        srats.getGuilds().addAll(gcs);
        srats.getCommands().addAll(ccs);
        ack.call(GsonUtil.toJSON(srats));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SocketEvent {
        String eventName() default "";
    }

}
