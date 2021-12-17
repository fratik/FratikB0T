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

package pl.fratik.core.command;

import io.sentry.Sentry;
import io.sentry.event.Event.Level;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.ArgsMissingException;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static pl.fratik.core.Statyczne.BRAND_COLOR;

public class CommandContext {
    @Getter private final ShardManager shardManager;
    @Getter private final Command command;
    @Getter private final Tlumaczenia tlumaczenia;
    @Getter private final Language language;
    @Getter private final MessageReceivedEvent event;
    @Getter private final String prefix;
    @Getter private final String label;
    @Getter private final String[] rawArgs;
    @Getter private final Object[] args;
    @Getter @Nullable private final PermLevel customPermLevel;
    @Getter private final boolean direct;

    private static final Pattern URLPATTERN = Pattern.compile("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\." +
            "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]" +
            "\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})");

    public CommandContext(ShardManager shardManager,
                          Tlumaczenia tlumaczenia,
                          Command command,
                          MessageReceivedEvent event,
                          String prefix,
                          String label,
                          @Nullable PermLevel customPermLevel,
                          boolean direct) {
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        this.command = command;
        this.prefix = prefix;
        this.customPermLevel = customPermLevel;
        this.direct = direct;
        if (event.isFromGuild()) this.language = tlumaczenia.getLanguage(event.getMember());
        else this.language = tlumaczenia.getLanguage(event.getAuthor());
        this.event = event;
        this.label = label;
        this.rawArgs = null;
        this.args = null;
    }

    public CommandContext(ShardManager shardManager,
                          Tlumaczenia tlumaczenia,
                          Command command,
                          MessageReceivedEvent event,
                          String prefix,
                          String label,
                          String[] args,
                          @Nullable PermLevel customPermLevel,
                          boolean direct) throws ArgsMissingException {
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        this.command = command;
        this.prefix = prefix;
        this.customPermLevel = customPermLevel;
        this.direct = direct;
        if (event.isFromGuild()) this.language = tlumaczenia.getLanguage(event.getMember());
        else this.language = tlumaczenia.getLanguage(event.getAuthor());
        this.event = event;
        this.label = label;
        this.rawArgs = args;
        this.args = command.getUzycie().resolveArgs(this);
    }

    public Message getMessage() {
        return event.getMessage();
    }

    public User getSender() {
        return event.getAuthor();
    }

    public Member getMember() {
        return event.getMember();
    }

    public MessageChannel getMessageChannel() {
        return event.getChannel();
    }

    public TextChannel getTextChannel() {
        return event.getTextChannel();
    }

    public Guild getGuild() {
        return event.isFromGuild() ? event.getGuild() : null;
    }

//    @Deprecated
    public Message send(CharSequence message) {
        return send(message, true);
    }

    public Message send(CharSequence message, boolean checkUrl) {
        if (checkUrl && URLPATTERN.matcher(message).matches()) {
            Exception blad = new Exception("Odpowiedź zawiera link!");
            Sentry.getContext().setUser(new io.sentry.event.User(getSender().getId(),
                    getSender().getName(), null, null));
            Sentry.capture(new EventBuilder().withLevel(Level.WARNING).withMessage(blad.getMessage())
                    .withExtra("wiadomosc", message).withSentryInterface(new ExceptionInterface(blad)));
            Sentry.clearContext();
        }
        return event.getChannel().sendMessage(message).complete();
    }

//    @Deprecated
    public Message send(MessageEmbed message) {
        return event.getChannel().sendMessage(new MessageBuilder(message).build()).complete();
    }

    public void send(CharSequence message, Consumer<Message> callback) {
        if (URLPATTERN.matcher(message).matches()) {
            Exception blad = new Exception("Odpowiedź zawiera link!");
            Sentry.getContext().setUser(new io.sentry.event.User(getSender().getId(),
                    getSender().getName(), null, null));
            Sentry.capture(new EventBuilder().withLevel(Level.WARNING).withMessage(blad.getMessage())
                    .withExtra("wiadomosc", message).withSentryInterface(new ExceptionInterface(blad)));
            Sentry.clearContext();
        }
        event.getChannel().sendMessage(message).queue(callback);
    }

    public void send(MessageEmbed message, Consumer<Message> callback) {
        event.getChannel().sendMessage(new MessageBuilder(message).build()).queue(callback);
    }

    public Message reply(CharSequence message) {
        return reply(message, true);
    }

    public Message reply(CharSequence message, Collection<ActionRow> actionRows) {
        return reply(message, true, actionRows.toArray(new ActionRow[0]));
    }

    public Message reply(CharSequence message, ActionRow... actionRows) {
        return reply(message, true, actionRows);
    }

    private boolean isUnknownMessage(Throwable e) {
        if (!(e instanceof ErrorResponseException)) return false;
        ErrorResponseException h = (ErrorResponseException) e;
        return h.getErrorCode() == 400 && h.getMeaning().equals("{\"message_reference\":[\"Unknown message\"]}");
    }

    public Message reply(CharSequence message, boolean checkUrl) {
        return reply(message, checkUrl, (ActionRow[]) null);
    }

    public Message reply(CharSequence message, boolean checkUrl, Collection<ActionRow> actionRows) {
        return reply(message, checkUrl, actionRows == null ? null : actionRows.toArray(new ActionRow[0]));
    }

