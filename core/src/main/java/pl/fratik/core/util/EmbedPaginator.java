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
import io.sentry.Sentry;
import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public abstract class EmbedPaginator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbedPaginator.class);
    private static final String FIRST_EMOJI = "\u23EE";
    private static final String LEFT_EMOJI = "\u25C0";
    private static final String RIGHT_EMOJI = "\u25B6";
    private static final String LAST_EMOJI = "\u23ED";
    private static final String STOP_EMOJI = "\u23F9";
    private static final String ONETWOTHREEFOUR_EMOJI = "\uD83D\uDD22";
    private static final String SHUFFLE_EMOJI = "\uD83D\uDD00";
    private static final String TRASH_EMOJI = "\uD83D\uDDD1";

    private static final Set<EmbedPaginator> runningPaginators = new HashSet<>();
    private static boolean shutdown;

    protected final EventBus eventBus;
    protected final EventWaiter eventWaiter;
    protected Message message;
    protected long messageId;
    protected int pageNo = 1;
    protected final long userId;
    protected final Language language;
    protected final Tlumaczenia tlumaczenia;
    protected boolean customFooter;
    protected boolean enableShuffle;
    protected boolean enableDelett;
    protected long timeout = 30;
    protected boolean ended = false;
    private static final String PMSTO = "moderation";
    private static final String PMZAADD = "znaneAkcje-add:";
    private Message doKtorej;
    @Getter(AccessLevel.PROTECTED) private MessageEmbed currentEmbed;

    protected EmbedPaginator(EventBus eventBus, EventWaiter eventWaiter, long userId, Language language, Tlumaczenia tlumaczenia) {
        this.eventBus = eventBus;
        this.eventWaiter = eventWaiter;
        this.userId = userId;
        this.language = language;
        this.tlumaczenia = tlumaczenia;
    }

    @NotNull protected abstract MessageEmbed render(int page) throws LoadingException;
    protected abstract int getPageCount();

    protected void rerender() throws LoadingException {
        addReactions(message.editMessage(currentEmbed = render(pageNo))).override(true).queue();
    }

    public void create(MessageChannel channel, String referenceMessageId) {
        try {
            MessageAction action = channel.sendMessage(currentEmbed = render(1));
            if (referenceMessageId != null) action = action.referenceById(referenceMessageId);
            action = addReactions(action);
            action.override(true).queue(msg -> {
                message = msg;
                messageId = msg.getIdLong();
                if (getPageCount() != 1) {
                    waitForReaction();
                }
            });
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) {
        LOGGER.error("Ładowanie strony nawaliło", e);
        Sentry.getContext().setUser(new io.sentry.event.User(String.valueOf(userId), null,
                null, null));
        Sentry.capture(e);
        Sentry.clearContext();
        String key = "generic.dynamicembedpaginator.error";
        if (e instanceof LoadingException) {
            if (((LoadingException) e).firstPage) key = "generic.dynamicembedpaginator.errorone";
            else if (((LoadingException) e).loading) key = "generic.lazyloading";
        }
        message.getChannel().sendMessage(tlumaczenia.get(language, key))
                .queue(m -> {
                    if (!(e instanceof LoadingException) || !((LoadingException) e).firstPage)
                        m.delete().queueAfter(5, TimeUnit.SECONDS);
                });
    }

    public void create(Message message) {
        this.message = message;
        messageId = message.getIdLong();
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + message.getId()));
        try {
            addReactions(message.editMessage(render(1))).override(true).queue(msg -> {
                if (getPageCount() != 1) {
                    waitForReaction();
                }
            });
        } catch (LoadingException e) {
            handleException(e);
        }
    }

    public void create(MessageChannel channel) {
        create(channel, (Message) null);
    }

    public void create(MessageChannel channel, Message referenceMessage) {
        create(channel, referenceMessage == null ? null : referenceMessage.getId());
    }

    private MessageAction addReactions(MessageAction action) {
        return addReactions(action, false);
    }

    private MessageAction addReactions(MessageAction action, boolean disabled) {
        if (getPageCount() == 1) return action;
        List<Button> secondRowButtons = new ArrayList<>();
        secondRowButtons.add(Button.danger("STOP_PAGE", Emoji.fromUnicode(STOP_EMOJI)).withDisabled(disabled));
        secondRowButtons.add(Button.secondary("CHOOSE_PAGE", Emoji.fromUnicode(ONETWOTHREEFOUR_EMOJI)).withDisabled(disabled));
        if (enableShuffle) secondRowButtons.add(Button.secondary("SHUFFLE_PAGE", Emoji.fromUnicode(SHUFFLE_EMOJI)).withDisabled(disabled));
        if (enableDelett) secondRowButtons.add(Button.danger("TRASH_PAGE", Emoji.fromUnicode(TRASH_EMOJI)).withDisabled(disabled));
        return action.setActionRows(
                ActionRow.of(
                        Button.secondary("FIRST_PAGE", Emoji.fromUnicode(FIRST_EMOJI)).withDisabled(pageNo == 1 || disabled),
                        Button.primary("PREV_PAGE", Emoji.fromUnicode(LEFT_EMOJI))
                                .withLabel(String.valueOf(Math.max(pageNo - 1, 1))).withDisabled(pageNo == 1 || disabled),
                        Button.primary("NEXT_PAGE", Emoji.fromUnicode(RIGHT_EMOJI))
                                .withLabel(String.valueOf(Math.min(pageNo + 1, getPageCount()))).withDisabled(pageNo == getPageCount() || disabled),
                        Button.secondary("LAST_PAGE", Emoji.fromUnicode(LAST_EMOJI)).withDisabled(pageNo == getPageCount() || disabled)
                ),
                ActionRow.of(secondRowButtons)
        );
    }

    private boolean checkReaction(ButtonClickEvent event) {
        if (shutdown) return false;
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
                case "SHUFFLE_PAGE":
                    return enableShuffle;
                case "TRASH_PAGE":
                    return enableDelett;
                default:
                    return false;
            }
        }
        return false;
    }

    private void handleReaction(ButtonClickEvent event) {
        runningPaginators.remove(this);
        final int oldPageNo = pageNo;
        switch (event.getComponentId()) {
            case "FIRST_PAGE":
                pageNo = 1;
                break;
            case "PREV_PAGE":
                if (pageNo > 1) pageNo--;
                break;
            case "NEXT_PAGE":
                if (pageNo < getPageCount()) pageNo++;
                break;
            case "LAST_PAGE":
                pageNo = getPageCount();
                break;
            case "STOP_PAGE":
                clearReactions();
                return;
            case "CHOOSE_PAGE":
                if (addPaginator()) {
                    addReactions(message.editMessage(message), true).override(true).queue();
                    doKtorej = event.getChannel().sendMessage(tlumaczenia.get(language, "paginator.waiting.for.pageno")).complete();
                    eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                            this::handleMessage, timeout, TimeUnit.SECONDS, this::clearReactions);
                }
                return;
            case "SHUFFLE_PAGE":
                pageNo = ThreadLocalRandom.current().nextInt(getPageCount()) + 1;
                break;
            case "TRASH_PAGE":
                clearReactions(true);
                return;
            default: return;
        }

        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + message.getId()));
        try {
            addReactions(message.editMessage(currentEmbed = render(pageNo))).override(true).queue(msg -> waitForReaction());
        } catch (LoadingException e) {
            pageNo = oldPageNo;
            addReactions(message.editMessage(currentEmbed)).override(true).queue(msg -> waitForReaction());
        }
    }

    private void handleMessage(MessageReceivedEvent event) {
        runningPaginators.remove(this);
        final int oldPageNo = pageNo;
        pageNo = Integer.parseInt(event.getMessage().getContentRaw());
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + doKtorej.getId()));
        doKtorej.delete().queue();
        doKtorej = null;
        eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + event.getMessage().getId()));
        event.getMessage().delete().queue();
        try {
            addReactions(message.editMessage(currentEmbed = render(pageNo))).override(true).queue(msg -> waitForReaction());
        } catch (LoadingException e) {
            pageNo = oldPageNo;
            addReactions(message.editMessage(currentEmbed)).override(true).queue(msg -> waitForReaction());
        }
    }

    private boolean checkMessage(MessageReceivedEvent e) {
        if (shutdown) return false;
        try {
            return (Integer.parseInt(e.getMessage().getContentRaw()) >= 1 && Integer.parseInt(e.getMessage().getContentRaw()) <= getPageCount()) &&
                    e.isFromGuild() && e.getTextChannel().equals(message.getTextChannel())
                    && e.getAuthor().getIdLong() == userId;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void waitForReaction() {
        if (addPaginator()) eventWaiter.waitForEvent(ButtonClickEvent.class, this::checkReaction,
                this::handleReaction, timeout, TimeUnit.SECONDS, this::clearReactions);
        else clearReactions();
    }

    private void clearReactions() {
        clearReactions(false);
    }

    private void clearReactions(boolean delett) {
        runningPaginators.remove(this);
        if (ended) return;
        ended = true;
        try {
            if (doKtorej != null) {
                eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + doKtorej.getId()));
                doKtorej.delete().queue();
                doKtorej = null;
            }
            if (delett) {
                eventBus.post(new PluginMessageEvent("core", PMSTO, PMZAADD + message.getId()));
                message.delete().queue();
            } else clearActions(message);
        } catch (PermissionException ignored) {/*lul*/}
    }

    private void clearActions(Message botMsg) {
        MessageBuilder mb = new MessageBuilder();
        if (!botMsg.getEmbeds().isEmpty()) mb.setEmbed(botMsg.getEmbeds().get(0));
        mb.setContent(botMsg.getContentRaw());
        mb.setActionRows(Collections.emptyList());
        botMsg.editMessage(mb.build()).queue();
    }

    public EmbedPaginator setCustomFooter(boolean customFooter) {
        this.customFooter = customFooter;
        return this;
    }

    public EmbedPaginator setEnableShuffle(boolean enableShuffle) {
        this.enableShuffle = enableShuffle;
        return this;
    }

    public EmbedPaginator setEnableDelett(boolean enableDelett) {
        this.enableDelett = enableDelett;
        return this;
    }

    public EmbedPaginator setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    protected static class LoadingException extends Exception {
        @Getter private final boolean firstPage;
        @Getter private final boolean loading;

        LoadingException(boolean firstPage, boolean loading, Throwable cause) {
            this.firstPage = firstPage;
            this.loading = loading;
            initCause(cause);
        }
    }

    public static void shutdown() {
        synchronized (runningPaginators) {
            shutdown = true;
            for (EmbedPaginator runningPaginator : new HashSet<>(runningPaginators)) {
                try {
                    runningPaginator.clearReactions();
                } catch (Exception ignored) {
                    // machnij ręką, idź do następnego
                }
                runningPaginators.remove(runningPaginator);
            }
        }
    }

    /**
     * Dodaje paginator do {@link EmbedPaginator#runningPaginators}, sprawdzając czy shutdown
     * @return {@code false}, jeżeli należy przerwać wykonywanie paginatora
     */
    private boolean addPaginator() {
        synchronized (runningPaginators) {
            if (shutdown) return false;
            runningPaginators.add(this);
            return true;
        }
    }
}
