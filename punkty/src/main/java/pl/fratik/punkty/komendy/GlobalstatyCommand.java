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
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static pl.fratik.core.Statyczne.BRAND_COLOR;

public class GlobalstatyCommand extends NewCommand {
    public GlobalstatyCommand() {
        name = "globalstaty";
    }

    @Override
    public void execute(@NotNull @Nonnull NewCommandContext context) {
        context.deferAsync(false);
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.decode(BRAND_COLOR))
                .setAuthor(context.getSender().getName(), null, UserUtil.getAvatarUrl(context.getSender()))
                .setFooter("© " + context.getSender().getJDA().getSelfUser().getName(),
                        UserUtil.getAvatarUrl(context.getSender().getJDA().getSelfUser()));
        eb.setTitle(context.getTranslated("globalstaty.embed.title"));
        eb.setDescription(context.getTranslated("globalstaty.embed.description"));
        Map<String, Integer> punkty = LicznikPunktow.getTotalPoints(context.getSender());
        AtomicInteger atomicInteger = new AtomicInteger();
        punkty.forEach((idSerwera, ilosc) -> atomicInteger.getAndAdd(ilosc));
        eb.addField(context.getTranslated("globalstaty.embed.points"), String.valueOf(atomicInteger.intValue()), false);
        eb.setTimestamp(Instant.now());
        context.sendMessage(Set.of(eb.build()));
    }
}
