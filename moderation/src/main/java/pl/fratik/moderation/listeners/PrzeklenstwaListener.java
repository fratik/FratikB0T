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
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseBuilder;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;
import pl.fratik.moderation.utils.WarnUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PrzeklenstwaListener {

    private final List<String> przeklenstwa;
    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final CasesDao casesDao;
    private static final Cache<String, Boolean> antiswearCache = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES).build();
    private static final Cache<String, String> modlogCache = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES).build();
    private static final Cache<String, List<String>> antiswearIgnoreChannelsCache = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES).build();

    public PrzeklenstwaListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, ShardManager shardManager, CasesDao casesDao) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.managerKomend = managerKomend;
        this.shardManager = shardManager;
        this.casesDao = casesDao;
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
        if (!e.isFromType(ChannelType.TEXT) || e.getAuthor().isBot() || e.getMember() == null) return;
        if (!e.getTextChannel().canTalk()) return;
        if (!isAntiswear(e.getTextChannel())) return;
        if (!e.getGuild().getSelfMember().canInteract(e.getMember())) return;
        String content = e.getMessage().getContentRaw();
        for (String przeklenstwo : przeklenstwa) {
            boolean res = processWord(przeklenstwo, content);
            if (res) {
                synchronized (e.getGuild()) {
                    Case c = new CaseBuilder(e.getGuild()).setUser(e.getAuthor().getId()).setKara(Kara.WARN)
                            .setTimestamp(Instant.now()).createCase();
                    c.setIssuerId(e.getJDA().getSelfUser());
                    c.setReason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "antiswear.reason"));
                    e.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(e.getMember()),
                            "antiswear.notice", e.getAuthor().getAsMention(),
                            managerKomend.getPrefixes(e.getGuild()).get(0))).queue();
                    String mlogchanStr = getModLogChan(e.getGuild());
                    if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
                    TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
                    if (!(mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))) {
                        Message m = mlogchan.sendMessage(ModLogBuilder.generate(c,
                                e.getGuild(), shardManager, tlumaczenia.getLanguage(e.getGuild()), managerKomend))
                                .complete();
                        c.setMessageId(m.getId());
                    }
                    CaseRow cr = casesDao.get(e.getGuild());
                    cr.getCases().add(c);
                    casesDao.save(cr);
                    WarnUtil.takeAction(guildDao, casesDao, e.getMember(), e.getChannel(),
                            tlumaczenia.getLanguage(e.getGuild()), tlumaczenia, managerKomend);
                }
            }
        }
    }

    private boolean isAntiswear(TextChannel channel) {
        //noinspection ConstantConditions - nie moze byc null
        return antiswearCache.get(channel.getGuild().getId(), id -> guildDao.get(channel.getGuild()).getAntiswear()) &&
                !antiswearIgnoreChannelsCache.get(channel.getGuild().getId(), id ->
                        guildDao.get(channel.getGuild()).getSwearchannels()).contains(channel.getId());
    }

    private String getModLogChan(Guild guild) {
        return modlogCache.get(guild.getId(), id -> guildDao.get(guild).getModLog());
    }

    private boolean processWord(String przeklenstwo, String content) {
        String[] slowa = content.split(" ");
        for (String slowo : slowa)
            if (przeklenstwo.equalsIgnoreCase(slowo)) return true;
        return false;
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent e) {
        if (!(e.getEntity() instanceof GuildConfig)) return;
        antiswearCache.invalidate(((GuildConfig) e.getEntity()).getGuildId());
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

        /**
         * The Author of the Message received as {@link net.dv8tion.jda.api.entities.User User} object.
         * <br>This will be never-null but might be a fake user if Message was sent via Webhook (Guild only).
         *
         * @return The Author of the Message.
         *
         * @see #isWebhookMessage()
         * @see net.dv8tion.jda.api.entities.User#isFake()
         */
        @Nonnull
        User getAuthor() {
            return message.getAuthor();
        }

        /**
         * The Author of the Message received as {@link net.dv8tion.jda.api.entities.Member Member} object.
         * <br>This will be {@code null} in case of Message being received in
         * a {@link net.dv8tion.jda.api.entities.PrivateChannel PrivateChannel}
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
