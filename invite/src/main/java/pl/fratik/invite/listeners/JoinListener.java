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
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import pl.fratik.invite.cache.FakeInvite;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteConfig;
import pl.fratik.invite.entity.InviteDao;

import java.util.List;

public class JoinListener {

    private final InviteDao inviteDao;
    private final InvitesCache invitesCache;

    public JoinListener(InviteDao inviteDao, InvitesCache invitesCache) {
        this.inviteDao = inviteDao;
        this.invitesCache = invitesCache;
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
                InviteConfig wchodzacy = inviteDao.get(e.getMember());
                InviteConfig zapraszajacy = inviteDao.get(user.getId(), e.getGuild().getId());
                wchodzacy.setDolaczylZJegoZaproszenia(user.getId());
                zapraszajacy.setTotalInvites(zapraszajacy.getTotalInvites() + 1);
                inviteDao.save(wchodzacy, zapraszajacy);
            }

        }

    }

    @Subscribe
    public void onMemberJoin(GuildMemberRemoveEvent e) {
        InviteConfig ic = inviteDao.get(e.getUser().getId(), e.getGuild().getId());
        if (ic.getDolaczylZJegoZaproszenia() != null) {
            InviteConfig zarazMuOdjebieZapro = inviteDao.get(ic.getDolaczylZJegoZaproszenia(), e.getGuild().getId());
            zarazMuOdjebieZapro.setLeaveInvites(zarazMuOdjebieZapro.getLeaveInvites() + 1);
            inviteDao.save(zarazMuOdjebieZapro);
        }
        ic.setDolaczylZJegoZaproszenia(null);
        inviteDao.save(ic);

    }

}
