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

package pl.kamil0024.privkanal.listener;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import pl.kamil0024.privkanal.entity.Privkanal;
import pl.kamil0024.privkanal.entity.PrivkanalDao;

public class PrivkanalListener {

    private final PrivkanalDao privkanalDao;
    public PrivkanalListener(PrivkanalDao privkanalDao) {
        this.privkanalDao = privkanalDao;
    }

    @Subscribe
    public void onGuildVoiceJoinEvent(GuildVoiceJoinEvent e) {
        Privkanal pd = privkanalDao.get(e.getGuild());
        if (pd.getChannel() == null || pd.getChannel().isEmpty()) return;

        if (!e.getChannelJoined().getId().equals(pd.getChannel())) return;

        Category cat = e.getGuild().getCategoryById(pd.getCategory());
        if (cat == null) return;

        try {
            cat.createVoiceChannel(e.getMember().getUser().getName())
                .setName(e.getMember().getUser().getName()).queue(xd ->
                    e.getGuild().moveVoiceMember(e.getMember(), xd).queue());

        } catch (Exception jd) {
            /* lul */
        }
    }

    @Subscribe
    public void onGuildVoiceLeaveEvent(GuildVoiceLeaveEvent e) {
        xd(e.getChannelLeft());
    }

    @Subscribe
    public void onGuildVoiceMoveEvent(GuildVoiceMoveEvent e) {
        xd(e.getChannelLeft());
    }

    private void xd(VoiceChannel e) {
        Privkanal pd = privkanalDao.get(e.getGuild());
        if (pd.getCategory() == null || pd.getCategory().isEmpty()) return;
        Category cat = e.getGuild().getCategoryById(pd.getCategory());
        if (cat == null || e.getParent() == null) return;
        if (pd.getChannel() != null) { if (e.getId().equals(pd.getChannel())) return; }
        if (e.getParent().getId().equals(pd.getCategory())) {
            e.delete().queue();
        }
    }

}
