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

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;

import java.time.temporal.ChronoUnit;

public class PingCommand extends Command {

    public PingCommand() {
        name = "ping";
        category = CommandCategory.BASIC;
        permLevel = PermLevel.EVERYONE;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message message = context.getChannel().sendMessage(context.getTranslated("ping.pinging")).complete();
        long ping = context.getEvent().getMessage().getTimeCreated().until(message.getTimeCreated(), ChronoUnit.MILLIS);
        message.editMessage(context.getTranslated("ping.delay", ping, context.getMessage().getJDA().getGatewayPing())).complete();
        return true;
    }
}
