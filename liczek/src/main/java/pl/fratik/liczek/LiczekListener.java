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
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

public class LiczekListener {
    private final GuildDao guildDao;
    @Getter private final Tlumaczenia tlumaczenia;

    LiczekListener(GuildDao guildDao, Tlumaczenia tlumaczenia) {
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
    }

    @Subscribe
    public void onGuildMessageReceivedEvent(GuildMessageReceivedEvent e) {
        e.getChannel();
        if (e.getChannel().getType() != ChannelType.TEXT) return;
        if (getChannel(e.getGuild()).equals(e.getChannel())) {
            Member botMember = e.getGuild().getMemberById(e.getJDA().getSelfUser().getId());
            assert botMember != null;
            if (hasPermission(botMember, getChannel(e.getGuild()))) {
                if (e.getMember().getUser().isBot()) {
                    e.getMessage().delete().queue();
                    return;
                }
                Integer liczba = getLiczba(e.getGuild());
                String[] msg = String.valueOf(e.getMessage()).split(" ");

                int wyslanaLiczba = Integer.parseInt(msg[0]);

                if (wyslanaLiczba != liczba + 1 || getLastUser(e.getGuild()).equals(e.getMember())) {
                    e.getMessage().delete().queue();
                    return;
                }
                setNumer(e.getGuild(), liczba);
                refreshDescription(e.getGuild(), e.getMember().getUser());
            }
        }
    }

    public TextChannel getChannel(Guild g) {
        String eldo = guildDao.get(g).getLiczekKanal();
        TextChannel kek = g.getTextChannelById(eldo);
        return kek;
    }

    public Integer getLiczba(Guild g) {
        return guildDao.get(g).getLiczekLiczba();
    }

    public Boolean hasPermission(Member mem, TextChannel c) {
        if (mem.hasPermission(c, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY, Permission.MANAGE_CHANNEL)) {
            return true;
        }
        return false;
    }

    public void refreshDescription(Guild g, User user) {
        TextChannel cha = getChannel(g);
        Language xd = tlumaczenia.getLanguage(g);
        String msg = tlumaczenia.get(xd, "liczek.topic", user.getAsTag(), getLiczba(g)+1, getLastUser(g).getAsTag());
        cha.getManager().setTopic(msg);
    }

    public void setNumer(Guild g, int num) {
        guildDao.get(g).setLiczekLiczba(num);
    }

    public void setChannel(Guild g, TextChannel ch) {
        assert ch != null;
        guildDao.get(g).setLiczekKanal(ch.getId());
    }

    public User getLastUser(Guild g) {
        String PiszeToOgodzienie1 = guildDao.get(g).getLiczekOstatniaOsoba();
        return g.getJDA().getUserById(PiszeToOgodzienie1);
    }

    public void setLastMember(Guild g, User user) {
        guildDao.get(g).setLiczekOstatniaOsoba(user.getId());
    }

}
