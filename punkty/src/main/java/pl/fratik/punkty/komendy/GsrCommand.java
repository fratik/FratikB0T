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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.GuildUtil;
import pl.fratik.core.util.MapUtil;
import pl.fratik.punkty.LicznikPunktow;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GsrCommand extends NewCommand {

    private final EventWaiter eventWaiter;
    private final ShardManager shardManager;
    private final EventBus eventBus;

    public GsrCommand(EventWaiter eventWaiter, ShardManager shardManager, EventBus eventBus) {
        this.eventWaiter = eventWaiter;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        name = "gsr";
    }

    @Override
    public void execute(@NotNull @Nonnull NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        Map<String, Integer> licznikAlboCo = MapUtil.sortByValueAsc(LicznikPunktow.getAllGuildPunkty());
        List<EmbedBuilder> embedy = new ArrayList<>();
        licznikAlboCo.forEach((id, poziom) -> {
            EmbedBuilder eb = new EmbedBuilder();
            Guild guild = shardManager.getGuildById(id);
            if (guild != null) {
                eb.setAuthor(guild.getName());
                String urlIkony = guild.getIconUrl();
                if (urlIkony != null) eb.setImage(urlIkony.replace(".webp", ".png") + "?size=2048");
                else eb.setImage(Ustawienia.instance.botUrl + "/genBigIcon/" + guild.getId());
                eb.addField(context.getTranslated("gsr.embed.points"), String.valueOf(poziom), false);
                eb.setColor(GuildUtil.getPrimColor(guild));
            } else {
                eb.setAuthor(context.getTranslated("gsr.embed.guild.notfound"));
                eb.addField(context.getTranslated("gsr.embed.points"), String.valueOf(poziom), false);
                eb.setColor(new Color(114, 137, 218));
            }
            embedy.add(eb);
        });
        ClassicEmbedPaginator embedPaginator = new ClassicEmbedPaginator(eventWaiter, embedy, context.getSender(),
                context.getLanguage(), context.getTlumaczenia(), eventBus);
        embedPaginator.create(hook);
    }
}
