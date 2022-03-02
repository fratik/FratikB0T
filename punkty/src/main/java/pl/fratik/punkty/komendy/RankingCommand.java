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

import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.*;
import pl.fratik.core.entity.MemberConfig;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;
import pl.fratik.punkty.entity.PunktyDao;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RankingCommand extends Command {
    private final PunktyDao punktyDao;
    private final LicznikPunktow licznik;
    private final MemberDao memberDao;

    public RankingCommand(PunktyDao punktyDao, LicznikPunktow licznik, MemberDao memberDao) {
        this.punktyDao = punktyDao;
        this.licznik = licznik;
        this.memberDao = memberDao;
        name = "ranking";
        aliases = new String[] {"rank"};
        uzycieDelim = " ";
        uzycie = new Uzycie("strona", "long", false);
        permLevel = PermLevel.EVERYONE;
        category = CommandCategory.POINTS;
        cooldown = 7;
        allowPermLevelChange = false;
    }

    @SuppressWarnings("squid:S1192")
    @SubCommand(name="punkty",aliases={"points", "pkt"})
    public boolean punkty(@NotNull CommandContext context) {
        long strona = 0;
        if (context.getArgs().length != 0) {
            if (context.getArgs()[0] == null || (Long) context.getArgs()[0] <= 0) {
                context.reply(context.getTranslated("ranking.page.invalid"));
                return false;
            }
            strona = (Long) context.getArgs()[0] - 1;
        }
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.reply(context.getTranslated("punkty.off"));
            return false;
        }
        Map<String, Integer> dane = punktyDao.getTopkaPunktow(context.getGuild(), strona);
        if (dane.size() == 0) {
            context.reply(context.getTranslated("ranking.page.empty"));
            return false;
        }
        ArrayList<String> tekst = new ArrayList<>();
        AtomicInteger miejsce = new AtomicInteger((int) strona * 10);
        tekst.add(context.getTranslated("ranking.page.header", strona + 1));
        tekst.add("");
        dane.forEach((id, punkty) -> {
            if (dane.size() >= 10) {
                int liczba = miejsce.incrementAndGet();
                String liczbaStringed;
                if (liczba <= 9) liczbaStringed = "[ " + liczba;
                else liczbaStringed = "[" + liczba;
                User uzytkownik = context.getShardManager().retrieveUserById(id).complete();
                tekst.add(String.format("%s] %s: %s", liczbaStringed, UserUtil.formatDiscrim(uzytkownik), punkty));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            } else {
                int liczba = miejsce.incrementAndGet();
                User uzytkownik = context.getShardManager().retrieveUserById(id).complete();
                tekst.add(String.format("[%s] %s: %s", liczba, UserUtil.formatDiscrim(uzytkownik), punkty));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            }
        });
        context.reply(context.getTranslated("ranking.points.header") + "\n```\n" + String.join("\n", tekst) + "```");
        return true;
    }

    @SubCommand(name="poziom",aliases={"level", "lvl"})
    public boolean poziom(@NotNull CommandContext context) {
        long strona = 0;
        if (context.getArgs().length != 0) {
            if (context.getArgs()[0] == null || (Long) context.getArgs()[0] <= 0) {
                context.reply(context.getTranslated("ranking.page.invalid"));
                return false;
            }
            strona = (Long) context.getArgs()[0] - 1;
        }
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.reply(context.getTranslated("punkty.off"));
            return false;
        }
        Map<String, Integer> dane = punktyDao.getTopkaPoziomow(context.getGuild(), strona );
        if (dane.size() == 0) {
            context.reply(context.getTranslated("ranking.page.empty"));
            return false;
        }
        ArrayList<String> tekst = new ArrayList<>();
        AtomicInteger miejsce = new AtomicInteger((int) strona * 10);
        tekst.add(context.getTranslated("ranking.page.header", strona + 1));
        tekst.add("");
        dane.forEach((id, poziom) -> {
            if (dane.size() >= 10) {
                int liczba = miejsce.incrementAndGet();
                String liczbaStringed;
                if (liczba <= 9) liczbaStringed = "[ " + liczba;
                else liczbaStringed = "[" + liczba;
                User uzytkownik = context.getShardManager().retrieveUserById(id).complete();
                tekst.add(String.format("%s] %s: %s", liczbaStringed, UserUtil.formatDiscrim(uzytkownik), poziom));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            } else {
                int liczba = miejsce.incrementAndGet();
                User uzytkownik = context.getShardManager().retrieveUserById(id).complete();
                tekst.add(String.format("[%s] %s: %s", liczba, UserUtil.formatDiscrim(uzytkownik), poziom));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            }
        });
        context.reply(context.getTranslated("ranking.levels.header") + "\n```\n" + String.join("\n", tekst) + "```");
        return true;
    }

    @SubCommand(name="fratikcoin",aliases={"fratikcoiny", "fc", "money"})
    public boolean fratikcoin(@NotNull CommandContext context) {
        long strona = 0;
        if (context.getArgs().length != 0) {
            if (context.getArgs()[0] == null || (Long) context.getArgs()[0] <= 0) {
                context.reply(context.getTranslated("ranking.page.invalid"));
                return false;
            }
            strona = (Long) context.getArgs()[0] - 1;
        }
        Map<String, Long> dane = memberDao.getTopkaFratikcoinow(context.getGuild(), strona);
        if (dane.size() == 0) {
            context.reply(context.getTranslated("ranking.page.empty"));
            return false;
        }
        ArrayList<String> tekst = new ArrayList<>();
        AtomicInteger miejsce = new AtomicInteger((int) strona * 10);
        tekst.add(context.getTranslated("ranking.page.header", strona + 1));
        tekst.add("");
        dane.forEach((id, fc) -> {
            if (dane.size() >= 10) {
                int liczba = miejsce.incrementAndGet();
                String liczbaStringed;
                if (liczba <= 9) liczbaStringed = "[ " + liczba;
                else liczbaStringed = "[" + liczba;
                User uzytkownik = context.getShardManager().retrieveUserById(id).complete();
                tekst.add(String.format("%s] %s: %s", liczbaStringed, UserUtil.formatDiscrim(uzytkownik), fc));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            } else {
                int liczba = miejsce.incrementAndGet();
                User uzytkownik = context.getShardManager().retrieveUserById(id).complete();
                tekst.add(String.format("[%s] %s: %s", liczba, UserUtil.formatDiscrim(uzytkownik), fc));
                if (liczba != Math.min(10, dane.size())) tekst.add("");
            }
        });
        context.reply(context.getTranslated("ranking.fratikcoin.header") + "\n```\n" + String.join("\n", tekst) + "```");
        return true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        CommonErrors.usage(context);
        return false;
    }
}
