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

package pl.fratik.commands.zabawa;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.StringUtil;

public class PasswordCommand extends NewCommand {

    public PasswordCommand() {
        name = "password";
        usage = "[znaki:int]";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        Integer arg = context.getArgumentOr("znaki", 9, OptionMapping::getAsInt);
        if (arg > 32 || arg <= 0) {
            context.replyEphemeral(context.getTranslated("password.length"));
            return;
        }
        String haslo;
        haslo = StringUtil.generateId(arg, true, true, true, true);
        if (haslo.isEmpty()) {
            context.replyEphemeral(context.getTranslated("password.empty", "\uD83E\uDD14"));
            return;
        }
        context.replyEphemeral(context.getTranslated("password.msg", "\uD83D\uDD11", "\uD83D\uDD11", haslo));
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("znaki")) {
            option.setRequiredRange(1, 32);
        }
    }
}
