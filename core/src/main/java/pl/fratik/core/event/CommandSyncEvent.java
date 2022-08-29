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

package pl.fratik.core.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Set;

@Getter
@ToString
@AllArgsConstructor
public class CommandSyncEvent {
    private final Set<CommandData> globalCommands;
    private final Set<CommandData> supportGuildCommands;
}
