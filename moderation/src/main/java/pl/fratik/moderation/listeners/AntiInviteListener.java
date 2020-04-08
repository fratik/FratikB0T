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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
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
import pl.fratik.moderation.entity.CaseBuilder;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.util.List;

public class AntiInviteListener {

    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ManagerKomend managerKomend;
    private final ShardManager shardManager;
    private final CasesDao casesDao;
    private final Cache<GuildConfig> gcCache;

    public AntiInviteListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, ShardManager shardManager, CasesDao casesDao, RedisCacheManager redisCacheManager) {
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
        if (!e.isFromType(ChannelType.TEXT) || e.getMember() == null || e.getAuthor().isBot() ||
                !e.getTextChannel().canTalk()) return;
        if (!isAntiinvite(e.getGuild()) || isIgnored(e.getTextChannel())) return;
        if (!e.getGuild().getSelfMember().canInteract(e.getMember())) return;
        if (UserUtil.getPermlevel(e.getMember(), guildDao, shardManager, PermLevel.OWNER).getNum() >= 1) return;
        String content = e.getMessage().getContentRaw().toLowerCase();
        if (content.contains("discord.gg/") || content.contains("discord.io/") || content.contains("discord.me/") ||
                content.contains("discordapp.com/invite/")) {
            try {
                e.getMessage().delete().queue();
                synchronized (e.getGuild()) {
                    Case c = new CaseBuilder(e.getGuild()).setUser(e.getAuthor().getId()).setKara(Kara.WARN)
                            .setTimestamp(Instant.now()).createCase();
                    c.setIssuerId(e.getJDA().getSelfUser());
                    c.setReason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "antiinvite.reason"));
                    e.getChannel().sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(e.getMember()),
                            "antiinvite.notice", e.getAuthor().getAsMention(),
                            managerKomend.getPrefixes(e.getGuild()).get(0))).queue();
                    String mlogchan = getModLogChan(e.getGuild());
                    if (mlogchan == null || mlogchan.equals("")) mlogchan = "0";
                    TextChannel mlogc = shardManager.getTextChannelById(mlogchan);
                    if (!(mlogc == null || !mlogc.getGuild().getSelfMember().hasPermission(mlogc,
                            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))) {
                        Message m = mlogc.sendMessage(ModLogBuilder.generate(c,
                                e.getGuild(), shardManager, tlumaczenia.getLanguage(e.getGuild()), managerKomend)).complete();
                        c.setMessageId(m.getId());
                    }
                    CaseRow cr = casesDao.get(e.getGuild());
                    cr.getCases().add(c);
                    casesDao.save(cr);
                    WarnUtil.takeAction(guildDao, casesDao, e.getMember(), e.getChannel(),
                            tlumaczenia.getLanguage(e.getGuild()), tlumaczenia, managerKomend);
                }
            } catch (Exception e1) {
                // no i chuj i jebło, ignoruj
            }
        }
    }

    private boolean isAntiinvite(Guild guild) {
        Boolean a = gcCache.get(guild.getId(), guildDao::get).getAntiInvite();
        return a != null;
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
