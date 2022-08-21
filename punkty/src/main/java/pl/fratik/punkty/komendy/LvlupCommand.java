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

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;

import javax.annotation.Nonnull;
import java.text.NumberFormat;

public class LvlupCommand extends NewCommand {
    private final LicznikPunktow licznik;

    public LvlupCommand(LicznikPunktow licznik) {
        this.licznik = licznik;
        name = "lvlup";
        usage = "[poziom:int] [osoba:user]";
    }

    @Override
    public void execute(@NotNull @Nonnull NewCommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.reply(context.getTranslated("punkty.off"));
            return;
        }

        Member mem = context.getMember();
        Integer lvl = null;

        if (context.getArguments().containsKey("poziom") && context.getArguments().containsKey("osoba")) {
            context.replyEphemeral(context.getTranslated("lvlup.invalid.arguments"));
            return;
        }

        if (context.getArguments().containsKey("poziom")) lvl = context.getArguments().get("poziom").getAsInt();
        if (context.getArguments().containsKey("osoba")) mem = context.getArguments().get("osoba").getAsMember();
        if (mem == null) {
            context.replyEphemeral(context.getTranslated("generic.no.member"));
            return;
        }

        context.deferAsync(false);

        if (lvl == null) lvl = LicznikPunktow.getLvl(mem) + 1;
        double kalkulejtedRaw = (Math.pow(lvl, 2) * 100) / 4;
        double zostaloRaw = kalkulejtedRaw - LicznikPunktow.getPunkty(mem);
        String kalkulejted = NumberFormat.getNumberInstance(context.getLanguage().getLocale()).format(kalkulejtedRaw);
        String zostalo = NumberFormat.getNumberInstance(context.getLanguage().getLocale()).format(zostaloRaw);

        if (!context.getMember().equals(mem)) {
            if (zostaloRaw <= 0) context.sendMessage(context.getTranslated("lvlup.response.lower.his", lvl,
                    UserUtil.formatDiscrim(mem), kalkulejted));
            else context.sendMessage(context.getTranslated("lvlup.response.higher.his", lvl, UserUtil.formatDiscrim(mem),
                    kalkulejted, zostalo));
            return;
        }
        if (zostaloRaw <= 0) context.sendMessage(context.getTranslated("lvlup.response.lower.self", lvl, kalkulejted));
        else context.sendMessage(context.getTranslated("lvlup.response.higher.self", lvl, kalkulejted, zostalo));
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("poziom")) {
            option.setMinValue(1);
            option.setMaxValue(1000);
        }
    }
}
