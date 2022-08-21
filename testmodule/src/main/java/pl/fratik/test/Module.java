/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.test;

import com.google.inject.Inject;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.util.EventWaiter;

import java.util.ArrayList;

public class Module implements Modul {
    @Inject private NewManagerKomend managerKomend;
    @Inject private EventWaiter eventWaiter;
    private ArrayList<NewCommand> commands;

    @Override
    public boolean startUp() {
        commands = new ArrayList<>();

        commands.add(new PingCommand(eventWaiter));
        commands.add(new ArgCommand());

        managerKomend.registerCommands(this, commands);

        return true;
    }

    @Override
    public boolean shutDown() {
        managerKomend.unregisterCommands(commands);
        return true;
    }
}