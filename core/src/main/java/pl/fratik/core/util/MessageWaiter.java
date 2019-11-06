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

package pl.fratik.core.util;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import pl.fratik.core.command.CommandContext;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MessageWaiter {
    private final EventWaiter eventWaiter;
    private final CommandContext context;

    @Getter @Setter private Consumer<MessageReceivedEvent> messageHandler;
    @Getter @Setter private Runnable timeoutHandler;

    public MessageWaiter(EventWaiter eventWaiter, CommandContext context) {
        this.eventWaiter = eventWaiter;
        this.context = context;
    }

    public void create() {
        eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                this::handleMessage, 30, TimeUnit.SECONDS, this::onTimeout);
    }

    protected boolean checkMessage(MessageReceivedEvent event) {
        return event.isFromType(ChannelType.TEXT) && event.getTextChannel().equals(context.getChannel())
                && event.getAuthor().equals(context.getSender());
    }

    protected void handleMessage(MessageReceivedEvent event) {
        if (messageHandler != null) {
            messageHandler.accept(event);
        }
    }

    protected void onTimeout() {
        if (timeoutHandler != null) timeoutHandler.run();
    }
}
