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

package pl.fratik.core.service;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import io.sentry.Sentry;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.GbanDao;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.manager.*;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.GuildUtil;
import pl.fratik.core.util.UserUtil;

public class FratikB0TService extends AbstractIdleService {
    private final Logger logger;
    private final ManagerModulow moduleManager;
    private final ShardManager shardManager;

    @SuppressWarnings("squid:S00107")
    public FratikB0TService(ShardManager shardManager, EventBus eventBus, EventWaiter eventWaiter, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, ManagerBazyDanych managerBazyDanych, GuildDao guildDao, ManagerModulow moduleManager, GbanDao gbanDao) {
        logger = LoggerFactory.getLogger(getClass());

        this.shardManager = shardManager;
        this.moduleManager = moduleManager;

        eventBus.register(eventWaiter);
        eventBus.register(managerKomend);
        eventBus.register(managerBazyDanych);
        eventBus.register(moduleManager);
        eventBus.register(tlumaczenia);
        eventBus.register(new UserUtil());
        GuildUtil.setGuildDao(guildDao);
        GuildUtil.setGbanDao(gbanDao);
        GuildUtil.setShardManager(shardManager);
        UserUtil.setGbanDao(gbanDao);
        eventBus.register(new GuildUtil());
        eventBus.register(this);
    }

    @Override
    public void startUp() {
        try {
            logger.debug("Uruchamiam serwis...");
            moduleManager.loadModules();
        } catch (Exception e) {
            logger.error("Oops, coś się popsuło!", e);
            Sentry.capture(e);
            this.stopAsync();
            System.exit(1);
        }
    }

    @Override
    public void shutDown() {
        if (shardManager != null)
            shardManager.shutdown();
    }

    @Subscribe
    public void deadEventHandler(DeadEvent deadEvent) {
        logger.debug("[DEAD EVENT] klasa eventu: {}, źródło: {}", deadEvent.getEvent().getClass().getName(), deadEvent.getSource());
    }
}
