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

package pl.fratik.commands;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.event.PluginMessageEvent;
import pl.fratik.core.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MemberListener {
    private final GuildDao guildDao;
    private final EventBus eventBus;
    private final Cache<GuildConfig> gcCache;

    private static final Pattern INVITE_TAG_REGEX = Pattern.compile("<invite>(.*)</invite>", Pattern.MULTILINE | Pattern.DOTALL);

    MemberListener(GuildDao guildDao, EventBus eventBus, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        this.eventBus = eventBus;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
    }

    @Subscribe
    public void onMemberJoinEvent(GuildMemberJoinEvent e) {
        autorole(e);
        przywitanie(e);
    }

    @Subscribe
    public void onMemberLeaveEvent(GuildMemberRemoveEvent e) {
        GuildConfig gc = getGuildConfig(e.getGuild());
        for (Map.Entry<String, String> ch : gc.getPozegnania().entrySet()) {
            TextChannel cha = e.getGuild().getTextChannelById(ch.getKey());
            if (cha == null) continue;
            contentCheck(cha, ch.getValue()
                .replace("{{user}}", e.getUser().getAsTag())
                .replace("{{server}}", e.getGuild().getName())).queue();
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
        try {
            e.getGuild().modifyMemberRoles(e.getMember(), role, new ArrayList<>()).queue(null, i -> {});
        } catch (Exception ignored) {} // nie nasz problem lmao
    }

    private void przywitanie(GuildMemberJoinEvent e) {
        GuildConfig gc = getGuildConfig(e.getGuild());
        for (Map.Entry<String, String> ch : gc.getPowitania().entrySet()) {
            MessageChannel cha = e.getGuild().getTextChannelById(ch.getKey());
            if (cha == null) cha = e.getGuild().getThreadChannelById(ch.getKey());
            if (cha == null || !CommonUtil.canTalk(cha)) continue;
            boolean hasMentions = ch.getValue().contains("{{mention}}");
            String cnt = ch.getValue()
                    .replace("{{user}}", e.getMember().getUser().getAsTag())
                    .replace("{{mention}}", e.getMember().getAsMention())
                    .replace("{{server}}", e.getGuild().getName());
            Matcher matcher = INVITE_TAG_REGEX.matcher(cnt);
            if (matcher.find()) {
                String tagCnt = matcher.group(1);
                StringBuffer buf = new StringBuffer();
                PluginMessageEvent event = new PluginMessageEvent("commands", "invite", "Module-getInviteData:" +
                        e.getUser().getId() + "." + e.getGuild().getId());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException err) {
                    return;
                }
                eventBus.post(event);
                awaitPluginResponse(event);
                if (event.getResponse() != null) {
                    pl.fratik.invite.entity.InviteData inv = (pl.fratik.invite.entity.InviteData) event.getResponse();
                    User invitedBy;
                    try {
                        invitedBy = e.getJDA().retrieveUserById(inv.getDolaczylZJegoZaproszenia()).complete();
                    } catch (Exception err) {
                        invitedBy = null;
                    }
                    if (invitedBy != null) {
                        tagCnt = tagCnt.replaceAll("(\\{\\{invitedBy}})|(\\{\\{invitedBy-user}})", invitedBy.getAsTag())
                                .replace("{{invitedBy-mention}}", invitedBy.getAsMention()).replace("{{inviteCount}}",
                                        String.valueOf(inv.getTotalInvites() - inv.getLeaveInvites()));
                    } else {
                        tagCnt = "";
                    }
                    matcher.appendReplacement(buf, tagCnt);
                } else {
                    matcher.appendReplacement(buf, "");
                }
                matcher.appendTail(buf);
                cnt = buf.toString();
            }
            MessageCreateAction ma = contentCheck(cha, cnt);
            if (hasMentions) ma.mention(e.getMember()).queue();
            else ma.queue();
        }
    }

    private MessageCreateAction contentCheck(MessageChannel guildChannel, String content) {
        if (content.length() > Message.MAX_CONTENT_LENGTH) {
            content = content.substring(0, Message.MAX_CONTENT_LENGTH);
        }
        return guildChannel.sendMessage(content);
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

    private void awaitPluginResponse(PluginMessageEvent event) {
        int waited = 0;
        while (event.getResponse() == null) {
            try {
                Thread.sleep(100);
                waited += 100;
                if (waited >= 3000) break;
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
