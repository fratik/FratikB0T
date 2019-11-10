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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.stax2.ri.typed.NumberUtil;
import org.jsoup.helper.StringUtil;
import pl.fratik.core.entity.GuildConfig;
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
        if (e.getMember().getUser().getId().equals("343467373417857025")) {
            GuildConfig gc = guildDao.get(e.getGuild());
            e.getChannel().sendMessage("DEBUG: " + "1").queue();
            if (e.getChannel().getType() != ChannelType.TEXT) { return; }
            e.getChannel().sendMessage("DEBUG: " + "2").queue();
            if (gc.getLiczekKanal().equals(e.getChannel())) {
                e.getChannel().sendMessage("DEBUG: " + "3").queue();
                if (e.getMember().getUser().isFake() || e.getMessage().getEmbeds().get(0) != null) {
                    e.getMessage().delete().queue();
                    return;
                }
                e.getChannel().sendMessage("DEBUG: " + "4").queue();

                String[] msg = String.valueOf(e.getMessage()).split(" ");
                if (msg[0].isEmpty() || !StringUtil.isNumeric(msg[0])) {
                    e.getMessage().delete().queue();
                    return;
                }
                e.getChannel().sendMessage("DEBUG: " + "5").queue();
                Integer wyslanaLiczba = Integer.parseInt(msg[0]);
                if (wyslanaLiczba != gc.getLiczekLiczba()+1 || e.getMember().equals(gc.getLiczekOstatniaOsoba())) {
                    e.getMessage().delete().queue();
                    return;
                }
                e.getChannel().sendMessage("DEBUG: " + "6").queue();

                gc.setLiczekLiczba(wyslanaLiczba);
                gc.setLiczekOstatniaOsoba(e.getMember().getUser().getId());
                guildDao.save(gc);
                refreshTopic(e.getChannel());
            }
        }
    }

    public void refreshTopic(TextChannel cha) {
        GuildConfig gc = guildDao.get(cha.getGuild());

        Integer liczba = gc.getLiczekLiczba()+1;
        User osoba = cha.getJDA().getUserById(gc.getLiczekLiczba());

        if (liczba == null || osoba == null) return;

        Language l = tlumaczenia.getLanguage(cha.getGuild());
        String msg = tlumaczenia.get(l, "liczek.topic", osoba.getAsMention(), liczba);

        try {
            cha.getManager().setTopic(msg);
        } catch (Exception xd) {
            /* brak permów, idziem sobie*/
        }
    }
}