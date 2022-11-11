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

package pl.fratik.moderation.listeners;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.crypto.AES;
import pl.fratik.core.crypto.CryptoException;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.StringUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.commands.PurgeCommand;
import pl.fratik.moderation.entity.*;
import redis.clients.jedis.exceptions.JedisException;

import javax.annotation.CheckReturnValue;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.awt.Color.decode;

public class LogListener {

    private final GuildDao guildDao;
    private final PurgeDao purgeDao;

    @Setter private static Tlumaczenia tlumaczenia;
    private final Cache<List<LogMessage>> cache;
    private final Cache<GuildConfig> gcCache;
    private final String password;
    @Getter private final List<String> znaneAkcje = new ArrayList<>();

    private static final Logger log = LoggerFactory.getLogger(LogListener.class);

    public LogListener(GuildDao guildDao, PurgeDao purgeDao, RedisCacheManager redisCacheManager, String password) {
        this.guildDao = guildDao;
        this.purgeDao = purgeDao;
        cache = redisCacheManager.new CacheRetriever<List<LogMessage>>(){}.setCanHandleErrors(true).getCache(900);
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
        this.password = password;
    }

    @Subscribe
    public void onMessage(MessageReceivedEvent messageReceivedEvent) {
        if (!messageReceivedEvent.isFromGuild()) return;
        try {
            List<LogMessage> messages = cache.get(messageReceivedEvent.getChannel().getId(), c -> new ArrayList<>());
            if (messages == null) throw new IllegalStateException("messages == null mimo compute'owania");
            if (messages.size() > 100) messages.remove(0);
            messages.add(new LogMessage(messageReceivedEvent.getMessage()));
            cache.put(messageReceivedEvent.getChannel().getId(), messages);
        } catch (JedisException e) {
            log.error("Redis nie odpowiada prawidłowo!", e);
        }
    }

