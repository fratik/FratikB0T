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
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;

import java.util.Random;

public class RzutKosciaCommand extends Command {
    private static final Random random = new Random();

    public RzutKosciaCommand() {
        name = "rzutkoscia";
        category = CommandCategory.FUN;
        aliases = new String[] {"rzutk", "kostka", "rk", "rool", "roll"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        context.send(context.getTranslated("rzutkoscia.response", "\uD83D\uDC4C", context.getSender().getAsMention(),
                String.valueOf(random.nextInt(6) + 1)));
        return true;
    }
}
