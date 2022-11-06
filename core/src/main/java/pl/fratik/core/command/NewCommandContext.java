/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static pl.fratik.core.Statyczne.BRAND_COLOR;

public class NewCommandContext {
    @Getter private final ShardManager shardManager;
    @Getter private final NewCommand command;
    @Getter private final Tlumaczenia tlumaczenia;
    private final Language language;
    private final CommandInteraction interaction;
    @Getter private final Map<String, OptionMapping> arguments;

    public NewCommandContext(ShardManager shardManager, NewCommand command, Tlumaczenia tlumaczenia, CommandInteraction interaction) {
        this.shardManager = shardManager;
        this.command = command;
        this.tlumaczenia = tlumaczenia;
        this.interaction = interaction;
        Map<String, OptionMapping> arguments = new HashMap<>();
        interaction.getOptions().forEach(m -> arguments.put(m.getName(), m));
        this.arguments = Collections.unmodifiableMap(arguments);
        Language language = Language.getByDiscordLocale(interaction.getUserLocale());
        if (language == Language.DEFAULT && interaction.isFromGuild())
            language = Language.getByDiscordLocale(interaction.getGuildLocale());
        this.language = language;
    }

    public String getCommandPath() {
        return interaction.getCommandPath();
    }

    public InteractionHook defer(boolean ephemeral) {
        return interaction.deferReply(ephemeral).complete();
    }

    public void deferAsync(boolean ephemeral) {
        interaction.deferReply(ephemeral).queue();
    }

    public InteractionHook reply(String content) {
        return interaction.reply(content).complete();
    }

    public InteractionHook reply(MessageEmbed embed) {
        return interaction.replyEmbeds(embed).complete();
    }

    public InteractionHook reply(Message message) {
        return reply(MessageCreateData.fromMessage(message));
    }

    public InteractionHook reply(MessageCreateData message) {
        return interaction.reply(message).complete();
    }

    public InteractionHook reply(Collection<MessageEmbed> embeds) {
        return interaction.replyEmbeds(embeds).complete();
    }

    public InteractionHook replyEphemeral(String content) {
        return interaction.reply(content).setEphemeral(true).complete();
    }

    public InteractionHook replyEphemeral(MessageEmbed embed) {
        return interaction.replyEmbeds(embed).setEphemeral(true).complete();
    }

    public InteractionHook replyEphemeral(Message message) {
        return interaction.reply(MessageCreateData.fromMessage(message)).setEphemeral(true).complete();
    }

    public InteractionHook replyEphemeral(Collection<MessageEmbed> embeds) {
        return interaction.replyEmbeds(embeds).setEphemeral(true).complete();
    }

    public Message sendMessage(String content) {
        return interaction.getHook().sendMessage(content).complete();
    }

    public Message sendMessage(MessageCreateData builder) {
        return interaction.getHook().sendMessage(builder).complete();
    }

    public Message sendMessage(String content, ActionRow actionRow) {
        return interaction.getHook().sendMessage(content).setComponents(actionRow).complete();
    }

    public Message sendMessage(Message message) {
        return interaction.getHook().sendMessage(MessageCreateData.fromMessage(message)).complete();
    }

    public Message sendMessage(Collection<MessageEmbed> embeds) {
        return interaction.getHook().sendMessageEmbeds(embeds).complete();
    }

    public Message sendMessage(MessageEmbed embed) {
        return interaction.getHook().sendMessageEmbeds(embed).complete();
    }

    public Message sendMessage(String fileName, byte[] data) {
        return interaction.getHook().sendFiles(FileUpload.fromData(data, fileName)).complete();
    }

    public Message editOriginal(String content) {
        return interaction.getHook().editOriginal(content).complete();
    }

    public Message editOriginal(Message message) {
        return interaction.getHook().editOriginal(MessageEditData.fromMessage(message)).complete();
    }

    public Message editOriginal(Collection<MessageEmbed> embeds) {
        return interaction.getHook().editOriginalEmbeds(embeds).complete();
    }

    public Message editOriginal(String content, Collection<MessageEmbed> embeds) {
        return interaction.getHook().editOriginalEmbeds(embeds).setContent(content).complete();
    }

    //todo nie udostępniać getInteraction(), tylko dodać wrappery do jego potrzebnych funkcji tutaj

    public MessageChannelUnion getChannel() {
        return (MessageChannelUnion) interaction.getChannel();
    }

    public Guild getGuild() {
        return interaction.isFromGuild() ? interaction.getGuild() : null;
    }

    public User getSender() {
        return interaction.getUser();
    }

    public Member getMember() {
        return interaction.getMember();
    }

    public String getTranslated(String key) {
        return tlumaczenia.get(language, key);
    }

    public String getTranslated(String key, Object... toReplace) {
        return tlumaczenia.get(language, key, toReplace);
    }

    public Language getLanguage() {
        return language == Language.DEFAULT ? Language.getDefault() : language;
    }
    public <T> T getArgumentOr(String key, T or, Function<? super OptionMapping, ? extends T> resolver) {
        if (getArguments().containsKey(key)) return resolver.apply(getArguments().get(key));
        return or;
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
}
