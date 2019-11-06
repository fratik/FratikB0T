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

package pl.fratik.core.manager;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.Emoji;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ManagerKomend {
    void registerCommand(Command command);
    void unregisterCommand(Command command);
    void unregisterAll();
    Emoji getReakcja(User user, boolean success);
    List<String> getPrefixes(Guild guild);
    Map<String, Command> getCommands();
    Set<Command> getRegistered();
    Map<String, Integer> getRegisteredPerModule();
    void shutdown();
}
