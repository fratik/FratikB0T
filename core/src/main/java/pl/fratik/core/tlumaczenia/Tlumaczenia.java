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

package pl.fratik.core.tlumaczenia;

import com.google.common.base.Charsets;
import com.google.common.eventbus.Subscribe;
import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.event.DatabaseUpdateEvent;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public class Tlumaczenia {
    private final UserDao userDao;
    private final GuildDao guildDao;
    private final Logger logger;
    @Getter private Map<Language, Properties> languages;
    private final Cache<Language> languageCache;
    private static final String NOTTRA = " nie jest przetłumaczone!";
    @Getter @Setter private static ShardManager shardManager;

    public Tlumaczenia(UserDao userDao, GuildDao guildDao, RedisCacheManager redisCacheManager) {
        this.userDao = userDao;
        this.guildDao = guildDao;
        logger = LoggerFactory.getLogger(getClass());
        languageCache = redisCacheManager.new CacheRetriever<Language>(){}.getCache();
    }

    public void loadMessages() {
        logger.debug("Ładowanie tłumaczeń...");
        languages = new EnumMap<>(Language.class);

        for (Language l : Language.values()) {
            if (l != Language.DEFAULT) try {
                Properties p = new Properties();
                File f = new File("./" + l.getShortName() + "/messages.properties");
                URL url;
                if (f.exists()) {
                    url = f.toURI().toURL();
                    logger.debug("Załadowano język {} z pliku: {}", l.getShortName(), url);
                } else {
                    url = getClass().getResource("/" + l.getShortName() + "/messages.properties");
                    if (url == null) {
                        logger.warn("Plik z stringami dla języka {} nie istnieje.", l.name());
                        continue;
                    }
                    logger.debug("Załadowano język {} z pliku: {}", l.getLocalized(), url);
                }

                p.load(new InputStreamReader(url.openStream(), Charsets.UTF_8));
                languages.put(l, p);
            } catch (Exception e) {
                logger.error("Nie udało się załadować języka!", e);
            }
        }
    }

    public String get(Language l, String key) {
        return get(l, key, false);
    }

    public String get(Language l, String key, boolean disableSentry) {
        String property;
        if (languages.containsKey(l)) {
            property = languages.get(l).getProperty(key);
            if (property == null) {
                property = languages.get(Language.POLISH).getProperty(key, key);
                if (property.equals(languages.get(Language.POLISH)
                        .getProperty("translation.empty", "translation.empty")))
                    property = "";
            }
        } else {
            property = languages.get(Language.POLISH).getProperty(key, key);
            if (property.equals(languages.get(Language.POLISH)
                    .getProperty("translation.empty", "translation.empty"))) property = "";
        }
        if (property.equals(key) && !disableSentry)
            Sentry.capture(new EventBuilder().withLevel(Event.Level.WARNING).withMessage(key + NOTTRA).build());
        if (property.equals(languages.getOrDefault(l, languages.get(Language.POLISH))
                .getProperty("translation.empty", languages.get(Language.POLISH)
                .getProperty("translation.empty", "translation.empty"))) ||
                property.equals(languages.get(Language.POLISH)
                        .getProperty("translation.empty", "translation.empty"))) {
            property = "";
        }
        return property;
    }

    public String get(Language l, String key, String ...toReplace) {
        return String.format(get(l, key), (Object[]) toReplace);
    }

    public String get(Language l, String key, Object ...toReplace) {
        ArrayList<String> parsedArray = new ArrayList<>();
        for (Object k : toReplace) parsedArray.add(k.toString());
        return String.format(get(l, key), parsedArray.toArray());
    }

    public Language getLanguage(Guild guild) {
        try {
            Language l = languageCache.get(guild.getId(),
                    gid -> guildDao.get(guild).getLanguage());

            return l == Language.DEFAULT ? Language.POLISH : l;
        } catch (Exception e) {
            logger.error("Error while getting guild language!", e);
            return Language.POLISH;
        }
    }

    public Language getLanguage(Member member) {
        try {
            Language l = languageCache.get(member.getUser().getId(),
                    uid -> userDao.get(member.getUser()).getLanguage());

            return l == Language.DEFAULT ? getLanguage(member.getGuild()) : l;
        } catch (Exception e) {
            logger.error("Error while getting user language!", e);
            return Language.POLISH;
        }
    }

    public Language getLanguage(User user) {
        try {
            Language l = languageCache.get(user.getId(),
                    uid -> userDao.get(user).getLanguage());

            return l == Language.DEFAULT ? Language.POLISH : l;
        } catch (Exception e) {
            logger.error("Error while getting user language!", e);
            return Language.POLISH;
        }
    }

    @Subscribe
    public void onUpdate(DatabaseUpdateEvent event) {
        if (event.getEntity() instanceof UserConfig) {
            languageCache.invalidate(((UserConfig) event.getEntity()).getId());
        } else if (event.getEntity() instanceof GuildConfig) {
            languageCache.invalidate(((GuildConfig) event.getEntity()).getGuildId());
        }
    }
}
