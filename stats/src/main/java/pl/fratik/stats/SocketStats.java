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

import io.socket.client.Ack;
import io.socket.emitter.Emitter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.api.SocketAdapter;
import pl.fratik.api.SocketEvent;
import pl.fratik.core.Statyczne;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.stats.entity.CommandCountStats;
import pl.fratik.stats.entity.GuildCountStats;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SocketStats implements SocketAdapter {
    private final Module stats;
    private final ShardManager shardManager;
    private final Cache<List<GuildCountStats>> cacheGuilds;
    private final Cache<List<CommandCountStats>> cacheCommands;

    public SocketStats(Module stats, ShardManager shardManager, RedisCacheManager rcm) {
        this.stats = stats;
        this.shardManager = shardManager;
        cacheGuilds = rcm.new CacheRetriever<List<GuildCountStats>>(){}.getCache();
        cacheCommands = rcm.new CacheRetriever<List<CommandCountStats>>(){}.getCache();
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
}
