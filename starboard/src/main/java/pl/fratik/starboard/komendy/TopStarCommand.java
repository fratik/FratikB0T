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

package pl.fratik.starboard.komendy;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.MapUtil;
import pl.fratik.starboard.StarManager;
import pl.fratik.starboard.StarboardListener;
import pl.fratik.starboard.entity.StarData;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TopStarCommand extends Command {

    private final StarDataDao starDataDao;
    private final StarManager starManager;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public TopStarCommand(StarDataDao starDataDao, StarManager starManager, EventWaiter eventWaiter, EventBus eventBus) {
        this.starDataDao = starDataDao;
        this.starManager = starManager;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "topstar";
        category = CommandCategory.STARBOARD;
        cooldown = 5;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_MANAGE);
        permissions.add(Permission.MESSAGE_ADD_REACTION);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Future<Message> msgFuture = context.getChannel().sendMessage(context.getTranslated("generic.loading")).submit();
        StarsData std = starDataDao.get(context.getGuild());
        Map<String, Integer> stars = new HashMap<>();
        for (StarData sd : std.getStarData().values()) {
            if (sd.getStarredBy().size() >= std.getStarThreshold()) {
                stars.put(sd.getAuthor(), stars.getOrDefault(sd.getAuthor(), 0) + sd.getStarredBy().size());
            }
        }
        stars = MapUtil.sortByValue(stars);
        Message msg;
        try {
            msg = msgFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            return false;
        }
        List<EmbedBuilder> pages = new ArrayList<>();
        int i = 0;
        int miejsce = 0;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : stars.entrySet()) {
            try {
                sb.append(miejsce + 1).append(".").append(" **")
                        .append(context.getShardManager().retrieveUserById(entry.getKey()).complete().getAsTag())
                        .append("**: ").append(entry.getValue().intValue()).append(" ")
                        .append(StarboardListener.getStarEmoji(entry.getValue())).append("\n");
                if (i != 0 && (i + 1) % 10 == 0) {
                    pages.add(new EmbedBuilder().setColor(new Color(255, 172, 51))
                            .setDescription(sb.toString().substring(0, sb.length() - 1))
                            .setTitle(context.getTranslated("topstar.embed.header")));
                    i = -1;
                    sb = new StringBuilder();
                }
                i++;
                miejsce++;
            } catch (Exception e) {
                //nie znaleziono użytkownika, cóż
            }
        }
        if (!sb.toString().isEmpty()) {
            pages.add(new EmbedBuilder().setColor(new Color(255, 172, 51))
                    .setDescription(sb.toString().substring(0, sb.length() - 1))
                    .setTitle(context.getTranslated("topstar.embed.header")));
        }
        if (pages.isEmpty()) {
            context.send(context.getTranslated("topstar.no.stars"));
            return false;
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(msg);
        return true;
    }
}
