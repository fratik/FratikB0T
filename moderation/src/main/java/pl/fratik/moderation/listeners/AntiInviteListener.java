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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Globals;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.util.List;

public class AntiInviteListener {

    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final CaseDao caseDao;
    private final Cache<GuildConfig> gcCache;

    public AntiInviteListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, ShardManager shardManager, CaseDao caseDao, RedisCacheManager redisCacheManager) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.managerKomend = managerKomend;
        this.shardManager = shardManager;
        this.caseDao = caseDao;
        gcCache = redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMessage(MessageReceivedEvent e) {
        if (!e.isFromType(ChannelType.TEXT) || e.getMember() == null || e.getAuthor().isBot() ||
                !e.getTextChannel().canTalk()) return;
        if (!isAntiinvite(e.getGuild()) || isIgnored(e.getTextChannel())) return;
        if (!e.getGuild().getSelfMember().canInteract(e.getMember())) return;
        if (UserUtil.getPermlevel(e.getMember(), guildDao, shardManager, PermLevel.OWNER).getNum() >= 1) return;

        if (containsInvite(e.getMessage().getContentRaw())) addKara(e.getMessage());
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEdit(GuildMessageUpdateEvent e) {
        if (e.getMember() == null || e.getAuthor().isBot() ||
                !e.getChannel().canTalk()) return;
        if (!isAntiinvite(e.getGuild()) || isIgnored(e.getChannel())) return;
        if (!e.getGuild().getSelfMember().canInteract(e.getMember())) return;
        if (UserUtil.getPermlevel(e.getMember(), guildDao, shardManager, PermLevel.OWNER).getNum() >= 1) return;

        if (containsInvite(e.getMessage().getContentRaw())) addKara(e.getMessage());
    }

    public static boolean containsInvite(String s) {
        s = s.toLowerCase();
        return s.contains("discord.gg/") || s.contains("discord.io/") || s.contains("discord.me/") ||
                s.contains("discord.com/invite/") || s.contains("discordapp.com/invite/") ||
                s.contains("invite.gg/") || s.contains("dus.im/") || s.contains("top.gg/servers/");
    }

    private synchronized void addKara(Message msg) {
        if (!containsInvite(msg.getContentRaw())) return;
        String trans = msg.isEdited() ? "antiinvite.notice.edited" : "antiinvite.notice";
        try {
            msg.delete().queue();
            synchronized (msg.getGuild()) {
                Member member = msg.getMember();
                if (member == null) {
                    // to się nie stanie
                    return;
                }
                Case c = new Case.Builder(member, Instant.now(), Kara.WARN).setIssuerId(Globals.clientId)
                        .setReasonKey("antiinvite.reason").build();
                caseDao.createNew(null, c, false, msg.getTextChannel(), tlumaczenia.getLanguage(member));
                msg.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(member),
                        trans, msg.getAuthor().getAsMention(), WarnUtil.countCases(caseDao.getCasesByMember(member), member.getId()),
                        managerKomend.getPrefixes(msg.getGuild()).get(0))).queue();
            }
        } catch (Exception ignored) {
            // no i chuj, wylądował, wszystko poszło w pizdu
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAntiinvite(Guild guild) {
        Boolean a = gcCache.get(guild.getId(), guildDao::get).getAntiInvite();
        return a != null && a;
    }

    private boolean isIgnored(TextChannel channel) {
        List<String> id = gcCache.get(channel.getGuild().getId(), guildDao::get).getKanalyGdzieAntiInviteNieDziala();
        if (id != null) return id.contains(channel.getId());
        return false;
    }

    private String getModLogChan(Guild guild) {
        return gcCache.get(guild.getId(), guildDao::get).getModLog();
    }
}
