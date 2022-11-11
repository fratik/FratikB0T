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

package pl.fratik.invite.listeners;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.Nullable;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.UserUtil;
import pl.fratik.invite.cache.FakeInvite;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteDao;
import pl.fratik.invite.entity.InviteData;

import java.util.List;

public class JoinListener {

    private final InvitesCache invitesCache;
    private final GuildDao guildDao;
    private final InviteDao inviteDao;
    private final Tlumaczenia tlumaczenia;

    private final Cache<InviteData> invCache;
    private final Cache<GuildConfig> gcCache;

    public JoinListener(InviteDao inviteDao, InvitesCache invitesCache, GuildDao guildDao, RedisCacheManager rcm, Tlumaczenia tlumaczenia) {
        this.inviteDao = inviteDao;
        this.invitesCache = invitesCache;
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        gcCache = rcm.new CacheRetriever<GuildConfig>(){}.getCache();
        invCache = rcm.new CacheRetriever<InviteData>(){}.getCache();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMemberJoin(GuildMemberJoinEvent e) {
        if (!invitesCache.isLoaded()) return;
        if (!gcCache.get(e.getGuild().getId(), guildDao::get).isTrackInvites()) return;
        List<Invite> zaproszenia;
        try {
            zaproszenia = e.getGuild().retrieveInvites().complete();
        } catch (InsufficientPermissionException err) {
            return;
        }
        for (Invite invite : zaproszenia) {
            FakeInvite inv = invitesCache.getInviteCache().getIfPresent(e.getGuild().getId() + "." + invite.getCode());
            if (inv == null || (invite.getUses() - 1) != inv.getUses()) continue;
            invitesCache.load(invite);
            User user = invite.getInviter();
            if (user != null) {
                InviteData wchodzacy = getInviteData(e.getMember().getId(), e.getGuild().getId());
                InviteData zapraszajacy = getInviteData(user.getId(), e.getGuild().getId());
                wchodzacy.setDolaczylZJegoZaproszenia(user.getId());
                zapraszajacy.setTotalInvites(zapraszajacy.getTotalInvites() + 1);
                addRole(e.getGuild(), zapraszajacy.getTotalInvites() - zapraszajacy.getLeaveInvites(), user);
                inviteDao.save(wchodzacy, zapraszajacy);
                TextChannel kanal = getFullLogs(e.getGuild());
                try {
                    if (kanal != null) {
                        kanal.sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "invites.joined",
                                e.getUser().getAsTag(), e.getUser().getId(), user.getAsTag(), user.getId(),
                                invite.getUses())).queue();
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void addRole(Guild guild, int invites, User zapraszajacy) {
        GuildConfig gc = getGuildConfig(guild);
        if (gc.getRoleZaZaproszenia() != null && !gc.getRoleZaZaproszenia().isEmpty()) {
            for (int i = 1; i <= invites; i++) {
                String roleId = gc.getRoleZaZaproszenia().get(i);
                if (roleId == null) continue;
                Role r = guild.getRoleById(roleId);
                if (r == null) continue;
                Runnable error = () -> {
                    TextChannel kanal = getFullLogs(guild, gc);
                    if (kanal == null) return;
                    String tarns = tlumaczenia.get(tlumaczenia.getLanguage(guild), "invites.addroleerror",
                            r.getName(), UserUtil.formatDiscrim(zapraszajacy));
                    kanal.sendMessage(tarns).queue();
                };
                try {
                    Member mem = guild.retrieveMember(zapraszajacy).complete();
                    if (mem == null) continue;
                    guild.addRoleToMember(mem, r).queue(null, t -> error.run());
                } catch (Exception ex) {
                    error.run();
                }
            }
        }
    }

    @Nullable
    private TextChannel getFullLogs(Guild guild) {
        return getFullLogs(guild, gcCache.get(guild.getId(), guildDao::get));
    }

    @Nullable
    private TextChannel getFullLogs(Guild guild, GuildConfig gc) {
        try {
            if (gc.getFullLogs() == null || gc.getFullLogs().isEmpty()) return null;
            return guild.getTextChannelById(gc.getFullLogs());
        } catch (Exception e) { //nieprawidłowy kanał
            return null;
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMemberLeave(GuildMemberRemoveEvent e) {
        if (!invitesCache.isLoaded()) return;
        if (!gcCache.get(e.getGuild().getId(), guildDao::get).isTrackInvites()) return;
        InviteData ic = inviteDao.get(e.getUser().getId(), e.getGuild().getId());
        if (ic.getDolaczylZJegoZaproszenia() != null) {
            InviteData zarazMuOdjebieZapro = getInviteData(ic.getDolaczylZJegoZaproszenia(), e.getGuild().getId());
            zarazMuOdjebieZapro.setLeaveInvites(zarazMuOdjebieZapro.getLeaveInvites() + 1);
            inviteDao.save(zarazMuOdjebieZapro);
        }
        ic.setDolaczylZJegoZaproszenia(null);
        inviteDao.save(ic);

    }

    private GuildConfig getGuildConfig(Guild guild) {
        return gcCache.get(guild.getId(), guildDao::get);
    }

    private InviteData getInviteData(String user, String guild) {
        String encode = user + "." + guild;
        return invCache.get(encode, inviteDao::get);
    }

}
