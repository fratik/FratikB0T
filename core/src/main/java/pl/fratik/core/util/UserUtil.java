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

package pl.fratik.core.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.Subscribe;
import lombok.Setter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.DatabaseUpdateEvent;

import java.awt.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class UserUtil {

    private static final Cache<User, GbanData> gbanCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();
    private static final Cache<User, TimeZone> timeZoneCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(100).build();

    @Setter private static GbanDao gbanDao;

    public static String formatDiscrim(User user) {
        return user.getName() + "#" + user.getDiscriminator();
    }

    public static String formatDiscrim(Member member) {
        return formatDiscrim(member.getUser());
    }

    public static GbanData getGbanData(User user) {
        GbanData gbanned = gbanCache.getIfPresent(user);
        if (gbanned == null) {
            GbanData config = gbanDao.get(user);
            gbanCache.put(user, config);
            return config;
        }
        return gbanned;
    }

    public static boolean isGbanned(User user) {
        GbanData gbanData = getGbanData(user);
        return gbanData.isGbanned();
    }

    public static PermLevel getPermlevel(User user, ShardManager shardManager) {
        return getPermlevel(user, shardManager, PermLevel.BOTOWNER);
    }

    public static PermLevel getPermlevel(User user, ShardManager shardManager, PermLevel max) {
        if (max.getNum() >= 10 && Globals.ownerId == user.getIdLong())
            return PermLevel.BOTOWNER;
        if (max.getNum() >= 6 && isZga(user, shardManager))
            return PermLevel.ZGA;
        if (max.getNum() >= 5 && isGadm(user, shardManager))
            return PermLevel.GADMIN;
        return PermLevel.EVERYONE;
    }

    public static PermLevel getPermlevel(Member member, GuildDao guildDao, ShardManager shardManager) {
        return getPermlevel(member, guildDao, shardManager, PermLevel.BOTOWNER);
    }

    public static PermLevel getPermlevel(Member member, GuildDao guildDao, ShardManager shardManager, PermLevel max) {
        if (max.getNum() >= 10 && Globals.ownerId == member.getUser().getIdLong())
            return PermLevel.BOTOWNER;
        if (max.getNum() >= 6 && isZga(member, shardManager))
            return PermLevel.ZGA;
        if (max.getNum() >= 5 && isGadm(member, shardManager))
            return PermLevel.GADMIN;
        if (max.getNum() >= 4 && member.isOwner())
            return PermLevel.OWNER;
        if (max.getNum() >= 3 && member.getPermissions().contains(Permission.MANAGE_SERVER))
            return PermLevel.MANAGESERVERPERMS;
        GuildConfig gc = guildDao.get(member.getGuild());
        if (max.getNum() >= 2 && gc.getAdminRole() != null && gc.getAdminRole().length() != 0 &&
                member.getRoles().stream().map(ISnowflake::getId).anyMatch(id -> gc.getAdminRole().equals(id)))
            return PermLevel.ADMIN;
        if (max.getNum() >= 1 && gc.getModRole() != null && gc.getModRole().length() != 0 &&
                member.getRoles().stream().map(ISnowflake::getId).anyMatch(id -> gc.getModRole().equals(id)))
            return PermLevel.MOD;
        return PermLevel.EVERYONE;
    }

    public static PermLevel getPermlevel(Member member, GuildDao guildDao, ShardManager shardManager, int max) {
        PermLevel permLevel = PermLevel.getPermLevel(max);
        if (permLevel == null) throw new IllegalArgumentException("Nieprawidłowa liczba!");
        return getPermlevel(member, guildDao, shardManager, permLevel);
    }

    private static boolean isZga(Member member, ShardManager shardManager) {
        return isZga(member.getUser(), shardManager);
    }

    private static boolean isZga(User user, ShardManager shardManager) {
        Ustawienia s = Ustawienia.instance;
        if (!Globals.inFratikDev) return false;
        Guild g = shardManager.getGuildById(s.botGuild);
        if (g == null) return false;
        return g.getMembersWithRoles(g.getRoleById(s.zgaRole))
                .stream().map(Member::getUser).anyMatch(user::equals);
    }

    public static boolean isGadm(Member member, ShardManager shardManager) {
        return isGadm(member.getUser(), shardManager);
    }

    public static boolean isGadm(User user, ShardManager shardManager) {
        Ustawienia s = Ustawienia.instance;
        if (!Globals.inFratikDev) return false;
        Guild g = shardManager.getGuildById(s.botGuild);
        if (g == null) return false;
        return g.getMembersWithRoles(g.getRoleById(s.gadmRole))
                .stream().map(Member::getUser).anyMatch(user::equals);
    }

    public static boolean isStaff(Member member, ShardManager shardManager) {
        return isStaff(member.getUser(), shardManager);
    }

    public static boolean isStaff(User user, ShardManager shardManager) {
        return isGadm(user, shardManager) || isZga(user, shardManager) || Globals.ownerId == user.getIdLong();
    }

    public static String getPoPrzecinku(User... users) {
        ArrayList<String> tagi = new ArrayList<>();
        for (User user : users)
            tagi.add(formatDiscrim(user));
        return String.join(", ", tagi);
    }

    public static String getPoPrzecinku(Collection<User> userCollection) {
        ArrayList<String> tagi = new ArrayList<>();
        for (User user : userCollection)
            tagi.add(formatDiscrim(user));
        return String.join(", ", tagi);
    }

    public static Color getPrimColor(User user) {
        try {
            JSONObject zdjecie = NetworkUtil.getJson(Ustawienia.instance.apiUrls.get("image-server") +
                            "/api/image/primColor?imageURL=" + URLEncoder.encode(user.getEffectiveAvatarUrl()
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
            LoggerFactory.getLogger(UserUtil.class).error("Błąd w uzyskiwaniu koloru!", e);
            return new Color(114, 137, 218);
        }
    }

    public static TimeZone getTimeZone(User user, UserDao userDao) {
        return timeZoneCache.get(user, u -> {
            UserConfig uc = userDao.get(u);
            if (uc.getTimezone().equals("default")) return TimeZone.getDefault();
            return TimeZone.getTimeZone(uc.getTimezone());
        });
    }

    public static String getAvatarUrl(User user) {
        return user.getEffectiveAvatarUrl().replace(".webp", ".png");
    }

    @Subscribe
    public void onDatabaseUpdate(DatabaseUpdateEvent event) {
        if (event.getEntity() instanceof UserConfig) {
            for (User user : gbanCache.asMap().keySet()) {
                if (((UserConfig) event.getEntity()).getId().equals(user.getId())) {
                    gbanCache.invalidate(user);
                    return;
                }
            }
            for (User user : timeZoneCache.asMap().keySet()) {
                if (((UserConfig) event.getEntity()).getId().equals(user.getId())) {
                    timeZoneCache.invalidate(user);
                    return;
                }
            }
        }
    }
}
