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

package pl.fratik.moderation.listeners;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.StringUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.commands.PurgeCommand;
import pl.fratik.moderation.entity.*;

import javax.annotation.CheckReturnValue;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.awt.Color.decode;

public class LogListener {

    private final GuildDao guildDao;
    private final PurgeDao purgeDao;

    @Setter private static Tlumaczenia tlumaczenia;
    private static final Cache<TextChannel, List<Message>>
            cache = Caffeine.newBuilder().maximumSize(300).expireAfterWrite(10, TimeUnit.MINUTES).build();
    @Getter private final List<String> znaneAkcje = new ArrayList<>();

    @Getter private static HashMap<Guild, List<Case>> knownCases = new HashMap<>();

    private final Cache<Guild, TextChannel> logChannelCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100).build();

    public LogListener(GuildDao guildDao, PurgeDao purgeDao) {
        this.guildDao = guildDao;
        this.purgeDao = purgeDao;
    }

    @Subscribe
    public void onMessage(MessageReceivedEvent messageReceivedEvent) {
        if (!messageReceivedEvent.isFromGuild()) return;
        List<Message> messages = cache.get(messageReceivedEvent.getTextChannel(), c -> new ArrayList<>());
        if (messages == null) throw new IllegalStateException("messages == null mimo compute'owania");
        if (messages.size() <= 100) messages.add(messageReceivedEvent.getMessage());
        else {
            messages.remove(0);
            messages.add(messageReceivedEvent.getMessage());
        }
    }

    @Subscribe
    public void onMessageEdit(MessageUpdateEvent messageUpdateEvent) {
        if (!messageUpdateEvent.isFromGuild()) return;
        List<Message> messages = cache.get(messageUpdateEvent.getTextChannel(), c -> new ArrayList<>());
        if (messages == null) throw new IllegalStateException("messages == null mimo compute'owania");
        Message m = findMessage(messageUpdateEvent.getTextChannel(), messageUpdateEvent.getMessageId(), false);
        if (m == null) {
            if (messages.size() <= 100) messages.add(messageUpdateEvent.getMessage());
            else {
                messages.remove(0);
                messages.add(messageUpdateEvent.getMessage());
            }
            znaneAkcje.remove(messageUpdateEvent.getMessageId());
            return;
        }
        messages.set(messages.indexOf(m), messageUpdateEvent.getMessage());
        if (znaneAkcje.contains(messageUpdateEvent.getMessageId())) {
            znaneAkcje.remove(messageUpdateEvent.getMessageId());
            return;
        }
        TextChannel channel = getChannel(messageUpdateEvent.getGuild());
        if (channel == null || !channel.canTalk()) {
            return;
        }
        if (messageUpdateEvent.getMessage().getContentRaw().equals(m.getContentRaw())) {
            //zmieniony embed/attachment, ignoruj
            return;
        }
        if (messageUpdateEvent.getMessage().getContentRaw().length() >= 1024 || m.getContentRaw().length() >= 1024) {
            return;
        }
        MessageEmbed embed = generateEmbed(LogType.EDIT, messageUpdateEvent.getMessage(), null, m.getContentRaw(), false);
        try {channel.sendMessage(embed).queue();} catch (Exception ignored) {/*lul*/}
    }

    @Subscribe
    public void onMessageRemoved(MessageDeleteEvent messageDeleteEvent) {
        if (!messageDeleteEvent.isFromGuild()) return;
        Message m = findMessage(messageDeleteEvent.getTextChannel(), messageDeleteEvent.getMessageId());
        if (m == null) {
            znaneAkcje.remove(messageDeleteEvent.getMessageId());
            return;
        }
        TextChannel channel = getChannel(messageDeleteEvent.getGuild());
        if (channel == null || !channel.canTalk()) {
            znaneAkcje.remove(messageDeleteEvent.getMessageId());
            return;
        }
        if (znaneAkcje.remove(messageDeleteEvent.getMessageId())) return;
        try {
            messageDeleteEvent.getGuild().retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).queue(
                    audiologi -> {
                        User deletedBy = null;
                        for (AuditLogEntry log : audiologi) {
                            if (log.getType() == ActionType.MESSAGE_DELETE
                                    && log.getTimeCreated().isAfter(OffsetDateTime.now().minusMinutes(1))
                                    && messageDeleteEvent.getChannel().getId().equals(log.getOption(AuditLogOption.CHANNEL))
                                    && m.getAuthor().getIdLong() == log.getTargetIdLong()) {
                                deletedBy = log.getUser();
                                break;
                            }
                        }
                        MessageEmbed embed = generateEmbed(LogType.DELETE, m, deletedBy, m.getContentRaw(), false);
                        try {
                            channel.sendMessage(embed).queue();
                        } catch (Exception ignored) {
                            /*lul*/
                        }
                    },
                    error -> {
                        MessageEmbed embed = generateEmbed(LogType.DELETE, m, null, m.getContentRaw(), true);
                        try {
                            channel.sendMessage(embed).queue();
                        } catch (Exception ignored) {
                            /*lul*/
                        }
                    }
            );
        } catch (Exception e) {
            MessageEmbed embed = generateEmbed(LogType.DELETE, m, null, m.getContentRaw(), true);
            try {
                channel.sendMessage(embed).queue();
            } catch (Exception ignored) {
                /*lul*/
            }
        }

    }

    @Subscribe
    public void onMessageBulkRemove(MessageBulkDeleteEvent messageBulkDeleteEvent) {
        List<Message> messages = new ArrayList<>();
        for (String id : messageBulkDeleteEvent.getMessageIds()) {
            messages.add(findMessage(messageBulkDeleteEvent.getChannel(), id));
        }
        if (messages.stream().allMatch(Objects::isNull)) return;
        if (messages.get(0) == null) return;
        TextChannel channel = getChannel(messageBulkDeleteEvent.getGuild());
        if (channel == null || !channel.canTalk()) return;
        boolean errored = false;
        String purgerId = PurgeCommand.getZnanePurge().remove(channel.getGuild().getId());
        Purge purge = null;
        try {
            purge = new Purge(StringUtil.generateId(7, true, true, false));
            purge.setChannelId(messageBulkDeleteEvent.getChannel().getId());
            purge.setGuildId(channel.getGuild().getId());
            if (purgerId != null) purge.setPurgedBy(new pl.fratik.api.entity.User(messageBulkDeleteEvent.getJDA().retrieveUserById(purgerId).complete()));
            purge.setPurgedOn(Instant.now().toEpochMilli());
            purge.getWiadomosci().addAll(messages.stream().map(m -> {
                if (m != null) {
                    //noinspection ConstantConditions (jak jest edited musi byc getTimeEdited)
                    return new WiadomoscImpl(m.getId(),
                            new pl.fratik.api.entity.User(m.getAuthor()), m.getContentRaw(),
                            m.getTimeCreated().toInstant().toEpochMilli(),
                            m.isEdited() ? m.getTimeEdited().toInstant().toEpochMilli() : null);
                } else {
                    return new FakeWiadomosc();
                }
            }).collect(Collectors.toList()));
            purgeDao.save(purge);
        } catch (Exception e) {
            errored = true;
        }
        MessageEmbed embed = generateEmbed(LogType.BULKDELETE, messages.get(0),
                purgerId != null ? messageBulkDeleteEvent.getJDA().retrieveUserById(purgerId).complete() : null,
                !errored ? Ustawienia.instance.botUrl + "/purges/" + purge.getId() : null, errored);
        try {
            channel.sendMessage(embed).queue();
        } catch (Exception ignored) {
            /*lul*/
        }
    }

    public void pushMessage(Message msg) {
        List<Message> kesz = Objects.requireNonNull(cache.get(msg.getTextChannel(), c -> new ArrayList<>()));
        if (kesz.stream().map(ISnowflake::getId).anyMatch(o -> o.equals(msg.getId())))
            return;
        kesz.add(msg);
        cache.put(msg.getTextChannel(), kesz);
    }

    private Message findMessage(TextChannel channel, String id) {
        return findMessage(channel, id, true);
    }

    private Message findMessage(TextChannel channel, String id, boolean delete) {
        try {
            List<Message> kesz = Objects.requireNonNull(cache.get(channel, c -> new ArrayList<>()));
            for (Message m : kesz)
                if (m.getId().equals(id)) {
                    if (!delete) return m;
                    kesz.remove(m);
                    cache.put(channel, kesz);
                    return m;
                }
        } catch (Exception ignored) {
            /*lul*/
        }
        return null;
    }

    @CheckReturnValue
    private MessageEmbed generateEmbed(LogType type, Message message, User deletedBy, String oldContent, boolean errored) {
        return generateEmbed(type, message, deletedBy, oldContent, errored, tlumaczenia.getLanguage(message.getGuild()));
    }

    @CheckReturnValue
    private MessageEmbed generateEmbed(LogType type, Message message, User deletedBy, String oldContent, boolean errored, Language l) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTimestamp(Instant.now());
        if (type == LogType.EDIT || type == LogType.DELETE) eb.setAuthor(UserUtil.formatDiscrim(message.getAuthor()),
                null, message.getAuthor().getEffectiveAvatarUrl().replace(".webp", ".png"));
        if (type == LogType.EDIT) {
            eb.setTimestamp(message.getTimeEdited());
            eb.addField(tlumaczenia.get(l, "fulllog.old.content"), oldContent, false);
            eb.addField(tlumaczenia.get(l, "fulllog.new.content"), message.getContentRaw(), false);
            pushChannel(eb, l, message);
            eb.addField(tlumaczenia.get(l, "fulllog.message.id"), message.getId(), false);
            eb.setColor(decode("#ffa500"));
            eb.setFooter(tlumaczenia.get(l, "fulllog.edit"), null);
        }
        if (type == LogType.DELETE) {
            eb.addField(tlumaczenia.get(l, "fulllog.content"), message.getContentRaw(), false);
            pushChannel(eb, l, message);
            eb.addField(tlumaczenia.get(l, "fulllog.message.id"), message.getId(), false);
            String rmby = "fulllog.removed.by";
            if (deletedBy != null)
                eb.addField(tlumaczenia.get(l, rmby), UserUtil.formatDiscrim(deletedBy), false);
            else if (errored) eb.addField(tlumaczenia.get(l, rmby),
                    tlumaczenia.get(l, "fulllog.removed.by.errored"), false);
            else eb.addField(tlumaczenia.get(l, rmby),
                        tlumaczenia.get(l, "fulllog.removed.by.unknown"), false);
            eb.setColor(decode("#ff0000"));
            eb.setFooter(tlumaczenia.get(l, "fulllog.removed"), null);
        }
        if (type == LogType.BULKDELETE) {
            if (!errored) eb.addField(tlumaczenia.get(l, "fulllog.messages"),
                    "[" + tlumaczenia.get(l, "fulllog.messages.on.dashboard") + "](" +
                            oldContent + ")", false);
            else eb.addField(tlumaczenia.get(l, "fulllog.messages"),
                    tlumaczenia.get(l, "fulllog.messages.errored"),false);
            pushChannel(eb, l, message);
            eb.setFooter(tlumaczenia.get(l, "fulllog.bulkdelete"), null);
            eb.setColor(decode("#00aaff"));
            if (deletedBy != null) eb.setAuthor(deletedBy.getAsTag(), null, deletedBy.getEffectiveAvatarUrl());
        }
        return eb.build();
    }

    private void pushChannel(EmbedBuilder eb, Language l, Message message) {
        eb.addField(tlumaczenia.get(l, "fulllog.channel"),
                String.format("%s (%s[%s])", message.getTextChannel().getAsMention(),
                        message.getTextChannel().getName(), message.getTextChannel().getId()), false);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTo().equals("moderation")) return;
        LoggerFactory.getLogger(getClass()).info("Wiadomość od {}: {}", e.getFrom(), e.getMessage());
        if (e.getMessage().startsWith("znaneAkcje")) {
            String komenda = e.getMessage().replace("znaneAkcje-", "");
            if (komenda.startsWith("add")) {
                String id = komenda.replace("add:", "");
                znaneAkcje.add(id);
                return;
            }
            if (komenda.startsWith("remove")) {
                String id = komenda.replace("remove:", "");
                znaneAkcje.remove(id);
                return;
            }
        }
        LoggerFactory.getLogger(getClass()).warn("Wiadomość niezrozumiała!");
    }

    private TextChannel getChannel(Guild guild) {
        return logChannelCache.get(guild, g -> {
            GuildConfig gc = guildDao.get(guild);
            if (gc.getFullLogs() == null) return null;
            if (!gc.getFullLogs().isEmpty()) return g.getTextChannelById(gc.getFullLogs());
            return null;
        });
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent event) {
        if (event.getEntity() instanceof GuildConfig) {
            for (Guild guild : logChannelCache.asMap().keySet()) {
                if (((GuildConfig) event.getEntity()).getGuildId().equals(guild.getId())) {
                    logChannelCache.invalidate(guild);
                    return;
                }
            }
        }
    }

    private enum LogType {
        EDIT, DELETE, BULKDELETE
    }

}
