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

package pl.fratik.liczek;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;

public class LiczekListener {
    private final GuildDao guildDao;
    @Getter private final Tlumaczenia tlumaczenia;

    LiczekListener(GuildDao guildDao, Tlumaczenia tlumaczenia) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
    }

    @Subscribe
    public void onGuildMessageReceivedEvent(GuildMessageReceivedEvent e) {
        GuildConfig gc = guildDao.get(e.getGuild());
        if (e.getChannel().getType() != ChannelType.TEXT) return;
        String id = gc.getLiczekKanal();
        if (id != null || !id.equals("0")) {
            TextChannel txt = e.getGuild().getTextChannelById(id);
            if (txt != null) {
                if (e.getMember().getUser().isFake() || e.getMember().getUser().isBot()) {
                    e.getMessage().delete().queue();
                    return;
                }
                String[] msg = String.valueOf(e.getMessage()).split(" ");
                e.getChannel().sendMessage("pierwsze zdanie to" + msg[0]);
            }
        }}
    }
