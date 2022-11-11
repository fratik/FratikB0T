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

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.concurrent.TimeUnit;

class TextPaginator {
    private static final String FIRST_EMOJI = "\u23EE";
    private static final String LEFT_EMOJI = "\u25C0";
    private static final String RIGHT_EMOJI = "\u25B6";
    private static final String LAST_EMOJI = "\u23ED";
    private static final String STOP_EMOJI = "\u23F9";
    private static final String ONETWOTHREEFOUR_EMOJI = "\uD83D\uDD22";

    private final EventWaiter eventWaiter;
    private final List<String> pages;
    private int pageNo = 1;
    private Message message;
    private long messageId = 0;
    private final long userId;
    private final Language language;
    private final Tlumaczenia tlumaczenia;

    public TextPaginator(EventWaiter eventWaiter, List<String> pages, User user, Language language, Tlumaczenia tlumaczenia) {
        this.language = language;
        this.tlumaczenia = tlumaczenia;
        this.eventWaiter = eventWaiter;
        this.pages = pages;
        if (pages.isEmpty()) throw new IllegalArgumentException("brak stron");
        this.userId = user.getIdLong();
    }

    public void create(MessageChannel channel) {
        channel.sendMessage(render(1)).queue(msg -> {
            this.message = msg;
            messageId = msg.getIdLong();
            if (pages.size() != 1) {
                addReactions(msg);
                waitForReaction();
            }
        });
    }

    public void create(Message message) {
        this.message = message;
        messageId = message.getIdLong();
        message.editMessage(render(1)).queue(msg -> {
            if (pages.size() != 1) {
                addReactions(msg);
                waitForReaction();
            }
        });
    }

    private void addReactions(Message message) {
        message.addReaction(Emoji.fromUnicode(FIRST_EMOJI)).queue();
        message.addReaction(Emoji.fromUnicode(LEFT_EMOJI)).queue();
        message.addReaction(Emoji.fromUnicode(RIGHT_EMOJI)).queue();
        message.addReaction(Emoji.fromUnicode(LAST_EMOJI)).queue();
        message.addReaction(Emoji.fromUnicode(STOP_EMOJI)).queue();
        message.addReaction(Emoji.fromUnicode(ONETWOTHREEFOUR_EMOJI)).queue();
    }

    private void waitForReaction() {
        eventWaiter.waitForEvent(MessageReactionAddEvent.class, this::checkReaction,
                this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
    }

    private boolean checkReaction(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() == messageId && event.getEmoji().getType() == Emoji.Type.UNICODE) {
            switch (event.getEmoji().getName()) {
                case FIRST_EMOJI:
                case LEFT_EMOJI:
                case RIGHT_EMOJI:
                case LAST_EMOJI:
                case STOP_EMOJI:
                case ONETWOTHREEFOUR_EMOJI:
                    return event.getUserIdLong() == userId;
                default:
                    return false;
            }
        }
        return false;
    }

    private void handleReaction(MessageReactionAddEvent event) {
        if (event.getEmoji().getType() == Emoji.Type.UNICODE) {
            switch (event.getEmoji().getName()) {
                case FIRST_EMOJI:
                    pageNo = 1;
                    break;
                case LEFT_EMOJI:
                    if (pageNo > 1) pageNo--;
                    break;
                case RIGHT_EMOJI:
                    if (pageNo < pages.size()) pageNo++;
                    break;
                case LAST_EMOJI:
                    pageNo = pages.size();
                    break;
                case STOP_EMOJI:
                    clearReactions();
                    return;
                case ONETWOTHREEFOUR_EMOJI:
                    event.getChannel().sendMessage(tlumaczenia.get(language, "paginator.waiting.for.pageno")).queue();
                    eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                            this::handleMessage, 30, TimeUnit.SECONDS, this::clearReactions);
                    break;
                default: break;
            }
        }

        try {
            event.getReaction().removeReaction(event.getUser()).queue();
        } catch (PermissionException ignored) {/*lul*/}

        message.editMessage(render(pageNo)).setReplace(true).queue(msg -> waitForReaction());
    }

    private void handleMessage(MessageReceivedEvent event) {
        pageNo = Integer.parseInt(event.getMessage().getContentRaw());
        render(pageNo);
    }

    private boolean checkMessage(MessageReceivedEvent e) {
        try {
            return (Integer.parseInt(e.getMessage().getContentRaw()) >= 1 && Integer.parseInt(e.getMessage().getContentRaw()) <= pages.size()) &&
                    e.isFromType(ChannelType.TEXT) && e.getChannel().equals(message.getChannel())
                    && e.getAuthor().getIdLong() == userId;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String render(int page) {
        return pages.get(page - 1).replace("{{page}}", Integer.toString(page));
    }

    private void clearReactions() {
        try {
            message.clearReactions().queue();
        } catch (PermissionException ignored) {/*lul*/}
    }
}
