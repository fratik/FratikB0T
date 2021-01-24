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

package pl.fratik.invite.cache;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.DatabaseUpdateEvent;

public class InvitesCache {

    private final ShardManager api;
    private final GuildDao guildDao;
    private final Cache<GuildConfig> gcCache;
    @Getter private final Cache<FakeInvite> inviteCache;
    @Getter private boolean loading = false;
    @Getter private boolean loaded = false;

    public InvitesCache(RedisCacheManager rcm, GuildDao guildDao, ShardManager api) {
        this.api = api;
        this.guildDao = guildDao;
        this.gcCache = rcm.new CacheRetriever<GuildConfig>(){}.getCache();
        this.inviteCache = rcm.new CacheRetriever<FakeInvite>(){}.getCache(-1);
    }

    public synchronized void load() {
        loading = true;
        for (Guild guild : api.getGuilds()) if (tracksInvites(guild)) load(guild);
        loading = false;
        loaded = true;
    }

    public synchronized void load(Guild guild) {
        try {
            guild.retrieveInvites().complete().forEach(this::load);
        } catch (InsufficientPermissionException ignored) { }
    }

    public synchronized void load(Invite invite) {
        @SuppressWarnings("ConstantConditions") // guild nie może być null
        String id = invite.getGuild().getId() + "." + invite.getCode();
        inviteCache.invalidate(id);
        inviteCache.put(id, new FakeInvite(invite));
    }

    @Subscribe
    public void createInvite(GuildInviteCreateEvent e) {
        load(e.getInvite());
    }

    @Subscribe
    @AllowConcurrentEvents
    public void deleteInvite(GuildInviteDeleteEvent e) {
        inviteCache.invalidate(e.getGuild().getId() + "." + e.getCode());
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent e) {
        if (e.getEntity() instanceof GuildConfig) {
            if (((GuildConfig) e.getEntity()).isLvlUpNotify()) {
                Guild g = api.getGuildById(((GuildConfig) e.getEntity()).getGuildId());
                if (g != null) load(g);
            }
        }
    }

    public boolean tracksInvites(Guild g) {
        return gcCache.get(g.getId(), guildDao::get).isTrackInvites();
    }

}
