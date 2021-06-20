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

package pl.fratik.core.util;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import pl.fratik.core.command.CommandContext;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ButtonWaiter {
    private final EventWaiter eventWaiter;
    private final CommandContext context;
    private final long messageId;
    private final ResponseType type;

    @Getter @Setter private Consumer<ButtonClickEvent> buttonHandler;
    @Getter @Setter private Runnable timeoutHandler;

    public ButtonWaiter(EventWaiter eventWaiter, CommandContext context, long messageId, ResponseType type) {
        this.eventWaiter = eventWaiter;
        this.context = context;
        this.messageId = messageId;
        this.type = type;
    }

    public void create() {
        eventWaiter.waitForEvent(ButtonClickEvent.class, this::checkReaction,
                this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
    }

    protected boolean checkReaction(ButtonClickEvent event) {
        if (event.getMessageIdLong() != messageId) return false;
        boolean validUser = event.getUser().equals(context.getSender());
        if (!validUser) event.reply(context.getTlumaczenia().get(context.getTlumaczenia().getLanguage(event.getMember()),
                "buttonwaiter.invalid.user", context.getSender().getAsMention())).setEphemeral(true).queue();
        else {
            if (type == ResponseType.REPLY) event.deferReply().queue();
            else if (type == ResponseType.EDIT) event.deferEdit().queue();
        }
        return validUser;
    }

    private void handleReaction(ButtonClickEvent event) {
        if (buttonHandler != null) {
            buttonHandler.accept(event);
        }
    }

    private void clearReactions() {
        if (timeoutHandler != null) timeoutHandler.run();
    }

    public enum ResponseType {
        EDIT,
        REPLY
    }
}
