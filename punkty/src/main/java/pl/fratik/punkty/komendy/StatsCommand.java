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

package pl.fratik.punkty.komendy;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;

import java.awt.*;
import java.math.RoundingMode;
import java.util.Set;

import static pl.fratik.core.Statyczne.BRAND_COLOR;

public class StatsCommand extends NewCommand {
    private final LicznikPunktow licznik;

    public StatsCommand(LicznikPunktow licznik) {
        this.licznik = licznik;
        name = "staty";
        usage = "[osoba:user]";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.reply(context.getTranslated("punkty.off"));
            return;
        }
        context.deferAsync(false);
        Member mem = context.getArguments().containsKey("osoba") ? context.getArguments().get("osoba").getAsMember() : context.getMember();
        context.sendMessage(Set.of(renderStatsEmbed(context.getTlumaczenia(), context.getLanguage(), mem)));
    }

    @NotNull
    public static MessageEmbed renderStatsEmbed(@NotNull Tlumaczenia t, @NotNull Language l, Member mem) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.decode(BRAND_COLOR))
                .setAuthor(mem.getUser().getName(), null, UserUtil.getAvatarUrl(mem.getUser()))
                .setFooter("© " + mem.getJDA().getSelfUser().getName(),
                        UserUtil.getAvatarUrl(mem.getJDA().getSelfUser()));
        eb.setTitle(t.get(l, "stats.embed.title"));
        eb.setDescription(t.get(l, "stats.embed.description"));
        int punkty = LicznikPunktow.getPunkty(mem);
        eb.addField(t.get(l, "stats.embed.points"), String.valueOf(punkty), false);
        int level = LicznikPunktow.getLvl(mem);
        eb.addField(t.get(l, "stats.embed.level"), String.valueOf(level), false);
        String progress = "%s\n\n%s %s/%s";
        double curLvlPunkty = (Math.pow(level, 2) * 100) / 4;
        double nextLvlPunkty = (Math.pow(level + 1, 2) * 100) / 4;
        double current = punkty - curLvlPunkty; //aktualna liczba punktów - ilość punktów na aktualny poziom
        double target = nextLvlPunkty - curLvlPunkty; //ilość punktów na (akt. poziom + 1) - ilość pkt. na akt. poziom
        progress = String.format(progress, t.get(l, "stats.progress.text", (int) (nextLvlPunkty - punkty)),
                CommonUtil.generateProgressBar((int) (CommonUtil.round(((current / target) * 100), 0,
                        RoundingMode.HALF_UP)), true), (int) current, (int) target);
        eb.addField(t.get(l, "stats.embed.progress"), progress, false);
        return eb.build();
    }
}
