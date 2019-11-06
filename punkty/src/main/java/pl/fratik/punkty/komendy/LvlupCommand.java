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
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;

import javax.annotation.Nonnull;
import java.text.NumberFormat;

public class LvlupCommand extends Command {
    private final LicznikPunktow licznik;
    private final ManagerArgumentow managerArgumentow;

    public LvlupCommand(LicznikPunktow licznik, ManagerArgumentow managerArgumentow) {
        this.licznik = licznik;
        this.managerArgumentow = managerArgumentow;
        name = "lvlup";
        aliases = new String[] {"pktNaPoziom", "getPoziom", "zaIleLevelUp", "kiedyNastepnyPoziom", "ilePunktowNaPoziom"};
        category = CommandCategory.POINTS;
    }

    @Override
    public boolean execute(@NotNull @Nonnull CommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.send(context.getTranslated("punkty.off"));
            return false;
        }
        Member mem = context.getMember();
        Integer lvl = null;

        if (context.getRawArgs().length >= 1) {
            Member checkMemer;
            checkMemer = (Member) managerArgumentow.getArguments().get("member").execute(context.getRawArgs()[0],
                    context.getTlumaczenia(), context.getLanguage(), context.getGuild());
            if (checkMemer == null && !context.getEvent().getMessage().getMentionedMembers().isEmpty() &&
                    context.getEvent().getMessage().getMentionedMembers().get(0) != null)
                checkMemer = context.getEvent().getMessage().getMentionedMembers().get(0);
            if (checkMemer != null) {
                mem = checkMemer;
                if (context.getRawArgs().length >= 2) {
                    Integer checkLvlv2 = (Integer) managerArgumentow.getArguments().get("integer")
                            .execute(context.getRawArgs()[1], context.getTlumaczenia(), context.getLanguage());
                    if (checkLvlv2 > 0) {
                        lvl = checkLvlv2;
                    }
                }
            } else {
                Integer checkLvl = (Integer) managerArgumentow.getArguments().get("integer")
                        .execute(context.getRawArgs()[0], context.getTlumaczenia(), context.getLanguage());
                if (checkLvl != null && checkLvl > 0) {
                    lvl = checkLvl;
                }
            }
        }

        if (lvl != null && lvl > 1000) {
            context.send(context.getTranslated("lvlup.integer.toobig"));
            return false;
        }

        if (lvl == null) lvl = LicznikPunktow.getLvl(mem) + 1;
        double kalkulejtedRaw = (Math.pow(lvl, 2) * 100) / 4;
        double zostaloRaw = kalkulejtedRaw - LicznikPunktow.getPunkty(mem);
        String kalkulejted = NumberFormat.getNumberInstance(context.getLanguage().getLocale()).format(kalkulejtedRaw);
        String zostalo = NumberFormat.getNumberInstance(context.getLanguage().getLocale()).format(zostaloRaw);

        if (!context.getMember().equals(mem)) {
            if (zostaloRaw <= 0) context.send(context.getTranslated("lvlup.response.lower.his", lvl,
                    UserUtil.formatDiscrim(mem), kalkulejted));
            else context.send(context.getTranslated("lvlup.response.higher.his", lvl, UserUtil.formatDiscrim(mem),
                    kalkulejted, zostalo));
            return true;
        }
        if (zostaloRaw <= 0) context.send(context.getTranslated("lvlup.response.lower.self", lvl, kalkulejted));
        else context.send(context.getTranslated("lvlup.response.higher.self", lvl, kalkulejted, zostalo));
        return true;
    }
}
