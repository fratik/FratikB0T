/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.ArgsMissingException;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;

import javax.annotation.CheckReturnValue;
import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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

    private static final Pattern URLPATTERN = Pattern.compile("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\." +
            "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]" +
            "\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})");

    public CommandContext(ShardManager shardManager, Tlumaczenia tlumaczenia, Command command, MessageReceivedEvent event, String prefix, String label) {
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        this.command = command;
        this.prefix = prefix;
        if (event.getChannelType().isGuild()) this.language = tlumaczenia.getLanguage(event.getMember());
        else this.language = tlumaczenia.getLanguage(event.getAuthor());
        this.event = event;
        this.label = label;
        this.rawArgs = null;
        this.args = null;
    }

    public CommandContext(ShardManager shardManager, Tlumaczenia tlumaczenia, Command command, MessageReceivedEvent event, String prefix, String label, String[] args) throws ArgsMissingException {
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        this.command = command;
        this.prefix = prefix;
        if (event.getChannelType().isGuild()) this.language = tlumaczenia.getLanguage(event.getMember());
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

    public TextChannel getChannel() {
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
        return event.getChannel().sendMessage(String.valueOf(message).replaceAll("@(everyone|here)", "@\u200b$1")).complete();
    }

//    @Deprecated
    public Message send(MessageEmbed message) {
        return event.getChannel().sendMessage(message).complete();
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
        event.getChannel().sendMessage(String.valueOf(message).replaceAll("@(everyone|here)", "@\u200b$1")).queue(callback);
    }

    public void send(MessageEmbed message, Consumer<Message> callback) {
        event.getChannel().sendMessage(message).queue(callback);
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
                .setColor(Color.decode("#bef7c3"))
                .setAuthor(authorText, null, authorImageUrl)
                .setFooter("© " + shard.getSelfUser().getName(),
                        UserUtil.getAvatarUrl(shard.getSelfUser()));
    }
}
