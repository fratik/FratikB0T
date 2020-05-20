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

package pl.fratik.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

class MemberListener {
    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final Cache<String, GuildConfig> gcCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

    MemberListener(GuildDao guildDao, Tlumaczenia tlumaczenia) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
    }

    @Subscribe
    public void onMemberJoinEvent(GuildMemberJoinEvent e) {
        autorole(e);
        przywitanie(e);
    }

    @Subscribe
    public void onMemberLeaveEvent(GuildMemberLeaveEvent e) {
        GuildConfig gc = getGuildConfig(e.getGuild());
        for (Map.Entry<String, String> ch : gc.getPozegnania().entrySet()) {
            TextChannel cha = e.getGuild().getTextChannelById(ch.getKey());
            if (cha == null) continue;
            cha.sendMessage(ch.getValue()
                    .replaceAll("\\{\\{user}}", e.getMember().getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"))
                    .replaceAll("\\{\\{server}}", e.getGuild().getName().replaceAll("@(everyone|here)", "@\u200b$1"))).queue();
        }
    }

    private void autorole(GuildMemberJoinEvent e) {
        GuildConfig gc = getGuildConfig(e.getGuild());
        List<Role> role = new ArrayList<>();
        if (gc.getAutoroleZa1szaWiadomosc()) return;
        for (String id : gc.getAutorole()) {
            Role rola = CommonUtil.supressException((Function<String, Role>) e.getGuild()::getRoleById, id);
            if (rola == null || !e.getGuild().getSelfMember().canInteract(rola)) continue;
            role.add(rola);
        }
        if (role.isEmpty()) return;
        e.getGuild().modifyMemberRoles(e.getMember(), role, new ArrayList<>()).queue();
    }

    private void przywitanie(GuildMemberJoinEvent e) {
        GuildConfig gc = getGuildConfig(e.getGuild());
        for (Map.Entry<String, String> ch : gc.getPowitania().entrySet()) {
            TextChannel cha = e.getGuild().getTextChannelById(ch.getKey());
            if (cha == null || !cha.canTalk()) continue;
            cha.sendMessage(ch.getValue()
                    .replaceAll("\\{\\{user}}", e.getMember().getUser().getAsTag().replaceAll("@(everyone|here)", "@\u200b$1"))
                    .replaceAll("\\{\\{mention}}", e.getMember().getAsMention())
                    .replaceAll("\\{\\{server}}", e.getGuild().getName().replaceAll("@(everyone|here)", "@\u200b$1"))).queue();
        }
    }

    private GuildConfig getGuildConfig(Guild guild) {
        return gcCache.get(guild.getId(), guildDao::get);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(MessageReceivedEvent m) {
        if (m.getChannelType() != ChannelType.TEXT || m.isWebhookMessage()) return;
        GuildConfig gc = getGuildConfig(m.getGuild());
        if (!gc.getAutoroleZa1szaWiadomosc()) return;
        List<Role> role = new ArrayList<>();
        for (String id : gc.getAutorole()) {
            Role rola = CommonUtil.supressException((Function<String, Role>) m.getGuild()::getRoleById, id);
            if (rola == null || !m.getGuild().getSelfMember().canInteract(rola)) continue;
            role.add(rola);
        }
        if (role.isEmpty()) return;
        if (role.stream().anyMatch(Objects.requireNonNull(m.getMember()).getRoles()::contains)) return;
        try {
            m.getGuild().modifyMemberRoles(m.getMember(), role, new ArrayList<>()).queue();
        } catch (Exception e) {
            // nie mamy permów, zawijamy się
        }
    }
    
    @Subscribe
    public void onDatabaseUpdateEvent(DatabaseUpdateEvent e) {
        if (e.getEntity() instanceof GuildConfig)
            gcCache.put(((GuildConfig) e.getEntity()).getGuildId(), (GuildConfig) e.getEntity());
    }

    @Subscribe
    public void onLiczekMessage(GuildMessageReceivedEvent m) {
        if (m.getAuthor().isBot() || m.getMessage().isWebhookMessage()) return;
        GuildConfig gc = getGuildConfig(m.getGuild());
        if (!gc.getLiczekKanal().equals(m.getChannel().getId())) return;

        String msg = m.getMessage().getContentRaw();
        if (msg.isEmpty()) {
            m.getMessage().delete().queue();
            return;
        }

        int liczba = -2137;
        try {
            liczba = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            m.getMessage().delete().queue();
            return;
        }

        synchronized (m.getGuild()) {
            List<Message> messages = getHistoryList(m.getChannel());
            int kiedysMessage = Integer.parseInt(messages.get(1).getContentRaw());

            if (messages.size() < 2 || liczba != kiedysMessage+1 || messages.get(1).getAuthor().getId().equals(m.getAuthor().getId())) {
                m.getMessage().delete().queue();
                return;
            }

            updateTopic(m.getChannel());
        }
    }

    private List<Message> getHistoryList(TextChannel txt) {
        List<Message> msgs = new ArrayList<>();
        for (Message msg : txt.getIterableHistory().stream().limit(10).collect(Collectors.toList())) {
            if (!msg.isEdited()) {
                msgs.add(msg);
                if (msgs.size() == 2) break;
            } else msg.delete().queue();
        }
        return msgs;
    }

    private void updateTopic(TextChannel txt) {
        GuildConfig gc = getGuildConfig(txt.getGuild());
        String trans = tlumaczenia.get(gc.getLanguage(), "liczek.topic");
        Message msg = getHistoryList(txt).get(0);
        try {
            int liczba = Integer.parseInt(msg.getContentDisplay());
            String format = String.format(trans, msg.getAuthor().getAsMention(), liczba+1);
            txt.getManager().setTopic(format).queue();
        } catch (Exception ignored) {}
    }
}
