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

package pl.fratik.liczek;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.*;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.liczek.commands.LiczekCommand;

import java.util.ArrayList;

public class Module implements Modul {

    @Inject private ManagerKomend managerKomend;
    @Inject private GuildDao guildDao;
    @Inject private EventBus eventBus;
    @Inject private Tlumaczenia tlumaczenia;
    private ArrayList<Command> commands;

    private LiczekListener listener;
    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        commands = new ArrayList<>();

        commands.add(new LiczekCommand(guildDao, listener));

        listener = new LiczekListener(guildDao, tlumaczenia);
        eventBus.register(listener);

        commands.forEach(managerKomend::registerCommand);
        return true;
    }

    @Override
    public boolean shutDown() {
        commands.forEach(managerKomend::unregisterCommand);
        return true;
    }

}
