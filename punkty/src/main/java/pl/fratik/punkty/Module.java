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

package pl.fratik.punkty;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.punkty.entity.PunktyDao;
import pl.fratik.punkty.komendy.*;

import java.util.ArrayList;

public class Module implements Modul {
    @Inject private NewManagerKomend managerKomend;
    @Inject private EventBus eventBus;
    @Inject private EventWaiter eventWaiter;
    @Inject private GuildDao guildDao;
    @Inject private MemberDao memberDao;
    @Inject private UserDao userDao;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private ShardManager shardManager;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private RedisCacheManager redisCacheManager;
    private ArrayList<NewCommand> commands;

    private LicznikPunktow licznik;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        PunktyDao punktyDao = new PunktyDao(managerBazyDanych, shardManager, eventBus);
        licznik = new LicznikPunktow(guildDao, userDao, punktyDao, managerKomend, eventBus, tlumaczenia, shardManager, redisCacheManager);
        commands = new ArrayList<>();

        commands.add(new StatsCommand(licznik));
        commands.add(new LvlupCommand(licznik));
        commands.add(new RankingCommand(punktyDao, licznik, memberDao, eventBus, eventWaiter));
        commands.add(new GlobalstatyCommand());
        commands.add(new GurCommand(eventWaiter, shardManager, eventBus));
        commands.add(new GsrCommand(eventWaiter, shardManager, eventBus));
        commands.add(new SyncCommand(guildDao));
        commands.add(new NukepointsCommand(eventWaiter, guildDao, licznik, punktyDao));

        managerKomend.registerCommands(this, commands);

        eventBus.register(licznik);
        return true;
    }

    @Override
    public boolean shutDown() {
        managerKomend.unregisterCommands(commands);
        licznik.shutdown();
        try { eventBus.unregister(licznik); } catch (IllegalArgumentException ignored) {/*lul*/}
        return true;
    }
}
