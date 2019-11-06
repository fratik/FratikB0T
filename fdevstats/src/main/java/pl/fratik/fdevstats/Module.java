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

package pl.fratik.fdevstats;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.ConnectedEvent;
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

    private ServiceManager service;
    private StatsService zerwajz;

    private static final Logger logger = LoggerFactory.getLogger(Module.class);

    @Override
    public boolean startUp() {
        Ustawienia.FdevStats ustst = Ustawienia.instance.fdevStats;
        if (ustst.status == null || ustst.uptime == null || ustst.wersja == null || ustst.ping == null ||
                ustst.users == null || ustst.serwery == null || ustst.ram == null || ustst.komdzis == null ||
                ustst.status.isEmpty() || ustst.uptime.isEmpty() || ustst.wersja.isEmpty() || ustst.ping.isEmpty() ||
                ustst.users.isEmpty() || ustst.serwery.isEmpty() || ustst.ram.isEmpty() || ustst.komdzis.isEmpty()) {
            logger.error("Co najmniej jedno ID kanałów ze statystykami jest nieprawidłowe");
            return false;
        }
        zerwajz = new StatsService(shardManager, managerModulow);
        eventBus.register(this);
        service = new ServiceManager(ImmutableList.of(zerwajz));
        service.startAsync();
        service.awaitHealthy();
        return true;
    }

    @Subscribe
    public void onConnectedEvent(ConnectedEvent e) {
        zerwajz.run();
    }

    @Override
    public boolean shutDown() {
        eventBus.unregister(this);
        service.stopAsync();
        service.awaitStopped();
        return true;
    }
}
