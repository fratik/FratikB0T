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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.punkty.LicznikPunktow;

public class StatsCommand extends Command {
    private final LicznikPunktow licznik;

    public StatsCommand(LicznikPunktow licznik) {
        this.licznik = licznik;
        name = "stats";
        aliases = new String[]{"staty", "punkty"};
        category = CommandCategory.POINTS;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!licznik.punktyWlaczone(context.getGuild())) {
            context.send(context.getTranslated("punkty.off"));
            return false;
        }
        EmbedBuilder eb = context.getBaseEmbed(context.getSender().getName(), context.getSender()
                .getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setTitle(context.getTranslated("stats.embed.title"));
        eb.setDescription(context.getTranslated("stats.embed.description"));
        eb.addField(context.getTranslated("stats.embed.points"), String.valueOf(LicznikPunktow.getPunkty(context.getMember())), false);
        eb.addField(context.getTranslated("stats.embed.level"), String.valueOf(LicznikPunktow.getLvl(context.getMember())), false);
        context.send(eb.build());
        return true;
    }
}
