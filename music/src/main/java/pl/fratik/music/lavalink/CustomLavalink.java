/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.music.lavalink;

import com.google.common.eventbus.Subscribe;
import lavalink.client.LavalinkUtil;
import lavalink.client.io.Lavalink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class CustomLavalink extends Lavalink<CustomLink> {
    private final Function<Integer, JDA> jdaProvider;
    private final Logger log;
    private final CustomVoiceInterceptor voiceInterceptor;

    public CustomLavalink(ShardManager shardManager, String userId) {
        super(userId, shardManager.getShardsTotal());
        jdaProvider = shardManager::getShardById;
        log = LoggerFactory.getLogger(getClass());
        this.voiceInterceptor = new CustomVoiceInterceptor(this);
    }

    public CustomVoiceInterceptor getVoiceInterceptor() {
        return voiceInterceptor;
    }

    public CustomLink getLink(Guild guild) {
        return getLink(guild.getId());
    }

    public CustomLink getExistingLink(Guild guild) {
        return getExistingLink(guild.getId());
    }

    public JDA getJda(int shardId) {
        if (jdaProvider == null) throw new IllegalStateException("JDAProvider is not initialised!");

        JDA result = jdaProvider.apply(shardId);
        if (result == null) throw new IllegalStateException("JDAProvider returned null for shard " + shardId);

        return jdaProvider.apply(shardId);
    }

    public JDA getJdaFromSnowflake(String snowflake) {
        return getJda(LavalinkUtil.getShardFromSnowflake(snowflake, numShards));
    }

    @Override
    protected CustomLink buildNewLink(String guildId) {
        return new CustomLink(this, guildId);
    }
    
    @Subscribe
    public void onReconnect(SessionRecreateEvent e) {
        getLinksMap().forEach((guildId, link) -> {
            try {
                //Note: We also ensure that the link belongs to the JDA object
                if (link.getLastChannel() != null && e.getJDA().getGuildById(guildId) != null) {
                    AudioChannel ac = e.getJDA().getChannelById(AudioChannel.class, link.getLastChannel());
                    if (ac == null) return;
                    link.connect(ac, false);
                }
            } catch (Exception ex) {
                log.error("Caught exception while trying to reconnect link " + link, ex);
            }
        });
    }

    @Subscribe
    public void onGuildLeave(GuildLeaveEvent e) {
        CustomLink link = getLinksMap().get(e.getGuild().getId());
        if (link == null) return;

        link.removeConnection();
    }
}
