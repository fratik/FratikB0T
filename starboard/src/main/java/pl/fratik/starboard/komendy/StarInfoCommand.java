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

package pl.fratik.starboard.komendy;

import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.starboard.StarManager;
import pl.fratik.starboard.StarboardListener;
import pl.fratik.starboard.entity.StarData;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StarInfoCommand extends Command {

    private final StarDataDao starDataDao;
    private final StarManager starManager;

    public StarInfoCommand(StarDataDao starDataDao, StarManager starManager) {
        this.starDataDao = starDataDao;
        this.starManager = starManager;
        name = "starinfo";
        category = CommandCategory.STARBOARD;
        cooldown = 15;
        uzycie = new Uzycie("osoba", "user", false);
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        allowPermLevelChange = false;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User user = context.getSender();
        if (context.getArgs().length >= 1 && context.getArgs()[0] != null) user = (User) context.getArgs()[0];
        List<StarData> stars = new ArrayList<>();
        int localStars = 0;
        int globalStars = 0;
        List<Message> toFix = new ArrayList<>();
        for (StarsData std : starDataDao.getAll()) {
            if (std.getStarboardChannel() == null || std.getStarboardChannel().isEmpty()) continue;
            TextChannel stch = context.getShardManager().getTextChannelById(std.getStarboardChannel());
            if (stch != null) {
                for (StarData sd : std.getStarData().values()) {
                    GuildChannel sch = context.getShardManager().getGuildChannelById(sd.getChannel());
                    if (!(sch instanceof GuildMessageChannel)) continue;
                    if (sd.getStarredBy().size() >= std.getStarThreshold()) {
                        boolean caught = false;
                        if (sd.getGuild() == null) {
                            try {
                                Message sbMsg = stch.retrieveMessageById(sd.getStarboardMessageId()).complete();
                                Message sMsg = ((GuildMessageChannel) sch)
                                        .retrieveMessageById(Objects.requireNonNull(Objects.requireNonNull(sbMsg.getEmbeds()
                                                .get(0).getFooter()).getText()).split("\\|")[1].trim())
                                        .complete();
                                toFix.add(sMsg);
                                sd.setGuild(sMsg.getGuild().getId());
                            } catch (Exception e) {
                                caught = true;
                            }
                        }
                        if (!caught && sd.getAuthor().equals(user.getId())) stars.add(sd);
                    }
                }
            }
        }
        StarDataExtended topStarData = new StarDataExtended();
        for (StarData sd : stars) {
            if (sd.getGuild().equals(context.getGuild().getId()) &&
                    sd.getStarredBy().size() >= topStarData.getStarredBy().size()) topStarData = new StarDataExtended(sd);
            if (sd.getGuild().equals(context.getGuild().getId())) localStars += sd.getStarredBy().size();
            globalStars += sd.getStarredBy().size();
        }
        try {
            //noinspection ConstantConditions
            Message sbMsg = context.getShardManager().getTextChannelById(starDataDao.get(topStarData.getGuild()).getStarboardChannel())
                    .retrieveMessageById(topStarData.getStarboardMessageId()).complete();
            //noinspection ConstantConditions
            Message sMsg = ((GuildMessageChannel) context.getShardManager().getGuildChannelById(topStarData.getChannel()))
                    .retrieveMessageById(sbMsg.getEmbeds().get(0).getFooter().getText().split("\\|")[1].trim())
                    .complete();
            topStarData.msg = sMsg;
            topStarData.id = sMsg.getId();
        } catch (Exception ignored) {
            /*lul*/
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl());
        eb.setColor(new Color(255, 172, 51));
        StringBuilder sb = eb.getDescriptionBuilder();
        sb.append("**").append(context.getTranslated("starinfo.embed.local")).append(":** ").append(localStars).append(" ")
                .append(StarboardListener.getStarEmoji(localStars)).append("\n");
        sb.append("**").append(context.getTranslated("starinfo.embed.global")).append(":** ").append(globalStars).append(" ")
                .append(StarboardListener.getStarEmoji(localStars)).append("\n");
        if (topStarData.msg != null) {
            eb.setImage(CommonUtil.getImageUrl(topStarData.msg));
            eb.addField(context.getTranslated("starinfo.embed.topstar"), topStarData.msg.getContentRaw() + "\n\n" +
                    topStarData.getStarredBy().size() + " " +
                    StarboardListener.getStarEmoji(topStarData.getStarredBy().size()) + " | " +
                    topStarData.id, false);
        }
        context.reply(eb.build());
        for (Message fix : toFix) {
            starManager.fixStars(fix, starDataDao.get(context.getGuild()));
        }
        return true;
    }

    @EqualsAndHashCode(callSuper = true)
    private static class StarDataExtended extends StarData {
        private Message msg;
        private String id;

        StarDataExtended(StarData starData) {
            super(starData.getStarredBy(), starData.getStarredOn(), starData.getAuthor(),
                    starData.getGuild(), starData.getChannel(), starData.getStarboardMessageId());
        }

        StarDataExtended() {
            super(null);
        }
    }
}
