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

package pl.fratik.commands.narzedzia;

import com.google.gson.JsonObject;
import io.sentry.Sentry;
import io.sentry.event.User;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.GuildReply;
import net.hypixel.api.reply.PlayerReply;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class HypixelCommand extends NewCommand {

    private final HypixelAPI hypixelAPI;
    private final Cache<String> uuidCache;

    public HypixelCommand(RedisCacheManager rcm) {
        name = "hypixel";
        usage = "<typ:string> <nazwa:string>";
        allowInDMs = true;
        cooldown = 3;
        hypixelAPI = new HypixelAPI(UUID.fromString(Ustawienia.instance.apiKeys.get("hypixelToken")));
        uuidCache = rcm.new CacheRetriever<String>("minecraftUUID"){}.getCache(3600);
    }

    @Override
    public void execute(@NotNull NewCommandContext context){
        //Player
        String cos = context.getArguments().get("typ").getAsString();
        String player;
        String tryb;
        String ranga = "Member";
        long last;
        long first;
        int level = 100;
        Date lastlogin;
        Date firstlogin;
        //Guild
        String name;
        String des;
        String tagname;
        String tagcolor;
        Integer members;
        long exp;
        Integer coins;
        long created;
        SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy '@' HH:mm z", context.getLanguage().getLocale());
        String arg = context.getArguments().get("nazwa").getAsString();
        context.deferAsync(false);
        if (cos.equals("player")) {
            try {
                String uuid;
                PlayerReply pr;
                if ((uuid = uuidCache.getIfPresent(arg)) != null) {
                    pr = hypixelAPI.getPlayerByUuid(uuid).join();
                } else {
                    pr = hypixelAPI.getPlayerByName(arg).join();
                    uuidCache.put(arg, pr.getPlayer().get("uuid").getAsString());
                }
                JsonObject pl = pr.getPlayer();
                if (pl.has("newPackageRank")){
                    ranga = pl.get("newPackageRank").getAsString().replaceAll("_PLUS", "+");
                    if (pl.has("monthlyPackageRank")){
                        ranga = pl.get("monthlyPackageRank").getAsString().replaceAll("SUPERSTAR", "MVP++");
                    }
                }
                player = pl.get("displayname").getAsString();
                tryb = pl.get("mostRecentGameType").getAsString();
                last = pl.get("lastLogin").getAsLong();
                first = pl.get("firstLogin").getAsLong();
                lastlogin = new Date(last);
                firstlogin = new Date(first);
                for (int lvl = 0; lvl < 101; lvl++) {
                    boolean lol = pl.has("levelingReward_" + lvl);
                    if (!lol) {
                        level = lvl;
                        break;
                    }
                }
            } catch (Exception e) {
                Sentry.getContext().setUser(new User(context.getSender().getId(),
                        UserUtil.formatDiscrim(context.getSender()), null, null));
                Sentry.capture(e);
                Sentry.clearContext();
                context.sendMessage(context.getTranslated("hypixel.error.playerapi"));
                return;
            }
            EmbedBuilder eb = new EmbedBuilder();
            String imageUrl = "https://minotar.net/helm/" + arg + "/2048.png";
            eb.setColor(CommonUtil.getPrimColorFromImageUrl(imageUrl));
            eb.setThumbnail(imageUrl);
            eb.addField(context.getTranslated("hypixel.embed.player.profile"), "[Hypixel.net](https://hypixel.net/player/" + player + ")", false);
            eb.addField(context.getTranslated("hypixel.embed.player.rank"), ranga, false);
            eb.addField(context.getTranslated("hypixel.embed.player.level"), String.valueOf(level), true);
            eb.addField(context.getTranslated("hypixel.embed.player.lasttryb"), tryb, true);
            eb.addField(context.getTranslated("hypixel.embed.player.firstlogin"), date.format(firstlogin), false);
            eb.addField(context.getTranslated("hypixel.embed.player.lastlogin"), date.format(lastlogin), true);
            context.sendMessage(eb.build());
        } else if (cos.equals("guild")) {
            try {
                GuildReply gr = hypixelAPI.getGuildByName(arg).join();
                GuildReply.Guild g = gr.getGuild();
                members = g.getMembers().size();
                name = g.getName();
                des = g.getDescription();
                created = Instant.from(g.getCreated()).toEpochMilli();
                exp = g.getExp();
                tagname = g.getTag();
                tagcolor = g.getTagColor();
                coins = g.getCoins();
            } catch (Exception e) {
                context.sendMessage(context.getTranslated("hypixel.error.guildapi"));
                return;
            }
            EmbedBuilder eb = new EmbedBuilder();
            if (tagcolor != null) eb.setColor(Kolory.valueOf(tagcolor).color);
            eb.addField(context.getTranslated("hypixel.embed.guild.profile"), "[" + name + "](https://hypixel.net/guilds/" + name + ")", false);
            eb.addField(context.getTranslated("hypixel.embed.guild.des"), des == null || des.isEmpty() ?
                    context.getTranslated("hypixel.embed.guild.des.empty") : des, false);
            eb.addField(context.getTranslated("hypixel.embed.guild.created"), date.format(created), false);
            eb.addField(context.getTranslated("hypixel.embed.guild.tagname"), tagname, false);
            if (tagcolor != null) eb.addField(context.getTranslated("hypixel.embed.guild.tagcolor"), tagcolor, false);
            eb.addField(context.getTranslated("hypixel.embed.guild.members"), String.valueOf(members), false);
            eb.addField(context.getTranslated("hypixel.embed.guild.coiny"), String.valueOf(coins), false);
            eb.addField(context.getTranslated("hypixel.embed.guild.exp"), String.valueOf(exp), true);
            context.sendMessage(eb.build());
        }
    }

    @Override
    public void updateOptionData(OptionData option) {
        if (option.getName().equals("typ")) {
            option.addChoice("gracz", "player");
            option.addChoice("gilida", "guild");
        }
    }

    private enum Kolory {

        /**
         * Represents black
         */
        BLACK(new Color(0x0)),
        /**
         * Represents dark blue
         */
        DARK_BLUE(new Color(0xAA)),
        /**
         * Represents dark green
         */
        DARK_GREEN(new Color(0xAA00)),
        /**
         * Represents dark blue (aqua)
         */
        DARK_AQUA(new Color(0xAAAA)),
        /**
         * Represents dark red
         */
        DARK_RED(new Color(0xAA0000)),
        /**
         * Represents dark purple
         */
        DARK_PURPLE(new Color(0xAA00AA)),
        /**
         * Represents gold
         */
        GOLD(new Color(0xFFAA00)),
        /**
         * Represents gray
         */
        GRAY(new Color(0xAAAAAA)),
        /**
         * Represents dark gray
         */
        DARK_GRAY(new Color(0x555555)),
        /**
         * Represents blue
         */
        BLUE(new Color(0x5555FF)),
        /**
         * Represents green
         */
        GREEN(new Color(0x55FF55)),
        /**
         * Represents aqua
         */
        AQUA(new Color(0x55FFFF)),
        /**
         * Represents red
         */
        RED(new Color(0xFF5555)),
        /**
         * Represents light purple
         */
        LIGHT_PURPLE(new Color(0xFF55FF)),
        /**
         * Represents yellow
         */
        YELLOW(new Color(0xFFFF55)),
        /**
         * Represents white
         */
        WHITE(new Color(0xFFFFFF));

        private final Color color;

        Kolory(Color color) {
            this.color = color;
        }
    }
}
