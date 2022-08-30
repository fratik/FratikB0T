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
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import pl.fratik.core.command.NewCommandContext;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ModalWaiter {
    private final EventWaiter eventWaiter;
    private final NewCommandContext context;
    private final String modalId;
    private final ResponseType type;

    @Getter @Setter private Consumer<ModalInteractionEvent> buttonHandler;
    @Getter @Setter private Runnable timeoutHandler;

    public ModalWaiter(EventWaiter eventWaiter, NewCommandContext context, String modalId, ResponseType type) {
        this.eventWaiter = eventWaiter;
        this.context = context;
        this.modalId = modalId;
        this.type = type;
    }

    public void create() {
        eventWaiter.waitForEvent(ModalInteractionEvent.class, this::checkReaction,
                this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
    }

    protected boolean checkReaction(ModalInteractionEvent event) {
        if (!event.getModalId().equals(modalId)) return false;
        boolean validUser = event.getUser().equals(context.getSender());
        if (!validUser) {
            event.reply(context.getTlumaczenia().get(context.getTlumaczenia().getLanguage(event.getMember()),
                    "buttonwaiter.invalid.user", context.getSender().getAsMention())).setEphemeral(true).queue();
            return false;
        }
        else {
            try {
                if (type == ResponseType.REPLY) event.deferReply().complete();
                else if (type == ResponseType.REPLY_EPHEMERAL) event.deferReply(true).complete();
                else if (type == ResponseType.EDIT) event.deferEdit().complete();
            } catch (ErrorResponseException ex) {
                if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_INTERACTION) return false;
                throw ex;
            }
        }
        return true;
    }

    protected void handleReaction(ModalInteractionEvent event) {
        if (buttonHandler != null) {
            buttonHandler.accept(event);
        }
    }

    protected void clearReactions() {
        if (timeoutHandler != null) timeoutHandler.run();
    }

    public enum ResponseType {
        EDIT,
        REPLY,
        REPLY_EPHEMERAL
    }
}
