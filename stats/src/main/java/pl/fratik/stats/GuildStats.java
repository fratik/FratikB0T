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

package pl.fratik.stats;

import io.undertow.server.RoutingHandler;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.LoggerFactory;
import pl.fratik.api.SocketManager;
import pl.fratik.api.entity.Exceptions;
import pl.fratik.api.internale.Exchange;
import pl.fratik.core.Statyczne;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.stats.entity.CommandCountStats;
import pl.fratik.stats.entity.GuildCountStats;
import pl.fratik.stats.entity.MembersStats;
import pl.fratik.stats.entity.MessagesStats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GuildStats {

    private final Module stats;
    private final Modul api;
    private final ShardManager shardManager;
    private final Cache<List<MembersStats>> cacheMms;
    private final Cache<List<MessagesStats>> cacheMsgs;
    private final Cache<List<GuildCountStats>> cacheGuilds;
    private final Cache<List<CommandCountStats>> cacheCommands;
    private SocketStats adapter;

    GuildStats(Module stats, Modul api, ShardManager shardManager, RedisCacheManager redisCacheManager) {
        this.stats = stats;
        this.api = api;
        this.shardManager = shardManager;
        cacheMms = redisCacheManager.new CacheRetriever<List<MembersStats>>(){}.getCache();
        cacheMsgs = redisCacheManager.new CacheRetriever<List<MessagesStats>>(){}.getCache();
        cacheGuilds = redisCacheManager.new CacheRetriever<List<GuildCountStats>>(){}.getCache();
        cacheCommands = redisCacheManager.new CacheRetriever<List<CommandCountStats>>(){}.getCache();
        RoutingHandler routes;
        try {
            routes = ((pl.fratik.api.Module) api).getRoutes();
            SocketManager socketManager = ((pl.fratik.api.Module) api).getSocketManager();
            adapter = new SocketStats(stats, shardManager, redisCacheManager);
            socketManager.registerAdapter(adapter);
        } catch (Exception | NoClassDefFoundError e) {
            LoggerFactory.getLogger(GuildStats.class).error("Nie udało się doczepić do modułu api!", e);
            return;
        }
        routes.get("/api/{guildId}/stats", ex -> {
            String guildId = Exchange.pathParams().pathParam(ex, "guildId").orElse(null);
            boolean wszystkieDane = Exchange.queryParams().queryParamAsBoolean(ex, "wszystkieDane")
                    .orElse(false);
            if (guildId == null || guildId.isEmpty()) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_PARAM);
                return;
            }
            Guild guild = null;
            try {
                guild = shardManager.getGuildById(guildId);
            } catch (Exception ignored) {/*lul*/}
            if (guild == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_GUILD);
                return;
            }
            List<MembersStats> mss = getAllMembersStatsFor(guildId);
            List<MessagesStats> msgs = getAllMessagesStatsFor(guildId);
            if (!wszystkieDane && mss.size() >= 30) {
                mss.sort(Comparator.comparingLong(MembersStats::getDate).reversed());
                mss = mss.subList(0, 29);
                msgs.sort(Comparator.comparingLong(MessagesStats::getDate).reversed());
                msgs = msgs.subList(0, 29);
            }
            Exchange.body().sendJson(ex, new Wrappers.GuildStatsWrapper(guildId, mss, msgs));
        });
        routes.get("/api/stats", ex -> {
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
            boolean wszystkieDane = Exchange.queryParams().queryParamAsBoolean(ex, "wszystkieDane")
                    .orElse(false);
            if (kesz != null) {
                tmpGcs = kesz;
                if (!wszystkieDane) {
                    tmpGcs = new ArrayList<>();
                    kesz.sort(Comparator.comparingLong(GuildCountStats::getDate).reversed());
                    for (int i = 0; i < kesz.size(); i++) {
                        tmpGcs.add(kesz.get(i));
                        if (i == 29) break;
                    }
                }
            } else {
                tmpGcs = stats.getGuildCountStatsDao().getAll();
                cacheGuilds.put(szard.getSelfUser().getId(), tmpGcs);
                if (!wszystkieDane)
                    tmpGcs = stats.getGuildCountStatsDao().getAll(30);
            }
            if (kesz2 != null) {
                tmpCcs = kesz2;
                if (!wszystkieDane) {
                    tmpCcs = new ArrayList<>();
                    kesz2.sort(Comparator.comparingLong(CommandCountStats::getDate).reversed());
                    for (int i = 0; i < kesz2.size(); i++) {
                        tmpCcs.add(kesz2.get(i));
                        if (i == 29) break;
                    }
                }
            } else {
                tmpCcs = stats.getCommandCountStatsDao().getAll();
                cacheCommands.put(szard.getSelfUser().getId(), tmpCcs);
                if (!wszystkieDane)
                    tmpCcs = stats.getCommandCountStatsDao().getAll(30);
            }
            List<Wrappers.GuildCountWrapper> gcs = tmpGcs.stream().map(Wrappers.GuildCountWrapper::new)
                    .sorted(Comparator.comparingLong(Wrappers.GuildCountWrapper::getDate).reversed()).peek(o -> {
                        if (o.getDate() == stats.getCurrentStorageDate()) o.setCount(shardManager.getGuilds().size());
                    }).collect(Collectors.toList());
            List<Wrappers.CommandStatsWrapper> ccs = tmpCcs.stream().map(Wrappers.CommandStatsWrapper::new)
                    .sorted(Comparator.comparingLong(Wrappers.CommandStatsWrapper::getDate).reversed()).peek(cecees -> {
                        if (cecees.getDate() == stats.getCurrentStorageDate())
                            cecees.setCount(cecees.getCount() + stats.getKomend());
                    }).collect(Collectors.toList());
            int guildsSummmary = 0;
            int commandsSummary = 0;
            int lastGuildCount = 0;
            for (int i = 0; i < gcs.size(); i++) {
                if (i > 0) {
                    guildsSummmary += lastGuildCount - gcs.get(i).count;
                }
                lastGuildCount = gcs.get(i).count;
                commandsSummary += CommonUtil.supressException(ccs::get, i) == null ? 0 : ccs.get(i).count;
                if (i == 29) break;
            }
            Wrappers.Stats srats = new Wrappers.Stats(guilds, (double) guildsSummmary / Math.min(gcs.size(), 30), ccs.get(0).count,
                    commandsSummary, (double) commandsSummary / Math.min(ccs.size(), 30), members,
                    textChannels, voiceChannels,
                    Instant.now().toEpochMilli() - (Statyczne.startDate.toInstant().getEpochSecond() * 1000));
            srats.guilds.addAll(gcs);
            srats.commands.addAll(ccs);
            Exchange.body().sendJson(ex, srats);
        });
    }

    private List<MembersStats> getAllMembersStatsFor(String guildId) {
        Guild g = shardManager.getGuildById(guildId);
        if (g == null) throw new IllegalArgumentException("nie ma takiego serwera");
        List<MembersStats> list = new ArrayList<>();
        for (MembersStats o : cacheMms.get(guildId, s -> stats.getMembersStatsDao().getAllForGuild(guildId))) {
            if (o.getDate() == stats.getCurrentStorageDate()) o.setCount(g.getMemberCount());
            list.add(o);
        }
        return list;
    }

    private List<MessagesStats> getAllMessagesStatsFor(String guildId) {
        Guild g = shardManager.getGuildById(guildId);
        if (g == null) throw new IllegalArgumentException("nie ma takiego serwera");
        List<MessagesStats> list = new ArrayList<>();
        for (MessagesStats o : cacheMsgs.get(guildId, s -> stats.getMessagesStatsDao().getAllForGuild(guildId))) {
            if (o.getDate() == stats.getCurrentStorageDate()) {
                o = new MessagesStats(o.getUniqid(), o.getDate(), o.getGuildId(), o.getCount());
                o.setCount(stats.getWiadomosci().getOrDefault(o.getGuildId(), 0) + o.getCount());
            }
            list.add(o);
        }
        return list;
    }

    public void shutdown() {
        if (adapter != null) {
            try {
                SocketManager socketManager = ((pl.fratik.api.Module) api).getSocketManager();
                socketManager.unregisterAdapter(adapter);
            } catch (Exception | NoClassDefFoundError e) {
                LoggerFactory.getLogger(GuildStats.class).error("Nie udało się odczepić od modułu api!", e);
            }
        }
    }

    @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection", "unused", "squid:S1068"})
    public static class Wrappers {
        private static class GuildStatsWrapper {
            private final String guildId;
            private final List<MembersStatsWrapper> members = new ArrayList<>();
            private final List<MessagesStatsWrapper> messages = new ArrayList<>();

            GuildStatsWrapper(String guildId, List<MembersStats> mss, List<MessagesStats> msgs) {
                this.guildId = guildId;
                mss.forEach(o -> members.add(new MembersStatsWrapper(o)));
                msgs.forEach(o -> messages.add(new MessagesStatsWrapper(o)));
            }
        }

        private static class MembersStatsWrapper {
            private final long date;
            private final int count;

            MembersStatsWrapper(MembersStats ms) {
                date = ms.getDate();
                count = ms.getCount();
            }
        }

        private static class MessagesStatsWrapper {
            private final long date;
            private final int count;

            MessagesStatsWrapper(MessagesStats ms) {
                date = ms.getDate();
                count = ms.getCount();
            }
        }

        public static class GuildCountWrapper {
            @Getter private final long date;
            @Getter @Setter private int count;

            public GuildCountWrapper(GuildCountStats ms) {
                date = ms.getDate();
                count = ms.getCount();
            }
        }

        public static class CommandStatsWrapper {
            @Getter private final long date;
            @Getter @Setter private int count;

            public CommandStatsWrapper(CommandCountStats ms) {
                date = ms.getDate();
                count = ms.getCount();
            }
        }

        @Data
        public static class Stats {
            private final int servers;
            private final double averageServers;
            private final int commandsToday;
            private final int commandsSummary;
            private final double averageCommands;
            private final List<Wrappers.GuildCountWrapper> guilds = new ArrayList<>();
            private final List<Wrappers.CommandStatsWrapper> commands = new ArrayList<>();
            private final int members;
            private final int text;
            private final int voice;
            private final long uptime;
            private final double memoryUsage;
            private final String jdaVersion;
            private final String coreVersion;
            private final String jVersion;

            @SuppressWarnings("squid:S00107")
            public Stats(int servers, double averageServers, int commandsToday, int commandsSummary, double averageCommands, int members, int text, int voice, long uptime) {
                this.servers = servers;
                this.averageServers = averageServers;
                this.commandsToday = commandsToday;
                this.commandsSummary = commandsSummary;
                this.averageCommands = averageCommands;
                this.members = members;
                this.text = text;
                this.voice = voice;
                this.uptime = uptime;
                long free = Runtime.getRuntime().freeMemory();
                long total = Runtime.getRuntime().totalMemory();
                memoryUsage = round((double) (total - free) / 1024 / 1024);
                jdaVersion = JDAInfo.VERSION;
                coreVersion = Statyczne.CORE_VERSION;
                jVersion = System.getProperty("java.version");
            }

            private static double round(double value) {
                BigDecimal bd = BigDecimal.valueOf(value);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                return bd.doubleValue();
            }
        }

    }
}
