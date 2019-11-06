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
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import pl.fratik.core.command.CommandContext;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ReactionWaiter {
    private final EventWaiter eventWaiter;
    private final CommandContext context;

    @Getter @Setter private Consumer<MessageReactionAddEvent> reactionHandler;
    @Getter @Setter private Runnable timeoutHandler;

    public ReactionWaiter(EventWaiter eventWaiter, CommandContext context) {
        this.eventWaiter = eventWaiter;
        this.context = context;
    }

    public void create() {
        eventWaiter.waitForEvent(MessageReactionAddEvent.class, this::checkReaction,
                this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
    }

    protected boolean checkReaction(MessageReactionAddEvent event) {
        return event.isFromType(ChannelType.TEXT) && event.getTextChannel().equals(context.getChannel())
                && event.getUser().equals(context.getSender());
    }

    private void handleReaction(MessageReactionAddEvent event) {
        if (reactionHandler != null) {
            reactionHandler.accept(event);
        }
    }

    private void clearReactions() {
        if (timeoutHandler != null) timeoutHandler.run();
    }
}
