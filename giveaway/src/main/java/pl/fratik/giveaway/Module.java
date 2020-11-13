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

package pl.fratik.giveaway;

import com.google.inject.Inject;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.GiveawayDao;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.giveaway.commands.GiveawayCommand;
import pl.fratik.giveaway.listener.GiveawayListener;

import java.util.ArrayList;

public class Module implements Modul {

    @Inject private ManagerKomend managerKomend;
    @Inject private EventWaiter eventWaiter;
    @Inject private ShardManager shardManager;
    @Inject private GiveawayDao giveawayDao;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private ManagerArgumentow managerArgumentow;
    private ArrayList<Command> commands;

    private GiveawayListener listener = new GiveawayListener(shardManager, giveawayDao, tlumaczenia);

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        commands = new ArrayList<>();
        listener.setStartup(true);
        commands.add(new GiveawayCommand(eventWaiter, managerArgumentow, listener));

        return true;
    }

    @Override
    public boolean shutDown() {
        commands.forEach(managerKomend::unregisterCommand);
        listener.setStartup(false);
        return true;
    }

}
