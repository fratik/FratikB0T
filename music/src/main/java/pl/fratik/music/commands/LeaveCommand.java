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

package pl.fratik.music.commands;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.music.managers.NowyManagerMuzyki;

public class LeaveCommand extends MusicCommand {
    private final NowyManagerMuzyki managerMuzyki;

    public LeaveCommand(NowyManagerMuzyki managerMuzyki) {
        this.managerMuzyki = managerMuzyki;
        name = "leave";
        requireConnection = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        managerMuzyki.getManagerMuzykiSerwera(context.getGuild()).disconnect();
        context.reply(context.getTranslated("leave.success"));
    }
}
