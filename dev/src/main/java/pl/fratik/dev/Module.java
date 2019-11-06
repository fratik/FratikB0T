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

package pl.fratik.dev;

import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.dev.commands.*;

import java.util.ArrayList;

public class Module implements Modul {
    @Inject private ManagerKomend managerKomend;
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private GuildDao guildDao;
    @Inject private MemberDao memberDao;
    @Inject private UserDao userDao;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private ShardManager shardManager;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private ManagerModulow managerModulow;
    private ArrayList<Command> commands;

    @Override
    public boolean startUp() {
        Globals.ownerId = Long.parseLong(Ustawienia.instance.devs);
        Globals.owner = shardManager.retrieveUserById(Globals.ownerId).complete().getAsTag();

        commands = new ArrayList<>();

        commands.add(new EvalCommand(managerKomend, managerArgumentow, managerBazyDanych, managerModulow, shardManager, tlumaczenia, guildDao, userDao, memberDao));
        commands.add(new ShellCommand());
        commands.add(new ReloadCommand(managerModulow));
        commands.add(new LoadCommand(managerModulow));
        commands.add(new UnloadCommand(managerModulow));
        commands.add(new ModulesCommand(managerModulow, managerKomend));

        commands.forEach(managerKomend::registerCommand);

        return true;
    }

    @Override
    public boolean shutDown() {
        commands.forEach(managerKomend::unregisterCommand);
        return true;
    }
}
