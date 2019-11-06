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

public class RzutMonetaCommand extends Command {
    private static final Random random = new Random();

    public RzutMonetaCommand() {
        name = "rzutmoneta";
        category = CommandCategory.FUN;
        aliases = new String[] {"rzutm", "moneta", "rm"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Moneta moneta;
        switch (random.nextInt(2)) {
            case 1:
                moneta = Moneta.RESZKA;
                break;
            case 0:
            default:
                moneta = Moneta.ORZEL; //prosty fallback
        }
        context.send(context.getTranslated("rzutmoneta.response", context.getSender().getAsMention(), context.getTranslated("rzutmoneta." + moneta.name().toLowerCase())));
        return true;
    }

    private enum Moneta {
        ORZEL, RESZKA
    }
}
