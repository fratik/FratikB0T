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

package pl.fratik.invite;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.ConnectedEvent;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.commands.*;
import pl.fratik.invite.entity.InviteData;
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
    @Inject private Tlumaczenia tlumaczenia;

    private ArrayList<Command> commands;
    private JoinListener joinListener;
    private InvitesCache invitesCache;
    private InviteDao inviteDao;

    private final Logger logger = LoggerFactory.getLogger(Module.class);

    @Override
    public boolean startUp() {
        eventBus.register(this);
        inviteDao = new InviteDao(managerBazyDanych, eventBus);

        this.invitesCache = new InvitesCache(redisCacheManager, guildDao, shardManager);
        this.joinListener = new JoinListener(inviteDao, invitesCache, guildDao, redisCacheManager, tlumaczenia);

        eventBus.register(invitesCache);
        eventBus.register(joinListener);

        commands = new ArrayList<>();
        commands.add(new InvitesCommand(inviteDao, invitesCache, guildDao, managerArgumentow, eventWaiter, eventBus));
        commands.add(new TopInvitesCommand(inviteDao, invitesCache, eventWaiter, eventBus));

        commands.forEach(managerKomend::registerCommand);

        if (shardManager.getShards().stream().anyMatch(s -> !s.getStatus().equals(JDA.Status.CONNECTED))) return true;

        invitesCache.load();

        return true;
    }

    @Subscribe
    public void onConnectedEvent(ConnectedEvent e) {
        if (!invitesCache.isLoading() && !invitesCache.isLoaded()) invitesCache.load();
    }

    @Override
    public boolean shutDown() {
        eventBus.unregister(this);
        eventBus.unregister(joinListener);
        commands.forEach(managerKomend::unregisterCommand);
        invitesCache.getInviteCache().invalidateAll();
        return true;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTo().equals("invite")) return;
        if (!e.getMessage().startsWith("Module-")) return;
        if (e.getMessage().startsWith("Module-getInviteData:")) {
            try {
                String id = e.getMessage().replace("Module-getInviteData:", "");
                InviteData inv = inviteDao.get(id);
                e.setResponse(inv);
            } catch (Exception err) {
                logger.error("Wystąpił błąd w wysyłaniu odpowiedzi!", err);
                Sentry.capture(new EventBuilder().withMessage(err.getMessage()).withExtra("message", e.getMessage())
                        .withLevel(Event.Level.ERROR).withSentryInterface(new ExceptionInterface(err)));
            }
        }
        logger.info("Nierozpoznana wiadomość: {}!", e.getMessage());
    }
}
