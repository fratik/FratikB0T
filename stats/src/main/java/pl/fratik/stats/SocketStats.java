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

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.api.SocketAdapter;
import pl.fratik.api.SocketManager;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.event.CommandDispatchedEvent;
import pl.fratik.core.util.NamedThreadFactory;
import pl.fratik.stats.entity.CommandCountStats;
import pl.fratik.stats.entity.GuildCountStats;

import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static pl.fratik.core.util.CommonUtil.round;

public class SocketStats implements SocketAdapter {
    private final Module stats;
    private final GuildStats guildStats;
    private final ShardManager shardManager;
    private final Cache<List<GuildCountStats>> cacheGuilds;
    private final Cache<List<CommandCountStats>> cacheCommands;
    private final Set<SocketManager.Connection> subscribedConnections = new HashSet<>();
    private final ScheduledExecutorService executor;

    public SocketStats(Module stats, GuildStats guildStats, ShardManager shardManager, RedisCacheManager rcm) {
        this.stats = stats;
        this.guildStats = guildStats;
        this.shardManager = shardManager;
        cacheGuilds = rcm.new CacheRetriever<List<GuildCountStats>>(){}.getCache();
        cacheCommands = rcm.new CacheRetriever<List<CommandCountStats>>(){}.getCache();
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("SocketStats"));
        executor.scheduleAtFixedRate(() -> {
            long free = Runtime.getRuntime().freeMemory();
            long total = Runtime.getRuntime().totalMemory();
            updateSocketStats("memoryUsage", round((double) (total - free) / 1024 / 1024, 2, RoundingMode.HALF_UP));
        }, 15, 15, TimeUnit.SECONDS);
    }

    private void updateSocketStats(String changedField, Object content) {
        Set<SocketManager.Connection> cons = new HashSet<>(subscribedConnections);
        for (SocketManager.Connection con : cons)
            con.sendMessage(getChannelName(), changedField, content);
    }

    @Override
    public void subscribe(SocketManager.Connection connection) {
        subscribedConnections.add(connection);
    }

    @Override
    public void unsubscribe(SocketManager.Connection connection) {
        subscribedConnections.remove(connection);
    }

    private void updateGuildCount() {
        updateSocketStats("servers", shardManager.getGuilds().size());
        //todo średnia
    }

    private void updateTextCount() {
        updateSocketStats("text", shardManager.getTextChannels().size());
    }

    private void updateVoiceCount() {
        updateSocketStats("voice", shardManager.getVoiceChannels().size());
    }

    private void updateCommandCount() {
        //todo
    }

    private void updateMemberCount() {
        updateSocketStats("members",
                shardManager.getGuilds().stream().map(Guild::getMemberCount).reduce(Integer::sum).orElse(0));
    }

    @Subscribe public void onNewGuild(GuildJoinEvent e) { updateGuildCount(); }
    @Subscribe public void onLeftGuild(GuildLeaveEvent e) { updateGuildCount(); }
    @Subscribe public void onTextChannelCreate(ChannelCreateEvent e) {
        if (e.getChannelType() == ChannelType.TEXT) updateTextCount();
    }
    @Subscribe public void onTextChannelDelete(ChannelCreateEvent e) {
        if (e.getChannelType() == ChannelType.TEXT) updateTextCount();
    }
    @Subscribe public void onVoiceChannelCreate(ChannelCreateEvent e) {
        if (e.getChannelType() == ChannelType.VOICE) updateTextCount();
    }
    @Subscribe public void onChannelDelete(ChannelCreateEvent e) {
        if (e.getChannelType() == ChannelType.VOICE) updateTextCount();
    }
    @Subscribe public void onCommand(CommandDispatchedEvent e) { updateCommandCount(); }
    @Subscribe public void onMemberJoin(GuildMemberJoinEvent e) { updateMemberCount(); }
    @Subscribe public void onMemberLeave(GuildMemberRemoveEvent e) { updateMemberCount(); }

    @Override
    public String getChannelName() {
        return "stats";
    }

    @Override
    protected void finalize() throws Throwable {
        executor.shutdown();
    }
}
