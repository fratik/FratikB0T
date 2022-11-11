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
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Globals;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.WarnUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PrzeklenstwaListener {

    private final List<String> przeklenstwa;
    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ShardManager shardManager;
    private final CaseDao caseDao;
    private final Cache<GuildConfig> gcCache;
    public PrzeklenstwaListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ShardManager shardManager, CaseDao caseDao, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.shardManager = shardManager;
        this.caseDao = caseDao;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
        try {
            String data = CommonUtil.fromStream(getClass().getResourceAsStream("/przeklenstwa.json"));
            przeklenstwa = GsonUtil.GSON.fromJson(data, new TypeToken<List<String>>() {}.getType());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
        if (!isAntiswear((GuildChannel) e.getChannel())) return;
        if (!e.getGuild().getSelfMember().canInteract(e.getMember())) return;
        String content = e.getMessage().getContentRaw();
        GuildConfig gc = getGuildConfig(e.getGuild());
        List<String> przeklenstwa; //NOSONAR
        if (gc.getCustomAntiSwearWords() != null && !gc.getCustomAntiSwearWords().isEmpty()) {
            przeklenstwa = new ArrayList<>(this.przeklenstwa);
            przeklenstwa.addAll(gc.getCustomAntiSwearWords());
        } else przeklenstwa = this.przeklenstwa;
        for (String przeklenstwo : przeklenstwa) {
            boolean res = processWord(przeklenstwo, content);
            if (res) {
                Case c = new Case.Builder(e.getMember(), Instant.now(), Kara.WARN).setIssuerId(Globals.clientId)
                        .setReasonKey("antiswear.reason").build();
                caseDao.createNew(null, c, false, e.getChannel(), tlumaczenia.getLanguage(e.getMember()));
                MessageCreateAction m = e.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(e.getMember()),
                        "antiswear.notice", e.getAuthor().getAsMention(),
                        WarnUtil.countCases(caseDao.getCasesByMember(e.getMember()), e.getAuthor().getId())));
                boolean deleteSwearMessage = gc.isDeleteSwearMessage();
                if (deleteSwearMessage) m.queue();
                else m.setMessageReference(e.getMessage()).queue();
                if (deleteSwearMessage) {
                    try {
                        e.getMessage().delete().complete();
                    } catch (Exception ignored) { }
                }
                return;
            }
        }
    }

    private GuildConfig getGuildConfig(Guild guild) {
        return gcCache.get(guild.getId(), guildDao::get);
    }

    private boolean isAntiswear(GuildChannel channel) {
        return getGuildConfig(channel.getGuild()).getAntiswear() &&
                !getGuildConfig(channel.getGuild()).getSwearchannels().contains(channel.getId());
    }

    private String getModLogChan(Guild guild) {
        return getGuildConfig(guild).getModLog();
    }

    private boolean processWord(String przeklenstwo, String content) {
        String[] slowa = content.split(" ");
        for (String slowo : slowa)
            if (przeklenstwo.equalsIgnoreCase(slowo)) return true;
        return false;
    }

    private static class MessageEvent extends GenericMessageEvent {
        @Nonnull private final Message message;

        public MessageEvent(@Nonnull JDA api, long responseNumber, @Nonnull Message message) {
            super(api, responseNumber, message.getIdLong(), message.getChannel());
            this.message = message;
        }

        /**
         * The received {@link net.dv8tion.jda.api.entities.Message Message} object.
         *
         * @return The received {@link net.dv8tion.jda.api.entities.Message Message} object.
         */
        @Nonnull
        public Message getMessage()
        {
            return message;
        }

        @Nonnull
        User getAuthor() {
            return message.getAuthor();
        }

        /**
         * The Author of the Message received as {@link net.dv8tion.jda.api.entities.Member Member} object.
         * <br>This will be {@code null} in case of Message being received in
         * a {@link net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel PrivateChannel}
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
