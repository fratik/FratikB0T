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

package pl.fratik.starboard;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.komendy.*;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Module implements Modul {
    @Inject private ManagerKomend managerKomend;
    @Inject private EventBus eventBus;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private GuildDao guildDao;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private EventWaiter eventWaiter;

    private ArrayList<Command> commands;
    private ExecutorService executor;
    private StarManager starManager;
    private StarboardListener starboardListener;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        executor = Executors.newSingleThreadExecutor();
        StarDataDao starDataDao = new StarDataDao(managerBazyDanych, eventBus);
        starManager = new StarManager(starDataDao, eventBus);
        starboardListener = new StarboardListener(starDataDao, tlumaczenia, starManager, executor);
        commands = new ArrayList<>();

        commands.add(new FixstarCommand(starManager, starDataDao));
        commands.add(new StarboardCommand(starDataDao));
        commands.add(new StarThresholdCommand(starDataDao));
        commands.add(new TopStarCommand(starDataDao, starManager, eventWaiter, eventBus));
        commands.add(new StarInfoCommand(starDataDao, starManager));
        commands.add(new StarRankingCommand(starDataDao, eventWaiter, eventBus));

        commands.forEach(managerKomend::registerCommand);

        eventBus.register(starboardListener);
        return true;
    }

    @Override
    public boolean shutDown() {
        commands.forEach(managerKomend::unregisterCommand);
        executor.shutdown();
        try {
            eventBus.unregister(starboardListener);
            eventBus.unregister(starManager);
        } catch (IllegalArgumentException ignored) {/*lul*/}
        return true;
    }
}
