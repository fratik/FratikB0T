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

package pl.fratik.gwarny;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.gwarny.arguments.GadminArgument;
import pl.fratik.gwarny.commands.GwarnCommand;
import pl.fratik.gwarny.commands.GwarnlistCommand;
import pl.fratik.gwarny.commands.UngwarnCommand;
import pl.fratik.gwarny.entity.GwarnDao;

import java.util.ArrayList;

public class Module implements Modul {
    @Inject
    private ManagerKomend managerKomend;
    @Inject
    private ManagerArgumentow managerArgumentow;
    @Inject
    private EventWaiter eventWaiter;
    @Inject
    private GuildDao guildDao;
    @Inject
    private MemberDao memberDao;
    @Inject
    private UserDao userDao;
    @Inject
    private GbanDao gbanDao;
    @Inject
    private ScheduleDao scheduleDao;
    @Inject
    private ManagerBazyDanych managerBazyDanych;
    @Inject
    private ShardManager shardManager;
    @Inject
    private Tlumaczenia tlumaczenia;
    @Inject
    private ManagerModulow managerModulow;
    @Inject
    private EventBus eventBus;

    private ArrayList<Command> commands;
    private ArrayList<Argument> arguments;
    private GwarnDao gwarnDao;

    public Module() {
        arguments = new ArrayList<>();
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        gwarnDao = new GwarnDao(managerBazyDanych, eventBus);

        arguments = new ArrayList<>();
        commands = new ArrayList<>();

        arguments.add(new GadminArgument(shardManager));
        arguments.forEach(managerArgumentow::registerArgument);

        commands.add(new GwarnCommand(gwarnDao));
        commands.add(new GwarnlistCommand(gwarnDao, eventWaiter, eventBus));
        commands.add(new UngwarnCommand(gwarnDao));

        commands.forEach(managerKomend::registerCommand);
        return true;
    }

    @Override
    public boolean shutDown() {
        arguments.forEach(managerArgumentow::unregisterArgument);
        commands.forEach(managerKomend::unregisterCommand);
        return true;
    }
}
