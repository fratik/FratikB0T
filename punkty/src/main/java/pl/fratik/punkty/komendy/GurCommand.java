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

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MapUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.punkty.LicznikPunktow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GurCommand extends Command {

    private final EventWaiter eventWaiter;
    private final ShardManager shardManager;
    private final EventBus eventBus;

    public GurCommand(EventWaiter eventWaiter, ShardManager shardManager, EventBus eventBus) {
        this.eventWaiter = eventWaiter;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
        name = "gur";
        category = CommandCategory.POINTS;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_MANAGE);
        permissions.add(Permission.MESSAGE_ADD_REACTION);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message message = context.send(context.getTranslated("generic.loading"));
        Map<String, Integer> licznikAlboCo = MapUtil.sortByValue(LicznikPunktow.getAllUserPunkty());
        List<EmbedBuilder> embedy = new ArrayList<>();
        licznikAlboCo.forEach((id, poziom) -> {
            EmbedBuilder eb = new EmbedBuilder();
            User user;
            try {
                user = shardManager.retrieveUserById(id).complete();
            } catch (Exception e) {
                user = null;
            }
            if (user == null) {
                eb.setAuthor(context.getTranslated("generic.user.unknown"));
            } else {
                eb.setAuthor(UserUtil.formatDiscrim(user));
                eb.setImage(user.getEffectiveAvatarUrl().replace(".webp", ".png") + "?size=2048");
                eb.setColor(UserUtil.getPrimColor(user));
            }
            eb.addField(context.getTranslated("gur.embed.points"), String.valueOf(poziom), false);
            embedy.add(eb);
        });
        ClassicEmbedPaginator embedPaginator = new ClassicEmbedPaginator(eventWaiter, embedy, context.getSender(),
                context.getLanguage(), context.getTlumaczenia(), eventBus);
        embedPaginator.create(message);
        return true;
    }
}
