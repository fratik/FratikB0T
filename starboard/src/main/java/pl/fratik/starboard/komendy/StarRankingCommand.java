/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.DynamicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.starboard.StarboardListener;
import pl.fratik.starboard.entity.StarData;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.FutureTask;

public class StarRankingCommand extends Command {

    private final StarDataDao starDataDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;

    public StarRankingCommand(StarDataDao starDataDao, EventWaiter eventWaiter, EventBus eventBus) {
        this.starDataDao = starDataDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        name = "starranking";
        category = CommandCategory.STARBOARD;
        cooldown = 15;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message msg = context.getChannel().sendMessage(context.getTranslated("generic.loading")).complete();
        List<FutureTask<EmbedBuilder>> pages = new LinkedList<>();
        StarsData std = starDataDao.get(context.getGuild());
        if (std.getStarboardChannel() == null || std.getStarboardChannel().isEmpty()) {
            context.send(context.getTranslated("starranking.no.channel"));
            return false;
        }
        TextChannel stdch = context.getShardManager().getTextChannelById(std.getStarboardChannel());
        if (stdch == null) {
            context.send(context.getTranslated("starranking.no.channel"));
            return false;
        }
        List<StarData> starDataList = new ArrayList<>(std.getStarData().values());
        starDataList.sort((uno, dos) -> dos.getStarredBy().size() - uno.getStarredBy().size());
        for (StarData sd : starDataList) {
            if (pages.size() == 100 || sd.getAuthor() == null || sd.getChannel() == null || sd.getStarboardMessageId() == null) continue;
            pages.add(new FutureTask<>(() -> {
                User user = context.getShardManager().getUserById(sd.getAuthor());
                if (user == null) user = context.getShardManager().retrieveUserById(sd.getAuthor()).complete();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl());
                eb.setColor(new Color(255, 172, 51));
                Message sbMsg = stdch.retrieveMessageById(sd.getStarboardMessageId()).complete();
                try {
                    Message sMsg = Objects.requireNonNull(context.getShardManager().getTextChannelById(sd.getChannel()))
                            .retrieveMessageById(Objects.requireNonNull(Objects.requireNonNull(sbMsg.getEmbeds().get(0)
                                    .getFooter()).getText()).split("\\|")[1].trim())
                            .complete();
                    eb.setImage(CommonUtil.getImageUrl(sMsg));
                    eb.setDescription(sMsg.getContentRaw());
                    eb.setFooter(String.format("%d %s | %s | %%s/%%s", sd.getStarredBy().size(),
                            StarboardListener.getStarEmoji(sd.getStarredBy().size()), sMsg.getId()), null);
                } catch (Exception e) {
                    eb.setDescription(context.getTranslated("starranking.missing.message"));
                    eb.setFooter(String.format("%d %s | %s | %%s/%%s", sd.getStarredBy().size(),
                            StarboardListener.getStarEmoji(sd.getStarredBy().size()), "---"), null);
                }
                return eb;
            }));
        }
        if (pages.isEmpty()) {
            context.send(context.getTranslated("starranking.nostars"));
            return false;
        }
        new DynamicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).setCustomFooter(true).create(msg);
        return true;
    }

}
