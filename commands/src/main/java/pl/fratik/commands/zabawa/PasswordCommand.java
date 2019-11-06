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

import net.dv8tion.jda.api.entities.PrivateChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.StringUtil;

public class PasswordCommand extends Command {

    public PasswordCommand() {
        name = "password";
        category = CommandCategory.FUN;
        aliases = new String[] {"haslo", "wygenerujhaslo"};
        uzycie = new Uzycie("znaki", "integer", false);
        cooldown = 4;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Integer arg;
        if (context.getArgs().length != 0 && context.getArgs()[0] != null)
            arg = (Integer) context.getArgs()[0];
        else arg = 9;
        if (arg > 32 || arg <= 0) {
            context.send(context.getTranslated("password.length"));
            return false;
        }
        String haslo;
        haslo = StringUtil.generateId(arg, true, true, true, true);
        if (haslo.isEmpty()) {
            context.send(context.getTranslated("password.empty", "\uD83E\uDD14"));
            return false;
        }
        try {
            PrivateChannel c = context.getMember().getUser().openPrivateChannel().complete();
            c.sendMessage(context.getTranslated("password.msg", "\uD83D\uDD11", "\uD83D\uDD11", haslo))
                    .complete();
            context.send(context.getTranslated("password.sent"));
            return true;
        } catch (Exception e) {
            context.send(context.getTranslated("password.cantmessage"));
            return false;
        }
    }
}
