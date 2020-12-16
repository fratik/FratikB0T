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

package pl.fratik.moderation.listeners;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseBuilder;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;
import pl.fratik.moderation.utils.WarnUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.regex.Matcher;

public class LinkListener {

    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final CasesDao casesDao;
    private final Cache<GuildConfig> gcCache;
    public LinkListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, ShardManager shardManager, CasesDao casesDao, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.managerKomend = managerKomend;
        this.shardManager = shardManager;
        this.casesDao = casesDao;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
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
        if (!e.isFromType(ChannelType.TEXT) || e.getAuthor().isBot() || e.getMember() == null) return;
        if (!e.getTextChannel().canTalk()) return;
        if (!isAntilink(e.getTextChannel())) return;
        if (!e.getGuild().getSelfMember().canInteract(e.getMember())) return;
        String content = e.getMessage().getContentRaw();
        Matcher matcher = CommonUtil.URL_PATTERN.matcher(content);
        if (!matcher.find()) return;
        try {
            Thread.sleep(1250); // poczekaj na antiinvite/inne boty
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            e.getChannel().retrieveMessageById(e.getMessageId()).complete();
        } catch (ErrorResponseException err) {
            //wiadomość nie istnieje; została usunięta przez inny bot/listener
        }
        synchronized (e.getGuild()) {
            Case c = new CaseBuilder(e.getGuild()).setUser(e.getAuthor().getId()).setKara(Kara.WARN)
                    .setTimestamp(Instant.now()).createCase();
            c.setIssuerId(e.getJDA().getSelfUser());
            c.setReason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "antilink.reason"));
            String mlogchanStr = getModLogChan(e.getGuild());
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (!(mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))) {
                Message m = mlogchan.sendMessage(ModLogBuilder.generate(c,
                        e.getGuild(), shardManager, tlumaczenia.getLanguage(e.getGuild()), managerKomend, true, false))
                        .complete();
                c.setMessageId(m.getId());
            }
            CaseRow cr = casesDao.get(e.getGuild());
            cr.getCases().add(c);
            e.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(e.getMember()),
                    "antilink.notice", e.getAuthor().getAsMention(),
                    WarnUtil.countCases(cr, e.getAuthor().getId()),
                    managerKomend.getPrefixes(e.getGuild()).get(0))).queue();
            casesDao.save(cr);
            WarnUtil.takeAction(guildDao, casesDao, e.getMember(), e.getChannel(),
                    tlumaczenia.getLanguage(e.getGuild()), tlumaczenia, managerKomend);
            if (gcCache.get(e.getGuild().getId(), guildDao::get).isDeleteLinkMessage()) {
                try {
                    e.getMessage().delete().complete();
                } catch (Exception ignored) { }
            }
        }
    }

    private boolean isAntilink(TextChannel channel) {
        return gcCache.get(channel.getGuild().getId(), guildDao::get).isAntiLink() &&
                !gcCache.get(channel.getGuild().getId(), guildDao::get).getLinkchannels().contains(channel.getId());
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
         * @see User#isFake()
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
