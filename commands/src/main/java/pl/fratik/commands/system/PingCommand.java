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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;

import java.time.temporal.ChronoUnit;

public class PingCommand extends NewCommand {

    public PingCommand() {
        name = "ping";
        allowInDMs = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        InteractionHook hook = context.reply(context.getTranslated("ping.pinging"));
        long ping = hook.getInteraction().getTimeCreated().until(hook.retrieveOriginal().complete().getTimeCreated(), ChronoUnit.MILLIS);
        context.editOriginal(context.getTranslated("ping.delay", ping, context.getSender().getJDA().getGatewayPing()));
    }
}
