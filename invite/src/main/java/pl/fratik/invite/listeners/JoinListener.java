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

package pl.fratik.invite.listeners;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;
import pl.fratik.invite.cache.FakeInvite;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteConfig;
import pl.fratik.invite.entity.InviteDao;

import java.util.List;

public class JoinListener {

    private final InvitesCache invitesCache;
    private final GuildDao guildDao;
    private final InviteDao inviteDao;
    private final Tlumaczenia tlumaczenia;

    private final Cache<InviteConfig> invCache;
    private final Cache<GuildConfig> gcCache;

    public JoinListener(InviteDao inviteDao, InvitesCache invitesCache, GuildDao guildDao, RedisCacheManager rcm, Tlumaczenia tlumaczenia) {
        this.inviteDao = inviteDao;
        this.invitesCache = invitesCache;
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        gcCache = rcm.new CacheRetriever<GuildConfig>(){}.getCache();
        invCache = rcm.new CacheRetriever<InviteConfig>(){}.getCache();
    }

    @Subscribe
    public void onMemberJoin(GuildMemberJoinEvent e) {
        List<Invite> zaproszenia = e.getGuild().retrieveInvites().complete();
        for (Invite invite : zaproszenia) {
            FakeInvite inv = invitesCache.inviteCache.getIfPresent(e.getGuild().getId() + "." + invite.getCode());
            if (inv == null || (invite.getUses() - 1) != inv.getUses()) continue;
            invitesCache.load(invite);
            User user = invite.getInviter();
            if (user != null) {
                InviteConfig wchodzacy = getInviteConfig(e.getMember().getId(), e.getGuild().getId());
                InviteConfig zapraszajacy = getInviteConfig(user.getId(), e.getGuild().getId());
                wchodzacy.setDolaczylZJegoZaproszenia(user.getId());
                zapraszajacy.setTotalInvites(zapraszajacy.getTotalInvites() + 1);
                addRole(e.getGuild(), zapraszajacy.getTotalInvites() - zapraszajacy.getLeaveInvites(), user);
                inviteDao.save(wchodzacy, zapraszajacy);
            }
        }
    }

    private void addRole(Guild guild, int invites, User zapraszajacy) {
        GuildConfig gc = getGuildConfig(guild);
        if (gc.getRoleZaZaproszenia() != null && !gc.getRoleZaZaproszenia().isEmpty()) {
            for (int i = 1; i < invites; i++) {
                String roleId = gc.getRoleZaZaproszenia().get(i);
                if (roleId == null) continue;
                Role r = guild.getRoleById(roleId);
                if (r == null) continue;
                try {
                    Member mem = guild.retrieveMember(zapraszajacy).complete();
                    if (mem == null) continue;
                    guild.addRoleToMember(mem, r).complete();
                } catch (Exception ex) {
                    if (gc.getFullLogs() == null || gc.getFullLogs().isEmpty()) continue;
                    TextChannel kanal = guild.getTextChannelById(gc.getFullLogs());
                    if (kanal == null) continue;
                    String tarns = tlumaczenia.get(gc.getLanguage(), "invites.addroleerror", r.getName(),  UserUtil.formatDiscrim(zapraszajacy));
                    kanal.sendMessage(tarns).queue();
                }
            }
        }
    }

    @Subscribe
    public void onMemberLeave(GuildMemberRemoveEvent e) {
        InviteConfig ic = inviteDao.get(e.getUser().getId(), e.getGuild().getId());
        if (ic.getDolaczylZJegoZaproszenia() != null) {
            InviteConfig zarazMuOdjebieZapro = getInviteConfig(ic.getDolaczylZJegoZaproszenia(), e.getGuild().getId());
            zarazMuOdjebieZapro.setLeaveInvites(zarazMuOdjebieZapro.getLeaveInvites() + 1);
            inviteDao.save(zarazMuOdjebieZapro);
        }
        ic.setDolaczylZJegoZaproszenia(null);
        inviteDao.save(ic);

    }

    private GuildConfig getGuildConfig(Guild guild) {
        return gcCache.get(guild.getId(), guildDao::get);
    }

    private InviteConfig getInviteConfig(String user, String guild) {
        String encode = user + "." + guild;
        return invCache.get(encode, inviteDao::get);
    }

}
