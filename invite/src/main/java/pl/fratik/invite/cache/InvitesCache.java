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

package pl.fratik.invite.cache;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;

import java.util.List;

public class InvitesCache {

    private final RedisCacheManager redisCacheManager;
    private final ShardManager api;
    public Cache<FakeInvite> inviteCache;

    public InvitesCache(RedisCacheManager rcm, ShardManager api) {
        this.redisCacheManager = rcm;
        this.api = api;
        this.inviteCache = rcm.new CacheRetriever<FakeInvite>(){}.getCache(-1);
    }

    public synchronized void load() {
        for (Guild guild : api.getGuilds()) {
            List<Invite> invites = guild.retrieveInvites().complete();
            for (Invite invite : invites) {
                inviteCache.put(guild.getId() + "." + invite.getCode(), new FakeInvite(invite));
            }
        }
    }

    @Subscribe
    public void createInvite(GuildInviteCreateEvent e) {
        inviteCache.put(e.getGuild().getId() + "." + e.getCode(), new FakeInvite(e.getInvite()));
    }

    @Subscribe
    public void createInvite(GuildInviteDeleteEvent e) {
        inviteCache.invalidate(e.getGuild().getId() + "." + e.getCode());
    }

}
