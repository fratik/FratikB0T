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
import pl.fratik.core.command.PermLevel;
import pl.fratik.punkty.LicznikPunktow;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalstatyCommand extends Command {
    public GlobalstatyCommand() {
        name = "globalstaty";
        category = CommandCategory.POINTS;
        permLevel = PermLevel.EVERYONE;
        aliases = new String[] {"globalstats", "gs", "globalnestaty", "statsglobal", "statyglobalne", "gp", "globalpunkty", "globalstaty"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull @Nonnull CommandContext context) {
        EmbedBuilder eb = context.getBaseEmbed(context.getSender().getName(), context.getSender()
                .getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setTitle(context.getTranslated("globalstaty.embed.title"));
        eb.setDescription(context.getTranslated("globalstaty.embed.description"));
        Map<String, Integer> punkty = LicznikPunktow.getTotalPoints(context.getSender());
        AtomicInteger atomicInteger = new AtomicInteger();
        punkty.forEach((idSerwera, ilosc) -> atomicInteger.getAndAdd(ilosc));
        eb.addField(context.getTranslated("globalstaty.embed.points"), String.valueOf(atomicInteger.intValue()), false);
        eb.setTimestamp(Instant.now());
        context.send(eb.build());
        return true;
    }
}
