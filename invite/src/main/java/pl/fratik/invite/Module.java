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

package pl.fratik.invite;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.commands.*;
import pl.fratik.invite.entity.InviteDao;
import pl.fratik.invite.listeners.JoinListener;

import java.util.ArrayList;

public class Module implements Modul {
    @Inject private ManagerKomend managerKomend;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private EventBus eventBus;
    @Inject private EventWaiter eventWaiter;
    @Inject private RedisCacheManager redisCacheManager;
    @Inject private ShardManager shardManager;
    @Inject private GuildDao guildDao;
    @Inject private ManagerArgumentow managerArgumentow;

    private ArrayList<Command> commands;
    private JoinListener joinListener;
    private InvitesCache invitesCache;

    @Override
    public boolean startUp() {
        InviteDao inviteDao = new InviteDao(managerBazyDanych, eventBus);

        this.invitesCache = new InvitesCache(redisCacheManager, shardManager);
        this.joinListener = new JoinListener(inviteDao, invitesCache);
        invitesCache.load();

        eventBus.register(invitesCache);
        eventBus.register(joinListener);

        commands = new ArrayList<>();
        commands.add(new InvitesCommand(inviteDao, guildDao, managerArgumentow));
        commands.add(new TopInvitesCommnad(inviteDao, eventWaiter, eventBus));

        commands.forEach(managerKomend::registerCommand);
        return true;
    }

    @Override
    public boolean shutDown() {
        invitesCache.inviteCache.invalidateAll();
        eventBus.unregister(joinListener);
        commands.forEach(managerKomend::unregisterCommand);
        return true;
    }

}
