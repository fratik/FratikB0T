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

package pl.fratik.liczek.listener;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LiczekListener {
    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final Cache<GuildConfig> gcCache;

    public LiczekListener(GuildDao guildDao, Tlumaczenia tlumaczenia, RedisCacheManager rcm) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        gcCache = rcm.new CacheRetriever<GuildConfig>(){}.getCache();
    }

    private GuildConfig getGuildConfig(Guild guild) {
        return gcCache.get(guild.getId(), guildDao::get);
    }

    @Subscribe
    public void onLiczekSend(MessageReceivedEvent m) {
        if (!m.isFromGuild()) return;
        GuildConfig gc = getGuildConfig(m.getGuild());
        if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) return;
        if (!gc.getLiczekKanal().equals(m.getChannel().getId())) return;
        if (m.getAuthor().isBot() || m.getMessage().isWebhookMessage()) {
            m.getMessage().delete().queue();
            return;
        }

        String msg = m.getMessage().getContentRaw();
        if (msg.isEmpty()) {
            m.getMessage().delete().queue();
            return;
        }

        int liczba;
        try {
            liczba = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            m.getMessage().delete().queue();
            return;
        }

        synchronized (m.getGuild()) {
            List<Message> messages = getHistoryList((GuildMessageChannel) m.getChannel());
            int kiedysMessage;
            try {
                kiedysMessage = Integer.parseInt(messages.get(1).getContentRaw());
            } catch (NumberFormatException e) {
                //nieprawidłowa liczba
                return;
            }

            if (messages.size() < 2 || liczba != kiedysMessage+1 || messages.get(1).getAuthor().getId().equals(m.getAuthor().getId())) {
                m.getMessage().delete().queue();
                return;
            }

            updateTopic(m.getChannel());
        }
    }

    @Subscribe
    public void onLiczekEdit(MessageUpdateEvent m) {
        if (!m.isFromGuild()) return;
        GuildConfig gc = getGuildConfig(m.getGuild());
        if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) return;
        if (!gc.getLiczekKanal().equals(m.getChannel().getId())) return;
        m.getMessage().delete().complete();
        updateTopic(m.getChannel());
    }

    @Subscribe
    public void onLiczekDelete(MessageDeleteEvent m) {
        if (!m.isFromGuild()) return;
        GuildConfig gc = getGuildConfig(m.getGuild());
        if (gc.getLiczekKanal() == null || gc.getLiczekKanal().isEmpty()) return;
        if (!gc.getLiczekKanal().equals(m.getChannel().getId())) return;
        updateTopic(m.getChannel());
    }

    private List<Message> getHistoryList(GuildMessageChannel txt) {
        List<Message> msgs = new ArrayList<>();
        for (Message msg : txt.getIterableHistory().stream().limit(10).collect(Collectors.toList())) {
            if (!msg.isEdited()) {
                msgs.add(msg);
                if (msgs.size() == 2) break;
            } else msg.delete().queue();
        }
        return msgs;
    }

    private void updateTopic(MessageChannel txt) {
        if (!(txt instanceof TextChannel)) return; //???
        GuildConfig gc = getGuildConfig(((TextChannel) txt).getGuild());
        String trans = tlumaczenia.get(gc.getLanguage(), "liczek.topic");
        Message msg = getHistoryList((TextChannel) txt).get(0);
        try {
            int liczba = Integer.parseInt(msg.getContentDisplay());
            String format = String.format(trans, msg.getAuthor().getAsMention(), liczba+1);
            ((TextChannel) txt).getManager().setTopic(format).queue();
        } catch (Exception ignored) {}
    }
}
