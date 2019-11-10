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
        GuildConfig gc = guildDao.get(e.getGuild());
        if (e.getChannel().getType() != ChannelType.TEXT) { return; }
        if (e.getChannel().getId().equals(gc.getLiczekKanal())) {
            if (e.getMember().getUser().isFake() || e.getMember().getUser().isBot()) {
                e.getMessage().delete().queue();
                return;
            }

            String kek = String.valueOf(e.getMessage());

            if (!isNumeric(kek)) {
                e.getMessage().delete().queue();
                return;
            }

            int wyslanaLiczba = Integer.parseInt(kek);
            if (wyslanaLiczba != gc.getLiczekLiczba()+1 || e.getMember().getUser().getId().equals(gc.getLiczekOstatniaOsoba())) {
                e.getMessage().delete().queue();
                return;
            }

            gc.setLiczekLiczba(wyslanaLiczba);
            gc.setLiczekOstatniaOsoba(e.getMember().getUser().getId());
            guildDao.save(gc);
            refreshTopic(e.getChannel());
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

    public static boolean isNumeric(String stringi) {
        try {
            double d = Double.parseDouble(stringi);
        } catch (NumberFormatException | NullPointerException xd) {
            return false;
        }
        return true;
    }
}