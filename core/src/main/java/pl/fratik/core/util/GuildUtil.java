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

package pl.fratik.core.util;

import com.google.common.eventbus.Subscribe;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.entity.GbanDao;
import pl.fratik.core.entity.GbanData;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.DatabaseUpdateEvent;

import java.awt.*;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

public class GuildUtil {

    @Setter private static Cache<GbanData> gbanCache;
    @Setter private static Cache<String> timeZoneCache;

    @Setter private static GuildDao guildDao;
    @Setter private static GbanDao gbanDao;
    @Setter private static ShardManager shardManager;

    public static Color getPrimColor(Guild guild) {
        try {
            JSONObject zdjecie = NetworkUtil.getJson(Ustawienia.instance.apiUrls.get("image-server") +
                            "/api/image/primColor?imageURL=" + URLEncoder.encode(Objects.requireNonNull(guild.getIconUrl())
                            .replace(".webp", ".png") + "?size=512", "UTF-8"),
                    Ustawienia.instance.apiKeys.get("image-server"));
            if (zdjecie == null) return new Color(114, 137, 218);
            int r = -1;
            int g = -1;
            int b = -1;
            for (Object color : zdjecie.getJSONArray("color")) {
                if (r == -1) r = (int) color;
                if (g == -1) g = (int) color;
                if (b == -1) b = (int) color;
            }
            return new Color(r, g, b);
        } catch (Exception e) {
            LoggerFactory.getLogger(GuildUtil.class).error("Błąd w uzyskiwaniu koloru!", e);
            return new Color(114, 137, 218);
        }
    }

    public static TimeZone getTimeZone(Guild guild) {
        String z = getTimeZone0(guild);
        if (z.equals("default")) return TimeZone.getDefault();
        return TimeZone.getTimeZone(z);
    }

    public static String getTimeZone0(Guild guild) {
        return timeZoneCache.get(guild.getId(), u -> {
            GuildConfig gc = guildDao.get(guild);
            return gc.getTimezone();
        });
    }

    public static TimeZone getTimeZone(Guild guild, GuildConfig gc) {
        String z = getTimeZone0(guild, gc);
        if (z.equals("default")) return TimeZone.getDefault();
        return TimeZone.getTimeZone(z);
    }

    public static String getTimeZone0(Guild guild, GuildConfig gc) {
        return timeZoneCache.get(guild.getId(), tak -> gc.getTimezone());
    }

    public static boolean isGbanned(Guild guild) {
        return getGbanData(guild).isGbanned();
    }

    public static GbanData getGbanData(Guild guild) {
        GbanData gbanned = gbanCache.getIfPresent(guild);
        if (gbanned == null) {
            GbanData config = gbanDao.get(guild);
            gbanCache.put(guild.getId(), config);
            return config;
        }
        return gbanned;
    }

    @Subscribe
    public void onGuildJoin(GuildJoinEvent guildJoinEvent) {
        Guild guild = guildJoinEvent.getGuild();
        GbanData data = getGbanData(guild);
        if (data.isGbanned()) {
            User issuer = shardManager.retrieveUserById(data.getIssuerId()).complete();
            Optional<TextChannel> kanal = guild.getTextChannels().stream().filter(TextChannel::canTalk).findFirst();
            kanal.ifPresent(tc -> {
                if (issuer != null) {
                    tc.sendMessage("Ten serwer jest globalnie zbanowany.\nOsoba banująca: `"
                            + (UserUtil.formatDiscrim(issuer).equals(data.getIssuer()) ?
                            UserUtil.formatDiscrim(issuer) :
                            UserUtil.formatDiscrim(issuer) + "` (`" + data.getIssuer() +
                                    "` w czasie gbana)`") +
                            "`.\nPowód: `" + data.getReason() + "`.\nZmywam się stąd, sajonara!`").complete();
                } else {
                    tc.sendMessage("Ten serwer jest globalnie zbanowany.\nOsoba banująca: `" + data.getIssuer() +
                            "`.\nPowód: `" + data.getReason() + "`.\nZmywam się stąd, sajonara!`").complete();
                }
            });
            guild.leave().queue();
        }
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent event) {
        if (event.getEntity() instanceof GuildConfig) {
            gbanCache.invalidate(((GuildConfig) event.getEntity()).getGuildId());
            timeZoneCache.invalidate(((GuildConfig) event.getEntity()).getGuildId());
        }
    }

    public static String getManageLink(Guild g) {
        return Ustawienia.instance.botUrl + "/dashboard/" + g.getId() + "/manage";
    }

}
