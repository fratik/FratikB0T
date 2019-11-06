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

package pl.fratik.logs;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;

public class Module implements Modul {
    @Inject private EventBus eventBus;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private EventWaiter eventWaiter;
    @Inject private ManagerKomend managerKomend;
    @Inject private ShardManager shardManager;
    @Inject private ManagerModulow managerModulow;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private GuildDao guildDao;

    private LogManager logManager;
    private Thread shutdownThread;

    @Override
    public boolean startUp() {
        logManager = new LogManager(shardManager, guildDao, tlumaczenia);
        shutdownThread = new Thread(logManager::shutdown);
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        eventBus.register(logManager);
        return true;
    }

    @Override
    public boolean shutDown() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
        } catch (Exception e) {
            //lul
        }
        eventBus.unregister(logManager);
        return true;
    }
}
