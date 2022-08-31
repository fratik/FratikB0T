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

package pl.fratik.dev.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.UserUtil;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ShellCommand extends NewCommand {
    public ShellCommand() {
        name = "shell";
        usage = "<pokaz:bool> <kod:string>";
        type = CommandType.SUPPORT_SERVER;
        permissions = DefaultMemberPermissions.DISABLED;
    }

    @Override
    public void execute(@NotNull @Nonnull NewCommandContext context) {
        context.deferAsync(!context.getArguments().get("pokaz").getAsBoolean());
        try {
            Process process = new ProcessBuilder
                    (Arrays.asList(System.getenv("SHELL"), "-c", context.getArguments().get("kod").getAsString()))
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
            if (result.length() > Message.MAX_CONTENT_LENGTH - 7) result = result.substring(0, Message.MAX_CONTENT_LENGTH - 7);
            context.sendMessage("```\n" + result + "```");
        } catch (Exception e) {
            context.sendMessage("Whoops! Coś nie pykło chyba: " + e.getMessage());
        }
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!UserUtil.isBotOwner(context.getSender().getIdLong())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return false;
        }
        return true;
    }
}
