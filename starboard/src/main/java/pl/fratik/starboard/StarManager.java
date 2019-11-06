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

package pl.fratik.starboard;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.starboard.entity.StarData;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;
import pl.fratik.starboard.event.StarEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class StarManager {

    private final StarDataDao starDataDao;
    private final EventBus eventBus;

    private static final Cache<String, String> starEmojiCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit
            .MINUTES).maximumSize(1000).build();

    StarManager(StarDataDao starDataDao, EventBus eventBus) {
        this.starDataDao = starDataDao;
        this.eventBus = eventBus;
    }

    void addStar(Message message, User user, StarsData std) {
        StarData starData = new StarData(message.getAuthor().getId());
        if (std.getStarData().get(message.getId()) != null)
            starData = std.getStarData().get(message.getId());
        List<String> starredBy = starData.getStarredBy();
        if (starredBy.contains(user.getId())) throw new IllegalStateException("juz dal gwiazdkę");
        starredBy.add(user.getId());
        starData.setStarredBy(starredBy);
        starData.setChannel(message.getChannel().getId());
        starData.setGuild(message.getGuild().getId());
        Map<String, StarData> starDataMap = std.getStarData();
        starDataMap.put(message.getId(), starData);
        std.setStarData(starDataMap);
        starDataDao.save(std);
        eventBus.post(new StarEvent(user, message, starData, std.getStarboardChannel()));
    }

    public void fixStars(Message message, StarsData std) {
        Optional<MessageReaction> reaction = message.getReactions().stream()
                .filter(r -> r.getReactionEmote().isEmote() ?
                        r.getReactionEmote().getEmote().equals(getStar(message.getGuild())) :
                        r.getReactionEmote().getName().equals(getStar(message.getGuild()))).findFirst();
        if (reaction.isPresent()) fixStars(message, std, reaction.get());
        else fixStars(message, std, null);
    }

    void fixStars(Message message, StarsData std, MessageReaction reaction) {
        StarData starData = new StarData(message.getAuthor().getId());
        if (starDataDao.get(message.getGuild()).getStarData().get(message.getId()) != null)
            starData = std.getStarData().get(message.getId());
        starData.setAuthor(message.getAuthor().getId());
        List<String> starredBy = new ArrayList<>();
        if (reaction != null) {
            List<User> userList = reaction.retrieveUsers().complete();
            for (User user : userList)
                starredBy.add(user.getId());
        }
        starData.setStarredBy(starredBy);
        starData.setChannel(message.getChannel().getId());
        starData.setGuild(message.getGuild().getId());
        if (starredBy.size() < std.getStarThreshold()) {
            Map<String, StarData> starDataMap = std.getStarData();
            starDataMap.remove(message.getId());
            std.setStarData(starDataMap);
            starDataDao.save(std);
            eventBus.post(new StarEvent(null, message, starredBy.size(), message.getTextChannel(),
                    std.getStarboardChannel(), starData.getStarboardMessageId()));
            return;
        }
        if (starData.getStarboardMessageId() != null && std.getStarboardChannel() != null) {
            try {
                Message msg = Objects.requireNonNull(message.getGuild().getTextChannelById(std.getStarboardChannel()))
                        .retrieveMessageById(starData.getStarboardMessageId()).complete();
                for (MessageReaction reacc : msg.getReactions()) {
                    if ((reacc.getReactionEmote().isEmote() && !reacc.getReactionEmote().getEmote().getId()
                            .equals(getStar(message.getGuild()))) || (!reacc.getReactionEmote().isEmote()
                            && !reacc.getReactionEmote().getName().equals(getStar(message.getGuild())))) continue;
                    List<User> userList = reacc.retrieveUsers().complete();
                    for (User user : userList) {
                        if (starredBy.contains(user.getId())) {
                            reacc.removeReaction(user).queue(null, t -> {});
                            continue;
                        }
                        starredBy.add(user.getId());
                    }
                }
            } catch (Exception ignored) {/*lul*/}
        }
        Map<String, StarData> starDataMap = std.getStarData();
        starDataMap.put(message.getId(), starData);
        std.setStarData(starDataMap);
        starDataDao.save(std);
        eventBus.post(new StarEvent(null, message, starData, std.getStarboardChannel()));

    }

    void removeStar(Message message, User user, StarsData std) {
        StarData starData = new StarData(message.getAuthor().getId());
        if (std.getStarData().get(message.getId()) != null)
            starData = std.getStarData().get(message.getId());
        List<String> starredBy = starData.getStarredBy();
        starData.setChannel(message.getChannel().getId());
        starData.setGuild(message.getGuild().getId());
        if (!starredBy.contains(user.getId())) throw new IllegalStateException("nie dal gwiazdki");
        starredBy.remove(user.getId());
        if (starredBy.size() < std.getStarThreshold()) {
            Map<String, StarData> starDataMap = std.getStarData();
            starDataMap.remove(message.getId());
            std.setStarData(starDataMap);
            starDataDao.save(std);
            eventBus.post(new StarEvent(null, message, starredBy.size(), message.getTextChannel(), std.getStarboardChannel(), starData.getStarboardMessageId()));
            return;
        }
        starData.setStarredBy(starredBy);
        Map<String, StarData> starDataMap = std.getStarData();
        starDataMap.put(message.getId(), starData);
        std.setStarData(starDataMap);
        starDataDao.save(std);
        eventBus.post(new StarEvent(user, message, starData, std.getStarboardChannel()));
    }

    void resetStars(Message message) {
        StarsData std = starDataDao.get(message.getGuild());
        Map<String, StarData> starDataMap = std.getStarData();
        if (starDataMap.get(message.getId()) == null) return;
        StarData starData = starDataMap.remove(message.getId());
        std.setStarData(starDataMap);
        starDataDao.save(std);
        eventBus.post(new StarEvent(null, message, 0, message.getTextChannel(), std.getStarboardChannel(), starData.getStarboardMessageId()));
    }

    Object getStar(Guild guild) {
        String e = Objects.requireNonNull(starEmojiCache.get(guild.getId(),
                uid -> starDataDao.get(guild).getStarEmoji()));

        Object emotka = null;
        try {
            emotka = guild.getEmoteById(e);
        } catch (Exception ignored) {
            if (EmojiUtils.isEmoji(e)) emotka = e;
        }
        if (emotka == null) emotka = "\u2b50";
        return emotka;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean checkPermissions(TextChannel kanal) {
        return kanal.getGuild().getSelfMember().hasPermission(kanal, Permission.MESSAGE_WRITE,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY);
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent event) {
        if (event.getEntity() instanceof GuildConfig) {
            for (String guildId : starEmojiCache.asMap().keySet()) {
                if (((GuildConfig) event.getEntity()).getGuildId().equals(guildId)) {
                    starEmojiCache.invalidate(guildId);
                    return;
                }
            }
        }
    }
}
