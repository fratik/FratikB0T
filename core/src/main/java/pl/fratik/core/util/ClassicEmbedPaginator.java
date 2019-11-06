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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ClassicEmbedPaginator implements EmbedPaginator {
    private static final String FIRST_EMOJI = "\u23EE";
    private static final String LEFT_EMOJI = "\u25C0";
    private static final String RIGHT_EMOJI = "\u25B6";
    private static final String LAST_EMOJI = "\u23ED";
    private static final String STOP_EMOJI = "\u23F9";
    private static final String ONETWOTHREEFOUR_EMOJI = "\uD83D\uDD22";

    private final EventWaiter eventWaiter;
    private final List<EmbedBuilder> pages;
    private final EventBus eventBus;
    private int pageNo = 1;
    private Message message;
    private Message doKtorej;
    private long messageId = 0;
    private long userId;
    private Language language;
    private Tlumaczenia tlumaczenia;
    private boolean customFooter;
    private static final String PMSTO = "moderation";
    private static final String PMZAADD = "znaneAkcje-add:";

    public ClassicEmbedPaginator(EventWaiter eventWaiter, List<EmbedBuilder> pages, User user, Language language, Tlumaczenia tlumaczenia, EventBus eventBus) {
        this.eventWaiter = eventWaiter;
        this.pages = pages;
        this.eventBus = eventBus;
        if (pages.isEmpty()) throw new IllegalArgumentException("brak stron");
        this.userId = user.getIdLong();
        this.language = language;
        this.tlumaczenia = tlumaczenia;
    }

    @Override
    public void create(MessageChannel channel) {
        channel.sendMessage(render(1)).override(true).queue(msg -> {
            message = msg;
            messageId = msg.getIdLong();
            if (pages.size() != 1) {
                addReactions(msg);
                waitForReaction();
            }
        });
    }

    @Override
    public void create(Message message) {
        this.message = message;
        messageId = message.getIdLong();
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + message.getId()));
        message.editMessage(render(1)).override(true).queue(msg -> {
            if (pages.size() != 1) {
                addReactions(msg);
                waitForReaction();
            }
        });
    }

    private void addReactions(Message message) {
        message.addReaction(FIRST_EMOJI).queue();
        message.addReaction(LEFT_EMOJI).queue();
        message.addReaction(RIGHT_EMOJI).queue();
        message.addReaction(LAST_EMOJI).queue();
        message.addReaction(STOP_EMOJI).queue();
        message.addReaction(ONETWOTHREEFOUR_EMOJI).queue();
    }

    private void waitForReaction() {
        eventWaiter.waitForEvent(MessageReactionAddEvent.class, this::checkReaction,
                this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
    }

    private boolean checkReaction(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() == messageId && !event.getReactionEmote().isEmote()) {
            switch (event.getReactionEmote().getName()) {
                case FIRST_EMOJI:
                case LEFT_EMOJI:
                case RIGHT_EMOJI:
                case LAST_EMOJI:
                case STOP_EMOJI:
                case ONETWOTHREEFOUR_EMOJI:
                    return event.getUser().getIdLong() == userId;
                default:
                    return false;
            }
        }
        return false;
    }

    private void handleReaction(MessageReactionAddEvent event) {
        if (!event.getReactionEmote().isEmote()) {
            switch (event.getReactionEmote().getName()) {
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
                    doKtorej = event.getChannel().sendMessage(tlumaczenia.get(language, "paginator.waiting.for.pageno")).complete();
                    eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                            this::handleMessage, 30, TimeUnit.SECONDS, this::clearReactions);
                    break;
                default: return;
            }
        }

        try {
            event.getReaction().removeReaction(event.getUser()).queue();
        } catch (PermissionException ignored) {/*lul*/}

        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + message.getId()));
        message.editMessage(render(pageNo)).override(true).queue(msg -> waitForReaction());
    }

    private void handleMessage(MessageReceivedEvent event) {
        pageNo = Integer.parseInt(event.getMessage().getContentRaw());
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + doKtorej.getId()));
        doKtorej.delete().queue();
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + event.getMessage().getId()));
        message.editMessage(render(pageNo)).override(true).queue(msg -> waitForReaction());
    }

    private boolean checkMessage(MessageReceivedEvent e) {
        try {
            return (Integer.parseInt(e.getMessage().getContentRaw()) >= 1 && Integer.parseInt(e.getMessage().getContentRaw()) <= pages.size()) &&
            e.isFromGuild() && e.getTextChannel().equals(message.getTextChannel())
                    && e.getAuthor().getIdLong() == userId;
        } catch (Exception ignored) {
            return false;
        }
    }

    private MessageEmbed render(int page) {
        EmbedBuilder pageEmbed = pages.get(page - 1);
        if (!customFooter) pageEmbed.setFooter(String.format("%s/%s", String.valueOf(page), String.valueOf(pages.size())), null);
        else pageEmbed.setFooter(String.format(Objects.requireNonNull(Objects.requireNonNull(pageEmbed.build().getFooter(),
                "stopka jest null mimo customFooter").getText(), "text jest null mimo customFooter"),
                String.valueOf(page), String.valueOf(pages.size())), null);
        return pageEmbed.build();
    }

    private void clearReactions() {
        try {
            if (doKtorej != null) {
                eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + doKtorej.getId()));
                doKtorej.delete().queue();
            }
            message.clearReactions().queue();
        } catch (PermissionException ignored) {/*lul*/}
    }

    @Override
    public ClassicEmbedPaginator setCustomFooter(boolean customFooter) {
        this.customFooter = customFooter;
        return this;
    }
}
