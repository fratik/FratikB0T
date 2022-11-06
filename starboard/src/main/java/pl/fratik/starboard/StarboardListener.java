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

package pl.fratik.starboard;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.starboard.entity.StarData;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;
import pl.fratik.starboard.event.StarEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class StarboardListener {

    private final StarDataDao starDataDao;
    private final Tlumaczenia tlumaczenia;
    private final StarManager starManager;
    private final ExecutorService executor;
    private final List<String> toIgnore = new ArrayList<>();
    private static final String SMSGSEP = " \\| ";

    private final Cache<StarsData> stdCache;

    StarboardListener(StarDataDao starDataDao, Tlumaczenia tlumaczenia, StarManager starManager, ExecutorService executor, RedisCacheManager redisCacheManager) {
        this.starDataDao = starDataDao;
        this.tlumaczenia = tlumaczenia;
        this.starManager = starManager;
        this.executor = executor;
        stdCache = redisCacheManager.new CacheRetriever<StarsData>(){}.getCache();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void starAddEvent(MessageReactionAddEvent event) {
        executor.submit(() -> {
            if (!event.isFromGuild()) return;
            boolean star = false;
            Object emotka = starManager.getStar(event.getGuild());
            if (event.getEmoji().getType() == Emoji.Type.CUSTOM) {
                if (event.getEmoji().asCustom().equals(emotka)) star = true;
            } else if (event.getEmoji().asUnicode().getName().equals(emotka)) star = true;
            if (!star || getChannel(event.getGuild()) == null) return;
            Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
            Consumer<Throwable> throwableConsumer = o -> toIgnore.remove(event.getMessageId());
            if (!message.getChannel().getId().equals(getChannel(event.getGuild()).getId()) ||
                    !message.getAuthor().equals(message.getJDA().getSelfUser()) || message.getEmbeds().isEmpty()) {
                if (!StarManager.checkPermissions(getChannel(event.getGuild()))) {
                    Language l = tlumaczenia.getLanguage(message.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.noperms", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
                    return;
                }
                boolean isNsfw;
                if (event.getChannel() instanceof ThreadChannel)
                    isNsfw = ((TextChannel) ((ThreadChannel) event.getChannel()).getParentChannel()).isNSFW();
                else if (event.getChannel() instanceof TextChannel) isNsfw = ((TextChannel) event.getChannel()).isNSFW();
                else isNsfw = false;
                if (isNsfw && !getChannel(event.getGuild()).isNSFW()) {
                    Language l = tlumaczenia.getLanguage(message.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.nsfw", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
                    return;
                }
                if (event.getUser().equals(message.getAuthor())) {
                    Language l = tlumaczenia.getLanguage(message.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.cant.star.yourself", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
                    return;
                }
                StarsData std = stdCache.get(message.getGuild().getId(), starDataDao::get);
                if (std.getBlacklista() == null) std.setBlacklista(new ArrayList<>());
                if (std.getBlacklista().contains(event.getUser().getId())) {
                    Language l = tlumaczenia.getLanguage(message.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.blacklisted", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
                    return;
                }
                try {
                    starManager.addStar(message, event.getUser(), starDataDao.get(event.getGuild()));
                } catch (IllegalStateException e) {
                    Language l = tlumaczenia.getLanguage(message.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.already.starred", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
                }
            } else {
                String mid = Objects.requireNonNull(Objects.requireNonNull(message.getEmbeds().get(0).getFooter())
                        .getText()).split(SMSGSEP)[1].trim();
                StarsData starsData = starDataDao.get(message.getGuild());
                StarData std = starsData.getStarData().get(mid);
                if (std.getChannel() == null || std.getChannel().isEmpty() || event.getGuild().getTextChannelById(std.getChannel()) == null) {
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                }
                Message msg = Objects.requireNonNull(event.getGuild().getTextChannelById(std.getChannel()))
                        .retrieveMessageById(mid).complete();
                if (event.getUser().equals(msg.getAuthor())) {
                    Language l = tlumaczenia.getLanguage(msg.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.cant.star.yourself", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }
                if (starsData.getBlacklista() == null) starsData.setBlacklista(new ArrayList<>());
                if (starsData.getBlacklista().contains(event.getUser().getId())) {
                    Language l = tlumaczenia.getLanguage(msg.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.blacklisted", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }
                try {
                    starManager.addStar(msg, event.getUser(), starsData);
                } catch (IllegalStateException e) {
                    Language l = tlumaczenia.getLanguage(msg.getMember());
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, throwableConsumer);
                    event.getChannel().sendMessage(tlumaczenia.get(l, "starboard.already.starred", event.getUser().getAsMention()))
                            .queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
                }
            }
        });
    }

    @Subscribe
    public void starRemoveEvent(MessageReactionRemoveEvent event) {
        executor.submit(() -> {
            if (!event.isFromGuild()) return;
            if (toIgnore.contains(event.getMessageId())) {
                toIgnore.remove(event.getMessageId());
                return;
            }
            boolean star = false;
            Object emotka = starManager.getStar(event.getGuild());
            if (event.getEmoji().getType() == Emoji.Type.CUSTOM) {
                if (event.getEmoji().asCustom().equals(emotka)) star = true;
            } else if (event.getEmoji().asUnicode().getName().equals(emotka)) star = true;
            if (!star || getChannel(event.getGuild()) == null) return;
            Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
            StarsData starsData = starDataDao.get(event.getGuild());
            if (message.getChannel().getId().equals(getChannel(event.getGuild()).getId()) &&
                    message.getAuthor().equals(message.getJDA().getSelfUser()) && !message.getEmbeds().isEmpty()) {
                String mid = Objects.requireNonNull(Objects.requireNonNull(message.getEmbeds().get(0)
                        .getFooter()).getText()).split(SMSGSEP)[1].trim();
                StarData std = starsData.getStarData().getOrDefault(mid, new StarData(null));
                if (std.getChannel() == null || std.getChannel().isEmpty() || event.getGuild().getTextChannelById(std.getChannel()) == null) {
                    toIgnore.add(event.getMessageId());
                    event.getReaction().removeReaction(event.getUser()).queue(null, o -> toIgnore.remove(event.getMessageId()));
                }
                Message msg = Objects.requireNonNull(event.getGuild().getTextChannelById(std.getChannel()))
                        .retrieveMessageById(mid).complete();
                if (!std.getStarredBy().contains(event.getUser().getId()))
                    starManager.fixStars(msg, starsData, event.getReaction());
                else starManager.removeStar(msg, event.getUser(), starsData);
            }
            if (!starsData.getStarData().getOrDefault(message.getId(), new StarData(message.getAuthor().getId()))
                    .getStarredBy().contains(event.getUser().getId()))
                starManager.fixStars(message, starsData, event.getReaction());
            else starManager.removeStar(message, event.getUser(), starsData);
        });
    }

    @Subscribe
    public void starPruneEvent(MessageReactionRemoveAllEvent event) {
        executor.submit(() -> {
            if (!event.isFromGuild()|| getChannel(event.getGuild()) == null) return;
            if (!StarManager.checkPermissions(getChannel(event.getGuild()))) return;
            Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
            if (message.getChannel().getId().equals(getChannel(event.getGuild()).getId()) &&
                    message.getAuthor().equals(message.getJDA().getSelfUser()) && !message.getEmbeds().isEmpty()) {
                String mid = Objects.requireNonNull(Objects.requireNonNull(message.getEmbeds().get(0)
                        .getFooter()).getText()).split(SMSGSEP)[1].trim();
                StarsData starsData = starDataDao.get(message.getGuild());
                StarData std = starsData.getStarData().get(mid);
                if (std.getChannel() == null || std.getChannel().isEmpty() || event.getGuild()
                        .getTextChannelById(std.getChannel()) == null)
                    return;
                starManager.fixStars(Objects.requireNonNull(event.getGuild().getTextChannelById(std.getChannel()))
                        .retrieveMessageById(mid).complete(), starsData);
                return;
            }
            starManager.resetStars(message);
        });
    }

    @Subscribe
    public void starEventHandler(StarEvent event) {
        GuildMessageChannel starboardChannel = event.getStarboardChannel();
        if (!StarManager.checkPermissions(starboardChannel)) return;
        if (event.getStarboardMessageId() == null) {
            if (event.getGwiazdki() < starDataDao.get(event.getMessage().getGuild()).getStarThreshold()) return;
            starboardChannel.sendMessageEmbeds(embedRenderer(event.getMessage(), event.getGwiazdki())).queue(msg -> {
                        StarsData std = starDataDao.get(event.getMessage().getGuild());
                        std.getStarData().get(event.getMessage().getId()).setStarboardMessageId(msg.getId());
                        starDataDao.save(std);
                    });
        } else {
            starboardChannel.retrieveMessageById(event.getStarboardMessageId())
                    .queue(message -> {
                        if (event.getGwiazdki() < starDataDao.get(event.getMessage().getGuild()).getStarThreshold()) {
                            message.delete().queue();
                            StarsData std = starDataDao.get(event.getMessage().getGuild());
                            Map<String, StarData> starDataMap = std.getStarData();
                            starDataMap.remove(event.getMessage().getId());
                            std.setStarData(starDataMap);
                            starDataDao.save(std);
                            return;
                        }
                        message.editMessageEmbeds(embedRenderer(event.getMessage(), event.getGwiazdki())).setReplace(true).queue();
                        }, ignored -> starboardChannel.sendMessageEmbeds(embedRenderer(event.getMessage(), event.getGwiazdki())).queue(msg -> {
                                StarsData std = starDataDao.get(event.getMessage().getGuild());
                                std.getStarData().get(event.getMessage().getId()).setStarboardMessageId(msg.getId());
                                starDataDao.save(std);
                            }));
        }
    }

    private MessageEmbed embedRenderer(Message message, int stars) {
        EmbedBuilder eb = new EmbedBuilder();
        if (!message.getAttachments().isEmpty())
            eb.setImage(message.getAttachments().get(0).getUrl());
        eb.setThumbnail(message.getAuthor().getEffectiveAvatarUrl().replace(".webp", ".png"));
        eb.setColor(new Color(255, 172, 51));
        Language l = tlumaczenia.getLanguage(message.getGuild());
        eb.addField(tlumaczenia.get(l, "starboard.embed.author"), message.getAuthor().getAsMention(), true);
        eb.addField(tlumaczenia.get(l, "starboard.embed.channel"), "<#" + message.getChannel().getId() + ">", true);
        eb.setFooter(String.format("%s %d | %s", getStarEmoji(stars), stars, message.getId()), null);
        String content = message.getContentRaw();
        if (content.length() > 1023) content = content.substring(0, 1021) + "...";
        if (content.length() != 0) eb.addField(tlumaczenia.get(l, "starboard.embed.message"), content, true);
        String link = CommonUtil.getImageUrl(message);
        if (link != null) eb.setImage(link);
        eb.addField(tlumaczenia.get(l, "starboard.embed.jump"), String.format("[\\[%s\\]](%s)",
                tlumaczenia.get(l, "starboard.embed.jump.to"), message.getJumpUrl()), false);
        eb.setTimestamp(message.getTimeCreated());
        return eb.build();
    }

    public static String getStarEmoji(int gwiazdki) {
        if (gwiazdki < 4) return "\u2b50";
        if (gwiazdki < 8) return "️\uD83C\uDF1F";
        if (gwiazdki < 12) return "\u2728";
        if (gwiazdki < 16) return "\uD83D\uDCAB";
        if (gwiazdki < 20) return "\ud83c\udf87";
        if (gwiazdki < 24) return "\ud83c\udf86";
        if (gwiazdki < 28) return "\u2604\ufe0f";
        if (gwiazdki < 32) return "\ud83c\udf20";
        return "\ud83c\udf0c";
    }

    private TextChannel getChannel(Guild guild) {
        StarsData std = stdCache.get(guild.getId(), starDataDao::get);
        String id = null;
        if (!std.getStarboardChannel().isEmpty()) {
            TextChannel kanal = guild.getTextChannelById(std.getStarboardChannel());
            if (kanal == null) id = null;
            else id = kanal.getId();
        }
        if (id == null) return null;
        return guild.getTextChannelById(id);
    }
}
