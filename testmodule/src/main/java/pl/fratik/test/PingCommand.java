/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.test;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.ButtonWaiter;
import pl.fratik.core.util.EventWaiter;

public class PingCommand extends NewCommand {
    private final EventWaiter eventWaiter;

    public PingCommand(EventWaiter eventWaiter) {
        this.eventWaiter = eventWaiter;
        name = "ping";
    }

    @Override
    public void execute(NewCommandContext context) {
        InteractionHook hook = context.defer(true);
        Message message = hook.retrieveOriginal().complete();
        context.sendMessage(new MessageBuilder("a").setActionRows(ActionRow.of(Button.success("XD", "XD"))).build());
        new ButtonWaiter(eventWaiter, context, hook.getInteraction(), ButtonWaiter.ResponseType.EDIT).create();
    }
}