    @Subscribe
    public void onMessageEdit(MessageUpdateEvent messageUpdateEvent) {
        if (!messageUpdateEvent.isFromGuild()) return;
        try {
            List<LogMessage> messages = cache.get(messageUpdateEvent.getChannel().getId(), c -> new ArrayList<>());
            if (messages == null) throw new IllegalStateException("messages == null mimo compute'owania");
            Message m = findMessage(messageUpdateEvent.getChannel(), messageUpdateEvent.getMessageId(), false, messages);
            if (m == null) {
                if (messages.size() > 100) messages.remove(0);
                messages.add(new LogMessage(messageUpdateEvent.getMessage()));
                znaneAkcje.remove(messageUpdateEvent.getMessageId());
                return;
            }
            messages.set(messages.indexOf(m), new LogMessage(messageUpdateEvent.getMessage()));
            cache.put(messageUpdateEvent.getChannel().getId(), messages);
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
            try {channel.sendMessageEmbeds(embed).queue();} catch (Exception ignored) {/*lul*/}
        } catch (JedisException e) {
            log.error("Redis nie odpowiada prawidłowo!", e);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessageRemoved(MessageDeleteEvent messageDeleteEvent) {
        if (!messageDeleteEvent.isFromGuild()) return;
        LogMessage m = findMessage(messageDeleteEvent.getChannel(), messageDeleteEvent.getMessageId());
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
            List<AuditLogEntry> audiologi = messageDeleteEvent.getGuild().retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).complete();
            User deletedBy = null;
            for (AuditLogEntry log : audiologi) {
                if (log.getType() == ActionType.MESSAGE_DELETE
                        && log.getTimeCreated().isAfter(OffsetDateTime.now().minusMinutes(1))
                        && messageDeleteEvent.getChannel().getId().equals(log.getOption(AuditLogOption.CHANNEL))
                        && m.getAuthorId() == log.getTargetIdLong()) {
                    deletedBy = log.getUser();
                    break;
                }
            }
            MessageEmbed embed = generateEmbed(LogType.DELETE, m, deletedBy, m.getContentRaw(), false);
            try {
                channel.sendMessageEmbeds(embed).queue();
            } catch (Exception ignored) {
                /*lul*/
            }
        } catch (Exception e) {
            MessageEmbed embed = generateEmbed(LogType.DELETE, m, null, m.getContentRaw(), true);
            try {
                channel.sendMessageEmbeds(embed).queue();
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
                    try {
                        return new WiadomoscImpl(m.getId(),
                                new pl.fratik.api.entity.User(m.getAuthor()), AES.encryptAsB64(m.getContentRaw(), password),
                                m.getTimeCreated().toInstant().toEpochMilli(),
                                m.isEdited() ? m.getTimeEdited().toInstant().toEpochMilli() : null);
                    } catch (CryptoException e) {
                        throw new IllegalStateException("Nie udało się zaszyfrować wiadomośći!", e);
                    }
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
            channel.sendMessageEmbeds(embed).queue();
        } catch (Exception ignored) {
            /*lul*/
        }
    }

    public void pushMessage(Message msg) {
        try {
            List<LogMessage> kesz = Objects.requireNonNull(cache.get(msg.getChannel().getId(), c -> new ArrayList<>()));
            if (kesz.stream().map(ISnowflake::getId).anyMatch(o -> o.equals(msg.getId())))
                return;
            kesz.add(new LogMessage(msg));
            cache.put(msg.getChannel().getId(), kesz);
        } catch (JedisException e) {
            log.error("Redis nie odpowiada prawidłowo!", e);
        }
    }

    private LogMessage findMessage(MessageChannel channel, String id) {
        return findMessage(channel, id, true);
    }

    private LogMessage findMessage(MessageChannel channel, String id, boolean delete) {
        try {
            List<LogMessage> kesz = Objects.requireNonNull(cache.get(channel.getId(), c -> new ArrayList<>()));
            return findMessage(channel, id, delete, kesz);
        } catch (JedisException e) {
            log.error("Redis nie odpowiada prawidłowo!", e);
            return null;
        }
    }

    private LogMessage findMessage(MessageChannel channel, String id, boolean delete, List<LogMessage> kesz) {
        try {
            for (LogMessage m : kesz)
                if (m.getId().equals(id)) {
                    if (!delete) return m;
                    kesz.remove(m);
                    cache.put(channel.getId(), kesz);
                    return m;
                }
        } catch (JedisException e) {
            log.error("Redis nie odpowiada prawidłowo!", e);
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
            String oldCnt = oldContent;
            String oldCnt2 = null;
            if (oldContent.length() >= 1021) {
                oldCnt = oldContent.substring(0, 1021).trim() + "...";
                oldCnt2 = "..." + oldContent.substring(1022).trim();
            }
            String newCnt = message.getContentRaw();
            String newCnt2 = null;
            if (oldContent.length() >= 1021) {
                newCnt = message.getContentRaw().substring(0, 1021).trim() + "...";
                newCnt2 = "..." + message.getContentRaw().substring(1022).trim();
            }
            eb.addField(tlumaczenia.get(l, "fulllog.old.content"), oldCnt, false);
            if (oldCnt2 != null) eb.addField(tlumaczenia.get(l, "fulllog.old.content.pt2"), oldCnt2, false);
            eb.addField(tlumaczenia.get(l, "fulllog.new.content"), newCnt, false);
            if (newCnt2 != null) eb.addField(tlumaczenia.get(l, "fulllog.new.content.pt2"), newCnt2, false);
            pushChannel(eb, l, message);
            eb.addField(tlumaczenia.get(l, "fulllog.message.id"), message.getId(), false);
            eb.setColor(decode("#ffa500"));
            eb.setFooter(tlumaczenia.get(l, "fulllog.edit"), null);
        }
        if (type == LogType.DELETE) {
            String cnt = message.getContentRaw();
            String cnt2 = null;
            if (message.getContentRaw().length() >= 1024) {
                cnt = message.getContentRaw().substring(0, 1021).trim() + "...";
                cnt2 = "..." + message.getContentRaw().substring(1022).trim();
            }
            eb.addField(tlumaczenia.get(l, "fulllog.content"), cnt, false);
            if (cnt2 != null) eb.addField(tlumaczenia.get(l, "fulllog.content.pt2"), cnt2, false);
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
                String.format("%s (%s[%s])", message.getChannel().getAsMention(),
                        message.getChannel().getName(), message.getChannel().getId()), false);
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
        LoggerFactory.getLogger(getClass()).warn("Wiadomość niezrozumiała!");
    }

    private TextChannel getChannel(Guild guild) {
        GuildConfig gc = gcCache.get(guild.getId(), guildDao::get);
        if (gc.getFullLogs() == null) return null;
        String id = null;
        if (!gc.getFullLogs().isEmpty()) {
            TextChannel kanal = guild.getTextChannelById(gc.getFullLogs());
            if (kanal == null) return null;
            id = kanal.getId();
        }
        if (id == null) return null;
        return guild.getTextChannelById(id);
    }

    private enum LogType {
        EDIT, DELETE, BULKDELETE
    }

}
