/*
 * Copyright (C) 2019 FratikB0T Contributors
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

package pl.fratik.beta;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import pl.fratik.core.event.ConnectedEvent;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.moduly.Modul;

public class Module implements Modul {
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private EventBus eventBus;
    @Inject private ShardManager shardManager;
    private BetaDao betaDao;

    public boolean startUp() {
        betaDao = new BetaDao(managerBazyDanych, eventBus);
        eventBus.register(this);
        if (shardManager.getShardsRunning() == shardManager.getShardsTotal()) onConnectEvent(new ConnectedEvent() {});
        return true;
    }

    @Subscribe
    private void onConnectEvent(ConnectedEvent connectedEvent) {
        for (Guild g : shardManager.getGuilds())
            if (!betaDao.get(g.getId()).isAllow()) g.leave().queue();
    }

    @Subscribe
    @AllowConcurrentEvents
    private void onGuildJoin(GuildJoinEvent e) {
        if (!betaDao.get(e.getGuild().getId()).isAllow()) e.getGuild().leave().queue();
    }

    public boolean shutDown() {
        eventBus.unregister(this);
        return true;
    }
}