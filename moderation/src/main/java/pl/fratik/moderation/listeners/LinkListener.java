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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Globals;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.manager.implementation.ManagerModulowImpl;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.WarnUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;

public class LinkListener {

    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ShardManager shardManager;
    private final CaseDao caseDao;
    private final Cache<GuildConfig> gcCache;
    private final EventBus eventBus;

    public LinkListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ShardManager shardManager, CaseDao caseDao, RedisCacheManager redisCacheManager, EventBus eventBus) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.shardManager = shardManager;
        this.caseDao = caseDao;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
        this.eventBus = eventBus;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(MessageReceivedEvent e) {
        checkEvent(new MessageEvent(e.getJDA(), e.getResponseNumber(), e.getMessage()));
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(MessageUpdateEvent e) {
        checkEvent(new MessageEvent(e.getJDA(), e.getResponseNumber(), e.getMessage()));
    }

    public void checkEvent(MessageEvent e) {
        if (e.isFromType(ChannelType.PRIVATE) || e.getAuthor().isBot() || e.getMember() == null) return;
        if (!CommonUtil.canTalk(e.getChannel())) return;
        if (!isAntilink((GuildChannel) e.getChannel())) return;
        if (!e.getGuild().getSelfMember().canInteract(e.getMember())) return;
        GuildConfig guildConfig = gcCache.get(e.getGuild().getId(), guildDao::get);
        if (guildConfig.isAntiLinkIgnoreAdmins() &&
                UserUtil.getPermlevel(e.getMember(), guildDao, shardManager, PermLevel.OWNER).getNum() > 0) return;
        if (guildConfig.getAntiLinkIgnoreRoles() != null) {
            for (String id : guildConfig.getAntiLinkIgnoreRoles())
                if (e.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(id))) return;
        }
        StringBuilder wiad = new StringBuilder();
        String content = e.getMessage().getContentRaw();
        Matcher matcher = CommonUtil.URL_PATTERN.matcher(content);
        if (!matcher.find()) return;
        boolean isInviteOnly = true;
        final boolean mediaAllowed = mediaAllowed((GuildChannel) e.getChannel());
        boolean isMedia = mediaAllowed;
        do {
            String text = matcher.group();
            boolean containsInvite = AntiInviteListener.containsInvite(text);
            if (!containsInvite) isInviteOnly = false;
            Boolean media = null;
            AtomicReference<String> contentType = new AtomicReference<>();
            if (mediaAllowed) {
                Function<String, Boolean> checkContent = url -> {
                    try {
                        NetworkUtil.ContentInformation ci = NetworkUtil.contentInformation(url);
                        if (ci != null && ci.getCode() == 200) {
                            contentType.set(ci.getContentType());
                            return ci.getContentType().startsWith("image/") || ci.getContentType().startsWith("video/");
                        }
                    } catch (Exception err) {
                        // niżej
                    }
                    return false;
                };
                media = checkContent.apply(text); // nigdy null
                if (!media) {
                    isMedia = false;
                }
            }
            wiad.append(text).append(" zaproszenie? ").append(containsInvite ? "tak" : "nie").append(" media? ");
            if (media != null) wiad.append(media ? "tak (" + contentType.get() + ")" : "nie");
            else wiad.append("[nie sprawdzane]");
            wiad.append("\n");
        } while (matcher.find());
        if (isMedia || isInviteOnly) {
            //media? nie reaguj (jeżeli mediaAllowed = false - isMedia = false)
            //zaproszenie? również nie, antiinvite ogarnie (lub nie, jeśli wyłączony)
            return;
        }
        try {
            e.getChannel().retrieveMessageById(e.getMessageId()).complete();
        } catch (ErrorResponseException err) {
            //wiadomość nie istnieje; została usunięta przez inny bot/listener
        }
        try {
            // Dlaczego linki są logowane? Po dzisiejszej (6.03.21) prośbie o pomoc, gdzie w nie wiadomo jakiej
            //wiadomości został wykryty link, uznaliśmy logowanie za potrzebne by móc dostosować logikę wykrywania linków
            wiad.setLength(wiad.length() - 1);
            Object logEvent = ManagerModulowImpl.moduleClassLoader.loadClass("pl.fratik.logs.GenericLogEvent")
                    .getDeclaredConstructor(String.class, String.class).newInstance("antilink", wiad.toString());
            eventBus.post(logEvent);
        } catch (Exception err) {
            // nic
        }
        if (guildConfig.isDeleteLinkMessage()) {
            try {
                e.getMessage().delete().complete();
            } catch (Exception ignored) { }
        }
        Case c = new Case.Builder(e.getMember(), Instant.now(), Kara.WARN).setIssuerId(Globals.clientId)
                .setReasonKey("antilink.reason").build();
        caseDao.createNew(null, c, false, e.getChannel(), tlumaczenia.getLanguage(e.getMember()));
        e.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(e.getMember()),
                "antilink.notice", e.getAuthor().getAsMention(),
                WarnUtil.countCases(caseDao.getCasesByMember(e.getMember()), e.getAuthor().getId()))).queue();
    }

    private boolean isAntilink(GuildChannel channel) {
        GuildConfig guildConfig = gcCache.get(channel.getGuild().getId(), guildDao::get);
        return guildConfig.isAntiLink() && !guildConfig.getLinkchannels().contains(channel.getId());
    }

    private boolean mediaAllowed(GuildChannel channel) {
        return gcCache.get(channel.getGuild().getId(), guildDao::get).isAntiLinkMediaAllowed();
    }

    private String getModLogChan(Guild guild) {
        return gcCache.get(guild.getId(), guildDao::get).getModLog();
    }

    private static class MessageEvent extends GenericMessageEvent {
        @Nonnull private final Message message;

        public MessageEvent(@Nonnull JDA api, long responseNumber, @Nonnull Message message) {
            super(api, responseNumber, message.getIdLong(), message.getChannel());
            this.message = message;
        }

        /**
         * The received {@link Message Message} object.
         *
         * @return The received {@link Message Message} object.
         */
        @Nonnull
        public Message getMessage()
        {
            return message;
        }

        /**
         * The Author of the Message received as {@link User User} object.
         * <br>This will be never-null but might be a fake user if Message was sent via Webhook (Guild only).
         *
         * @return The Author of the Message.
         *
         * @see #isWebhookMessage()
         */
        @Nonnull
        User getAuthor() {
            return message.getAuthor();
        }

        /**
         * The Author of the Message received as {@link Member Member} object.
         * <br>This will be {@code null} in case of Message being received in
         * a {@link PrivateChannel PrivateChannel}
         * or {@link #isWebhookMessage() isWebhookMessage()} returning {@code true}.
         *
         * @return The Author of the Message as null-able Member object.
         *
         * @see    #isWebhookMessage()
         */
        @Nullable
        public Member getMember() {
            return isFromType(ChannelType.TEXT) && !isWebhookMessage() ? getGuild().getMember(getAuthor()) : null;
        }

        /**
         * Whether or not the Message received was sent via a Webhook.
         * <br>This is a shortcut for {@code getMessage().isWebhookMessage()}.
         *
         * @return True, if the Message was sent via Webhook
         */
        boolean isWebhookMessage()
        {
            return getMessage().isWebhookMessage();
        }
    }
}
