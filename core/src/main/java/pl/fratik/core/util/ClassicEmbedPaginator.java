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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.ArrayList;
import java.util.Collections;
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
    private final long userId;
    private final Language language;
    private final Tlumaczenia tlumaczenia;
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
    public void create(MessageChannel channel, String referenceMessageId) {
        MessageAction action = channel.sendMessage(render(1));
        if (referenceMessageId != null) action = action.referenceById(referenceMessageId);
        action = addReactions(action);
        action.override(true).queue(msg -> {
            message = msg;
            messageId = msg.getIdLong();
            if (pages.size() != 1) {
                waitForReaction();
            }
        });
    }

    @Override
    public void create(Message message) {
        this.message = message;
        messageId = message.getIdLong();
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + message.getId()));
        addReactions(message.editMessage(render(1))).override(true).queue(msg -> {
            if (pages.size() != 1) {
                waitForReaction();
            }
        });
    }

    private MessageAction addReactions(MessageAction action) {
        if (pages.size() == 1) return action;
        List<Button> secondRowButtons = new ArrayList<>();
        secondRowButtons.add(Button.danger("STOP_PAGE", Emoji.ofUnicode(STOP_EMOJI)));
        secondRowButtons.add(Button.secondary("CHOOSE_PAGE", Emoji.ofUnicode(ONETWOTHREEFOUR_EMOJI)));
        return action.setActionRows(
                ActionRow.of(
                        Button.secondary("FIRST_PAGE", Emoji.ofUnicode(FIRST_EMOJI)).withDisabled(pageNo == 1),
                        Button.primary("PREV_PAGE", Emoji.ofUnicode(LEFT_EMOJI))
                                .withLabel(String.valueOf(Math.max(pageNo - 1, 1))).withDisabled(pageNo == 1),
                        Button.primary("NEXT_PAGE", Emoji.ofUnicode(RIGHT_EMOJI))
                                .withLabel(String.valueOf(Math.min(pageNo + 1, pages.size()))).withDisabled(pageNo == pages.size()),
                        Button.secondary("LAST_PAGE", Emoji.ofUnicode(LAST_EMOJI)).withDisabled(pageNo == pages.size())
                ),
                ActionRow.of(secondRowButtons)
        );
    }

    private void waitForReaction() {
        eventWaiter.waitForEvent(ButtonClickEvent.class, this::checkReaction,
                this::handleReaction, 30, TimeUnit.SECONDS, this::clearReactions);
    }

    private boolean checkReaction(ButtonClickEvent event) {
        if (event.getMessageIdLong() == messageId) {
            if (event.getUser().getIdLong() == userId) event.deferEdit().queue();
            else {
                event.reply(tlumaczenia.get(tlumaczenia.getLanguage(event.getMember()),
                        "paginator.invalid.user", "<@" + userId + ">")).setEphemeral(true).queue();
                return false;
            }
            switch (event.getComponentId()) {
                case "FIRST_PAGE":
                case "PREV_PAGE":
                case "NEXT_PAGE":
                case "LAST_PAGE":
                case "STOP_PAGE":
                case "CHOOSE_PAGE":
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private void handleReaction(ButtonClickEvent event) {
        switch (event.getComponentId()) {
            case "FIRST_PAGE":
                pageNo = 1;
                break;
            case "PREV_PAGE":
                if (pageNo > 1) pageNo--;
                break;
            case "NEXT_PAGE":
                if (pageNo < pages.size()) pageNo++;
                break;
            case "LAST_PAGE":
                pageNo = pages.size();
                break;
            case "STOP_PAGE":
                clearReactions();
                return;
            case "CHOOSE_PAGE":
                doKtorej = event.getChannel().sendMessage(tlumaczenia.get(language, "paginator.waiting.for.pageno")).complete();
                eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                        this::handleMessage, 30, TimeUnit.SECONDS, this::clearReactions);
                return;
            default: return;
        }

        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + message.getId()));
        addReactions(message.editMessage(render(pageNo))).override(true).queue(msg -> waitForReaction());
    }

    private void handleMessage(MessageReceivedEvent event) {
        pageNo = Integer.parseInt(event.getMessage().getContentRaw());
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + doKtorej.getId()));
        doKtorej.delete().queue();
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + event.getMessage().getId()));
        event.getMessage().delete().queue();
        addReactions(message.editMessage(render(pageNo))).override(true).queue(msg -> waitForReaction());
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
        if (!customFooter) pageEmbed.setFooter(String.format("%s/%s", page, pages.size()), null);
        else pageEmbed.setFooter(String.format(Objects.requireNonNull(Objects.requireNonNull(pageEmbed.build().getFooter(),
                "stopka jest null mimo customFooter").getText(), "text jest null mimo customFooter"),
                page, pages.size()), null);
        return pageEmbed.build();
    }

    private void clearReactions() {
        try {
            if (doKtorej != null) {
                eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + doKtorej.getId()));
                doKtorej.delete().queue();
            }
            message.editMessage(message.getContentRaw()).setActionRows(Collections.emptySet()).queue();
        } catch (PermissionException ignored) {/*lul*/}
    }

    @Override
    public ClassicEmbedPaginator setCustomFooter(boolean customFooter) {
        this.customFooter = customFooter;
        return this;
    }
}
