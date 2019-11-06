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

package pl.fratik.punkty.komendy;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.*;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;
import pl.fratik.punkty.entity.PunktyDao;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RankingCommand extends Command {
    private final PunktyDao punktyDao;
    private final LicznikPunktow licznik;

    public RankingCommand(PunktyDao punktyDao, LicznikPunktow licznik) {
        this.punktyDao = punktyDao;
        this.licznik = licznik;
        name = "ranking";
        aliases = new String[] {"rank"};
        permLevel = PermLevel.EVERYONE;
        category = CommandCategory.POINTS;
    }

    @SuppressWarnings("squid:S1192")
    @SubCommand(name="punkty",aliases={"points", "pkt"})
    public boolean punkty(@NotNull CommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.send(context.getTranslated("punkty.off"));
            return false;
        }
        Map<String, Integer> dane = punktyDao.getTopkaPunktow(context.getGuild());
        ArrayList<String> tekst = new ArrayList<>();
        AtomicInteger miejsce = new AtomicInteger(0);
        dane.forEach((id, punkty) -> {
            if (dane.size() == 10) {
                int liczba = miejsce.incrementAndGet();
                String liczbaStringed;
                if (liczba <= 9) liczbaStringed = "[ " + liczba;
                else liczbaStringed = "[" + liczba;
                Member uzytkownik = context.getGuild().getMemberById(id);
                if (uzytkownik != null)
                    tekst.add(String.format("%s] %s: %s", liczbaStringed, UserUtil.formatDiscrim(uzytkownik), punkty));
                else
                    tekst.add(String.format("%s] %s: %s", liczbaStringed, context.getTranslated("ranking.user.left", id),
                            punkty));
                if (liczba != dane.size()) tekst.add("");
            } else {
                int liczba = miejsce.incrementAndGet();
                Member uzytkownik = context.getGuild().getMemberById(id);
                if (uzytkownik != null)
                    tekst.add(String.format("[%s] %s: %s", liczba, UserUtil.formatDiscrim(uzytkownik), punkty));
                else
                    tekst.add(String.format("[%s] %s: %s", liczba, context.getTranslated("ranking.user.left", id),
                            punkty));
                if (liczba != dane.size()) tekst.add("");
            }
        });
        context.send(context.getTranslated("ranking.points.header") + "```" + String.join("\n", tekst) + "```");
        return true;
    }

    @SubCommand(name="poziom",aliases={"level", "lvl"})
    public boolean poziom(@NotNull CommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.send(context.getTranslated("punkty.off"));
            return false;
        }
        Map<String, Integer> dane = punktyDao.getTopkaPoziomow(context.getGuild());
        ArrayList<String> tekst = new ArrayList<>();
        AtomicInteger miejsce = new AtomicInteger(0);
        dane.forEach((id, poziom) -> {
            if (dane.size() >= 10) {
                int liczba = miejsce.incrementAndGet();
                if (liczba > 10) return;
                String liczbaStringed;
                if (liczba <= 9) liczbaStringed = "[ " + liczba;
                else liczbaStringed = "[" + liczba;
                Member uzytkownik = context.getGuild().getMemberById(id);
                if (uzytkownik != null)
                    tekst.add(String.format("%s] %s: %s", liczbaStringed, UserUtil.formatDiscrim(uzytkownik), poziom));
                else
                    tekst.add(String.format("%s] %s: %s", liczbaStringed, context.getTranslated("ranking.user.left", id),
                            poziom));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            } else {
                int liczba = miejsce.incrementAndGet();
                if (liczba > 10) return;
                Member uzytkownik = context.getGuild().getMemberById(id);
                if (uzytkownik != null)
                    tekst.add(String.format("[%s] %s: %s", liczba, UserUtil.formatDiscrim(uzytkownik), poziom));
                else
                    tekst.add(String.format("[%s] %s: %s", liczba, context.getTranslated("ranking.user.left", id),
                            poziom));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            }
        });
        context.send(context.getTranslated("ranking.levels.header") + "```" + String.join("\n", tekst) + "```");
        return true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }
}
