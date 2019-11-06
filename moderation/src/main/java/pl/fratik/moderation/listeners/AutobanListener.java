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
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AutobanListener {

    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;

    public AutobanListener(GuildDao guildDao, Tlumaczenia tlumaczenia) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGuildMemberJoinEvent(GuildMemberJoinEvent e) {
        GuildConfig gc = guildDao.get(e.getGuild());
        if (gc.getAutoban()) {
            Case aCase = new CaseBuilder().setUser(e.getUser()).setGuild(e.getGuild())
                    .setCaseId(Case.getNextCaseId(e.getGuild())).setTimestamp(Instant.now())
                    .setMessageId(null).setKara(Kara.BAN).createCase();
            aCase.setIssuerId(e.getJDA().getSelfUser());
            aCase.setReason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "autoban.case.reason"));
            List<Case> caseList = ModLogListener.getKnownCases().getOrDefault(e.getGuild(), new ArrayList<>());
            caseList.add(aCase);
            e.getGuild().ban(e.getMember(), 0,
                    tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "autoban.audit.reason"))
                    .reason(tlumaczenia.get(tlumaczenia.getLanguage(e.getGuild()), "autoban.audit.reason")).complete();
            ModLogListener.getKnownCases().put(e.getGuild(), caseList);
        }
    }
}
