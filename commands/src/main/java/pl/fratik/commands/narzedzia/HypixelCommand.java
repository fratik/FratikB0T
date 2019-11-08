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

package pl.fratik.commands.narzedzia;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.GuildReply;
import net.hypixel.api.reply.PlayerReply;
import net.hypixel.api.reply.WatchdogStatsReply;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class HypixelCommand extends Command {

    private final HypixelAPI hypixelAPI;
    private WatchdogStatsReply wdr;

    public HypixelCommand() {
        name = "hypixel";
        category = CommandCategory.UTILITY;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("typ", "string");
        hmap.put("nazwa", "string");
        uzycie = new Uzycie(hmap, new boolean[] {true, false});
        uzycieDelim = " ";
        allowInDMs = true;
        aliases = new String[] {"hp"};
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        cooldown = 5; // Jak będzie za dużo requestów, to Hypixel pernametnie banuje token ~ Kamil
        hypixelAPI = new HypixelAPI(UUID.fromString(Ustawienia.instance.apiKeys.get("hypixelToken")));
    }

    @Override
    public boolean execute(@NotNull CommandContext context){
        Object[] args = context.getArgs();

        SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy '@' HH:mm z", context.getLanguage().getLocale());
        if (context.getArgs()[0].equals("player")) {
            if (context.getArgs()[1] == null) {
                CommonErrors.usage(context);
                return false;
            }
            String player;
            String wersja;
            String tryb;
            String jezyk;
            String ranga = "Member";
            Integer karma;
            long last;
            long first;
            int level = 100;
            Date lastlogin;
            Date firstlogin;
            PlayerReply pr = hypixelAPI.getPlayerByName((String) context.getArgs()[1]).join();
            try {
                JsonObject pl = pr.getPlayer();
                if (pl.has("newPackageRank")){
                    ranga = pl.get("newPackageRank").getAsString().replaceAll("_PLUS", "+");
                    if (pl.has("monthlyPackageRank")){
                        ranga = pl.get("monthlyPackageRank").getAsString().replaceAll("SUPERSTAR", "MVP++");
                    }
                }
                player = pl.get("displayname").getAsString();
                wersja = pl.get("mcVersionRp").getAsString();
                tryb = pl.get("mostRecentGameType").getAsString();
                jezyk = pl.has("userLanguage") ? pl.get("userLanguage").getAsString() : context.getTranslated("hypixel.player.language.notset");
                karma = pl.get("karma").getAsInt();
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
                context.send(context.getTranslated("hypixel.error.playerapi"));
                return false;
            }
            EmbedBuilder eb = new EmbedBuilder();
            String imageUrl = "https://minotar.net/helm/" + context.getArgs()[1] + "/2048.png";
            eb.setColor(CommonUtil.getPrimColorFromImageUrl(imageUrl));
            eb.setThumbnail(imageUrl);
            eb.addField(context.getTranslated("hypixel.embed.player.profile"), "[Hypixel.net](https://hypixel.net/player" + player + ")", false);
            eb.addField(context.getTranslated("hypixel.embed.player.rank"), ranga, false);
            eb.addField(context.getTranslated("hypixel.embed.player.level"), String.valueOf(level), true);
            eb.addField(context.getTranslated("hypixel.embed.player.version"), wersja, false);
            eb.addField(context.getTranslated("hypixel.embed.player.lasttryb"), tryb, true);
            eb.addField(context.getTranslated("hypixel.embed.player.firstlogin"), date.format(firstlogin), false);
            eb.addField(context.getTranslated("hypixel.embed.player.lastlogin"), date.format(lastlogin), true);
            eb.addField(context.getTranslated("hypixel.embed.player.language"), jezyk, false);
            eb.addField(context.getTranslated("hypixel.embed.player.karma"), String.valueOf(karma), true);
            context.send(eb.build());
            return true;
        }
        if (Objects.equals(context.getArgs()[0], "guild")) {
            if (context.getArgs()[1] == null) {
                CommonErrors.usage(context);
                return false;
            }
            String name;
            String des;
            String tagname;
            String tagcolor;
            Integer members;
            long exp;
            Integer coins;
            long created;
            try {
                GuildReply gr = hypixelAPI.getGuildByName((String) context.getArgs()[1]).join();
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
                context.send(context.getTranslated("hypixel.error.guildapi"));
                return false;
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Kolory.valueOf(tagcolor).color);
            eb.addField(context.getTranslated("hypixel.embed.guild.profile"), "[" + name + "](https://hypixel.net/guilds" + name + ")", false);
            eb.addField(context.getTranslated("hypixel.embed.guild.des"), des == null || des.isEmpty() ?
                    context.getTranslated("hypixel.embed.guild.des.empty") : des, false);
            eb.addField(context.getTranslated("hypixel.embed.guild.created"), date.format(created), false);
            eb.addField(context.getTranslated("hypixel.embed.guild.tagname"), tagname, false);
            eb.addField(context.getTranslated("hypixel.embed.guild.tagcolor"), tagcolor, true);
            eb.addField(context.getTranslated("hypixel.embed.guild.members"), String.valueOf(members), false);
            eb.addField(context.getTranslated("hypixel.embed.guild.coiny"), String.valueOf(coins), false);
            eb.addField(context.getTranslated("hypixel.embed.guild.exp"), String.valueOf(exp), true);
            context.send(eb.build());
            return true;
        } if (context.getArgs()[0].equals("wdr")) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
            eb.setTitle(context.getTranslated("hypixel.embed.wdr.title"));
            eb.setDescription(context.getTranslated("hypixel.embed.wdr.description"));

            eb.addField(context.getTranslated("hypixel.embed.wdr.stafftotal"), string(wdr.getStaffTotal(), context), false);
            eb.addField(context.getTranslated("hypixel.embed.wdr.wdrtotal"), string(wdr.getWatchdogTotal(), context), false);

            eb.addField(context.getTranslated("hypixel.embed.wdr.staffdaily"), string(wdr.getStaffRollingDaily(), context), false);
            eb.addField(context.getTranslated("hypixel.embed.wdr.wdrlastminute"), string(wdr.getWatchdogLastMinute(), context), false);

            eb.addField(context.getTranslated("hypixel.embed.wdr.stafflastday"), string(wdr.getStaffRollingDaily(), context), false);
            eb.addField(context.getTranslated("hypixel.embed.wdr.wdrlastday"), string(wdr.getWatchdogRollingDaily(), context), false);


            context.send(eb.build());
            return true;
        }
        CommonErrors.usage(context);
        return false;
    }

    private String string(int eldo, CommandContext ctx) {
        return String.format("%s %s", eldo, ctx.getTranslated("hypixel.embed.wdr.ban"));
    }

    private String rzeczownik(int liczba, String drugaZmiana, String trzeciaZmiana) {
        return liczba == 1 ? "" : liczba <= 4 && liczba >= 2 ? drugaZmiana : trzeciaZmiana; //NOSONAR
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
