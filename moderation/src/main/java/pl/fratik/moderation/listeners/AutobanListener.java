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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import pl.fratik.core.Globals;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.Case;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class AutobanListener {

    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private final ModLogListener modLogListener;

    public AutobanListener(GuildDao guildDao, Tlumaczenia tlumaczenia, ModLogListener modLogListener) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        this.modLogListener = modLogListener;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGuildMemberJoinEvent(GuildMemberJoinEvent e) {
        if (!e.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) return;
        GuildConfig gc = guildDao.get(e.getGuild());
        if (gc.getAutoban() != null && gc.getAutoban()) {
            Case aCase = new Case.Builder(e.getMember(), Instant.now(), Kara.BAN).setIssuerId(Globals.clientId)
                    .setReasonKey("autoban.case.reason").build();
            modLogListener.getKnownCases().put(ModLogListener.generateKey(e.getMember()), aCase);
            e.getGuild().ban(e.getMember(), 0, TimeUnit.MILLISECONDS)
                .reason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "autoban.audit.reason")).complete();
        }
    }
}
