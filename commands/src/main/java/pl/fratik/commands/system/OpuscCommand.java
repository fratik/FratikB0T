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

package pl.fratik.commands.system;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;

public class OpuscCommand extends Command {
    public OpuscCommand() {
        name = "opusc";
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.SYSTEM;
        aliases = new String[] {"koniec"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (context.getGuild().getId().equals(Ustawienia.instance.botGuild)) {
            context.send(context.getTranslated("opusc.cantleave"));
            return false;
        }
        context.send(context.getTranslated("opusc.leaving"), m -> context.getGuild().leave().queue());
        return true;
    }
}
