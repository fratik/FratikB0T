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

package pl.fratik.fratikcoiny;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.fratikcoiny.commands.*;
import pl.fratik.fratikcoiny.entity.ChinczykStateDao;
import pl.fratik.fratikcoiny.entity.ChinczykStatsDao;
import pl.fratik.fratikcoiny.games.BlackjackCommand;
import pl.fratik.fratikcoiny.games.ChinczykCommand;
import pl.fratik.fratikcoiny.games.SlotsCommand;
import pl.fratik.fratikcoiny.libs.chinczyk.Chinczyk;

import java.util.ArrayList;

public class Module implements Modul {
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private NewManagerKomend managerKomend;
    @Inject private EventWaiter eventWaiter;
    @Inject private GuildDao guildDao;
    @Inject private MemberDao memberDao;
    @Inject private ShardManager shardManager;
    @Inject private EventBus eventBus;
    @Inject private Tlumaczenia tlumaczenia;
    private ArrayList<NewCommand> commands;
    private ChinczykStatsDao chinczykStatsDao;
    private ChinczykStateDao chinczykStateDao;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        chinczykStatsDao = new ChinczykStatsDao(managerBazyDanych, eventBus);
        chinczykStateDao = new ChinczykStateDao(managerBazyDanych, eventBus);
        commands = new ArrayList<>();

        commands.add(new DailyCommand(memberDao));
        commands.add(new DodajFcCommand(memberDao));
        commands.add(new UsunFcCommand(memberDao));
        commands.add(new GiveCommand(memberDao));
        commands.add(new KasaCommand(memberDao));
        commands.add(new SklepCommand(guildDao, memberDao, eventWaiter, shardManager, eventBus));
        commands.add(new SklepAdminCommand(guildDao, memberDao, eventWaiter, shardManager, eventBus));
        commands.add(new BlackjackCommand(memberDao, eventWaiter));
        commands.add(new SlotsCommand(memberDao));
        commands.add(new PremiaCommand(guildDao, memberDao, eventWaiter, eventBus));
        commands.add(new PremiaAdminCommand(guildDao, memberDao, eventWaiter, eventBus));
        if (Chinczyk.canBeUsed()) commands.add(new ChinczykCommand(shardManager, eventBus, eventWaiter, chinczykStatsDao, chinczykStateDao, tlumaczenia));

        managerKomend.registerCommands(this, commands);

        return true;
    }

    @Override
    public boolean shutDown() {
        managerKomend.unregisterCommands(commands);
        return true;
    }
}
