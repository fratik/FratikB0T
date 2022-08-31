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

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EmojifyCommand extends NewCommand {
    public EmojifyCommand() {
        name = "emojify";
        cooldown = 10;
        usage = "<tekst:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String str = context.getArguments().get("tekst").getAsString();
        String res = Arrays.stream(str.split(" ")).map(this::replaceChars)
                .collect(Collectors.joining("\n"));
        if (!str.matches("^[a-zA-Z ]+$")) {
            context.reply(context.getTranslated("emojify.regex"));
            return;
        }
        if (res.length() >= 2000) {
            context.reply(context.getTranslated("emojify.toolong"));
            return;
        }
        context.reply(res.trim());
    }

    private String replaceChars(String slowo) {
        StringBuilder res = new StringBuilder();
        for (char literka : slowo.toLowerCase().toCharArray()) {
            res.append(":regional_indicator_").append(literka).append(":").append("\u200b");
        }
        return res.toString();
    }
}
