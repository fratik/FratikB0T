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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.punkty.LicznikPunktow;

import java.math.RoundingMode;

public class StatsCommand extends Command {
    private final LicznikPunktow licznik;

    public StatsCommand(LicznikPunktow licznik) {
        this.licznik = licznik;
        name = "stats";
        aliases = new String[]{"staty", "punkty"};
        category = CommandCategory.POINTS;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        uzycie = new Uzycie("uzytkownik", "member", false);
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.reply(context.getTranslated("punkty.off"));
            return false;
        }
        Member mem = context.getMember();
        try {
            mem = (Member) context.getArgs()[0];
        } catch (Exception ignored) {}
        if (mem == null) mem = context.getMember();
        EmbedBuilder eb = context.getBaseEmbed(mem.getUser().getName(), mem.getUser()
                .getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setTitle(context.getTranslated("stats.embed.title"));
        eb.setDescription(context.getTranslated("stats.embed.description"));
        int punkty = LicznikPunktow.getPunkty(mem);
        eb.addField(context.getTranslated("stats.embed.points"), String.valueOf(punkty), false);
        int level = LicznikPunktow.getLvl(mem);
        eb.addField(context.getTranslated("stats.embed.level"), String.valueOf(level), false);
        String progress = "%s\n\n%s %s/%s";
        double curLvlPunkty = (Math.pow(level, 2) * 100) / 4;
        double nextLvlPunkty = (Math.pow(level + 1, 2) * 100) / 4;
        double current = punkty - curLvlPunkty; //aktualna liczba punktów - ilość punktów na aktualny poziom
        double target = nextLvlPunkty - curLvlPunkty; //ilość punktów na (akt. poziom + 1) - ilość pkt. na akt. poziom
        progress = String.format(progress, context.getTranslated("stats.progress.text", (int) (nextLvlPunkty - punkty)),
                CommonUtil.generateProgressBar((int) (CommonUtil.round(((current / target) * 100), 0,
                        RoundingMode.HALF_UP)), true), (int) current, (int) target);
        eb.addField(context.getTranslated("stats.embed.progress"), progress, false);
        context.reply(eb.build());
        return true;
    }
}