    public Message reply(CharSequence message, boolean checkUrl, ActionRow... actionRows) {
        if (actionRows == null) actionRows = new ActionRow[0];
        if (checkUrl && URLPATTERN.matcher(message).matches()) {
            Exception blad = new Exception("Odpowiedź zawiera link!");
            Sentry.getContext().setUser(new io.sentry.event.User(getSender().getId(),
                    getSender().getName(), null, null));
            Sentry.capture(new EventBuilder().withLevel(Level.WARNING).withMessage(blad.getMessage())
                    .withExtra("wiadomosc", message).withSentryInterface(new ExceptionInterface(blad)));
            Sentry.clearContext();
        }
        try {
            if (event.isFromGuild() && !event.getGuild().getSelfMember().hasPermission(getGuildChannel(),
                    Permission.MESSAGE_HISTORY)) return event.getChannel().sendMessage(message)
                    .setActionRows(actionRows).complete();
            return event.getChannel().sendMessage(message).setActionRows(actionRows).reference(getMessage()).complete();
        } catch (ErrorResponseException e) {
            if (isUnknownMessage(e)) return event.getChannel().sendMessage(message).setActionRows(actionRows).complete();
            throw e;
        }
    }

    //    @Deprecated
    public Message reply(MessageEmbed message) {
        return reply(message, (ActionRow[]) null);
    }

    public Message reply(MessageEmbed message, Collection<ActionRow> actionRows) {
        return reply(message, actionRows == null ? null : actionRows.toArray(new ActionRow[0]));
    }

    public Message reply(MessageEmbed message, ActionRow... actionRows) {
        if (actionRows == null) actionRows = new ActionRow[0];
        Message msg = new MessageBuilder(message).build();
        try {
            return event.getChannel().sendMessage(msg).reference(getMessage()).setActionRows(actionRows).complete();
        } catch (ErrorResponseException e) {
            if (isUnknownMessage(e)) return event.getChannel().sendMessage(msg).setActionRows(actionRows).complete();
            throw e;
        }
    }

    public void reply(CharSequence message, Consumer<Message> callback) {
        if (URLPATTERN.matcher(message).matches()) {
            Exception blad = new Exception("Odpowiedź zawiera link!");
            Sentry.getContext().setUser(new io.sentry.event.User(getSender().getId(),
                    getSender().getName(), null, null));
            Sentry.capture(new EventBuilder().withLevel(Level.WARNING).withMessage(blad.getMessage())
                    .withExtra("wiadomosc", message).withSentryInterface(new ExceptionInterface(blad)));
            Sentry.clearContext();
        }
        event.getChannel().sendMessage(message).reference(getMessage()).queue(callback, e -> {
            if (isUnknownMessage(e)) event.getChannel().sendMessage(message).queue(callback);
        });
    }

    public void reply(MessageEmbed message, Consumer<Message> callback) {
        Message msg = new MessageBuilder(message).build();
        event.getChannel().sendMessage(msg).reference(getMessage()).queue(callback, e -> {
            if (isUnknownMessage(e)) event.getChannel().sendMessage(msg).queue(callback);
        });
    }

    public Message reply(Message message) {
        try {
            return replyAsAction(message).complete();
        } catch (ErrorResponseException e) {
            if (isUnknownMessage(e)) return event.getChannel().sendMessage(message).complete();
            throw e;
        }
    }

    public void reply(Message message, Consumer<Message> callback) {
        replyAsAction(message).queue(callback, e -> {
            if (isUnknownMessage(e)) event.getChannel().sendMessage(message).queue(callback);
        });
    }

    public MessageAction replyAsAction(Message message) {
        return event.getChannel().sendMessage(message).reference(getMessage());
    }

    public boolean checkSensitive(String input) {
        return Ustawienia.instance.apiKeys.values().stream().anyMatch(input::contains);
    }

    @CheckReturnValue
    public String getTranslated(String key) {
        return tlumaczenia.get(language, key);
    }

    @CheckReturnValue
    public String getTranslated(String key, String ...argi) {
        return tlumaczenia.get(language, key, argi);
    }

    @CheckReturnValue
    public String getTranslated(String key, Object ...argi) {
        ArrayList<String> parsedArgi = new ArrayList<>();
        for (Object arg : argi) {
            parsedArgi.add(arg.toString());
        }
        return tlumaczenia.get(language, key, parsedArgi.toArray(new String[] {}));
    }

    public EmbedBuilder getBaseEmbed() {
        JDA shard = shardManager.getShardById(0);
        if (shard == null) throw new IllegalStateException("bot nie załadowany poprawnie");
        return getBaseEmbed(shard.getSelfUser().getName(),
                UserUtil.getAvatarUrl(shard.getSelfUser()));
    }

    public EmbedBuilder getBaseEmbed(String authorText) {
        JDA shard = shardManager.getShardById(0);
        if (shard == null) throw new IllegalStateException("bot nie załadowany poprawnie");
        return getBaseEmbed(authorText, UserUtil.getAvatarUrl(shard.getSelfUser()));
    }

    public EmbedBuilder getBaseEmbed(String authorText, String authorImageUrl) {
        JDA shard = shardManager.getShardById(0);
        if (shard == null) throw new IllegalStateException("bot nie załadowany poprawnie");
        return new EmbedBuilder()
                .setColor(Color.decode(BRAND_COLOR))
                .setAuthor(authorText, null, authorImageUrl)
                .setFooter("© " + shard.getSelfUser().getName(),
                        UserUtil.getAvatarUrl(shard.getSelfUser()));
    }

    public boolean canTalk() {
        return CommonUtil.canTalk(getMessageChannel());
    }

    public GuildChannel getGuildChannel() {
        return event.isFromGuild() ? (GuildChannel) event.getChannel() : null;
    }
}
