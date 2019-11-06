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

package pl.fratik.dev.commands;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.*;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ShellCommand extends Command {
    public ShellCommand() {
        name = "shell";
        type = CommandType.DEBUG;
        permLevel = PermLevel.BOTOWNER;
        category = CommandCategory.SYSTEM;
    }

    @Override
    public boolean execute(@NotNull @Nonnull CommandContext context) {
        context.send("<a:loading:503651397049516053> Wykonuję...", message -> {
            try {
                Process process = new ProcessBuilder
                        (Arrays.asList(System.getenv("SHELL"), "-c", String.join(" ", context.getRawArgs())))
                        .redirectErrorStream(true).start();
                process.waitFor(15, TimeUnit.MINUTES);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder builder = new StringBuilder();
                if (reader.ready()) {
                    String line;
                    while ((line = reader.readLine()) != null) builder.append(line).append(System.lineSeparator());
                    reader.close();
                }
                process.destroyForcibly();
                String result = builder.toString();
                if (result.length() > 1993) result = result.substring(0, 1993);
                message.editMessage("```\n" + result + "```").queue();
            } catch (Exception e) {
                message.editMessage("Whoops! Coś nie pykło chyba: " + e.getMessage()).queue();
            }
        });
        return true;
    }
}
