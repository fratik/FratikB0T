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

package pl.fratik.commands.zabawa;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OsiemBallCommand extends Command {
    private static final Random random = new Random();

    public OsiemBallCommand() {
        name = "8ball";
        category = CommandCategory.FUN;
        uzycie = new Uzycie("pytanie", "string", true);
        aliases = new String[] {"pytanie"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String[] odpowiedzi = context.getTranslated("8ball.responses").split(";");
        String odp = odpowiedzi[random.nextInt(odpowiedzi.length)];
        if (!((String) context.getArgs()[0]).endsWith("?")) {
            context.send(context.getTranslated("8ball.not.a.question"));
            return false;
        }
        context.send("\uD83E\uDD14", m -> m.editMessage(odp).queueAfter(3, TimeUnit.SECONDS));
        return true;
    }
}
