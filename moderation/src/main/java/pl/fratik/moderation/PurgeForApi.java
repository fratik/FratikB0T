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

package pl.fratik.moderation;

import com.fasterxml.jackson.core.type.TypeReference;
import io.undertow.server.RoutingHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.LoggerFactory;
import pl.fratik.api.entity.Exceptions;
import pl.fratik.api.entity.Successes;
import pl.fratik.api.entity.User;
import pl.fratik.api.internale.Exchange;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Purge;
import pl.fratik.moderation.entity.PurgeDao;
import pl.fratik.moderation.entity.PurgePrivacy;
import pl.fratik.moderation.entity.Wiadomosc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class PurgeForApi {
    PurgeForApi(Modul modul, ShardManager shardManager, PurgeDao purgeDao, GuildDao guildDao) {
        RoutingHandler routes;
        try {
            routes = (RoutingHandler) modul.getClass().getDeclaredMethod("getRoutes").invoke(modul);
        } catch (Exception e) {
            LoggerFactory.getLogger(PurgeForApi.class).error("Nie udało się doczepić do modułu api!", e);
            return;
        }
        routes.get("/api/purge/{purgeId}", ex -> {
            Purge purge = purgeDao.get(Exchange.pathParams().pathParam(ex, "purgeId").orElse(""));
            if (purge == null) {
                Exchange.body().sendJson(ex, new Exceptions.GenericException("Nie znaleziono purge z tym ID"), 404);
                return;
            }
            if (purge.getPrivacy() == PurgePrivacy.PERMLEVEL) {
                String requester = Exchange.headers().getHeader(ex, "Requester-ID").orElse(null);
                if (requester == null) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("Brak Requester-ID dla prywatnego purge"), 401);
                    return;
                }
                Member member;
                try {
                    Guild g = shardManager.getGuildById(purge.getGuildId());
                    if (g == null) {
                        Exchange.body().sendJson(ex, new Exceptions.NoGuild(), 400);
                        return;
                    }
                    member = g.getMember(shardManager.retrieveUserById(requester).complete());
                } catch (Exception e) {
                    Exchange.body().sendJson(ex, new Exceptions.NoUser(), 400);
                    return;
                }
                if (UserUtil.getPermlevel(member, guildDao, shardManager).getNum() < purge.getMinPermLevel()) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("Brak uprawnień"), 403);
                    return;
                }
            }
            try { //próbujemy uaktualnić obiekty użytkowników, jeżeli się nie powiedzie to trudno
                if (purge.getPurgedBy() != null) {
                    net.dv8tion.jda.api.entities.User xd = shardManager.getUserById(purge.getPurgedBy().getId());
                    if (xd != null) {
                        purge.setPurgedBy(new User(xd));
                    }
                }
                for (Wiadomosc w : purge.getWiadomosci()) {
                    if (w instanceof Purge.ResolvedWiadomosc) {
                        net.dv8tion.jda.api.entities.User xd = shardManager.getUserById(w.getAuthor().getId());
                        if (xd != null) {
                            ((Purge.ResolvedWiadomosc) w).setAuthor(new User(xd));
                        }
                    }
                }
            } catch (Exception e) {
                // nic nie robimy
            }
            Exchange.body().sendJson(ex, purge);
        });
        routes.delete("/api/purge/{purgeId}", ex -> {
            Purge purge = purgeDao.get(Exchange.pathParams().pathParam(ex, "purgeId").orElse(""));
            if (purge == null) {
                Exchange.body().sendJson(ex, new Exceptions.GenericException("Nie znaleziono purge z tym ID"), 404);
                return;
            }
            String requester = Exchange.headers().getHeader(ex, "Requester-ID").orElse(null);
            if (requester == null) {
                Exchange.body().sendJson(ex, new Exceptions.GenericException("Brak Requester-ID"), 401);
                return;
            }
            Member member;
            try {
                Guild g = shardManager.getGuildById(purge.getGuildId());
                if (g == null) {
                    Exchange.body().sendJson(ex, new Exceptions.NoGuild(), 400);
                    return;
                }
                member = g.getMember(shardManager.retrieveUserById(requester).complete());
            } catch (Exception e) {
                Exchange.body().sendJson(ex, new Exceptions.NoUser(), 400);
                return;
            }
            if (UserUtil.getPermlevel(member, guildDao, shardManager).getNum() < PermLevel.ADMIN.getNum()) {
                Exchange.body().sendJson(ex, new Exceptions.GenericException("Brak uprawnień"), 403);
                return;
            }
            purge.setDeleted(true);
            purge.setDeletedBy(new User(shardManager.retrieveUserById(requester).complete()));
            purge.setDeletedOn(Instant.now().toEpochMilli());
            purge.setWiadomosci(new ArrayList<>());
            purgeDao.save(purge);
            Exchange.body().sendJson(ex, new Successes.GenericSuccess());
        });
        routes.add("PATCH", "/api/purge/{purgeId}", ex -> {
            Purge purge = purgeDao.get(Exchange.pathParams().pathParam(ex, "purgeId").orElse(""));
            Purge modifiedPurge = Exchange.body().parseJson(ex, new TypeReference<Purge>() {});
            if (modifiedPurge.getPrivacy() == PurgePrivacy.PUBLIC && purge.getPrivacy() != PurgePrivacy.PUBLIC) {
                purge.setPrivacy(PurgePrivacy.PUBLIC);
                purge.setMinPermLevel(0);
                purgeDao.save(purge);
                Exchange.body().sendJson(ex, new Successes.GenericSuccess("Pomyślnie ustawiono prywatność na publiczną."));
                return;
            }
            if (modifiedPurge.getPrivacy() == PurgePrivacy.PERMLEVEL && purge.getPrivacy() != PurgePrivacy.PERMLEVEL) {
                purge.setPrivacy(PurgePrivacy.PERMLEVEL);
                try {
                    purge.setMinPermLevel(PermLevel.getPermLevel(modifiedPurge.getMinPermLevel()).getNum());
                } catch (Exception e) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("Nieprawidłowy permlevel"), 400);
                }
                purgeDao.save(purge);
                Exchange.body().sendJson(ex, new Successes.GenericSuccess(String.format("Pomyślnie ustawiono prywatność na perm level %s.", modifiedPurge.getMinPermLevel())));
                return;
            }
            if (modifiedPurge.getPrivacy() == PurgePrivacy.PERMLEVEL && purge.getPrivacy() == PurgePrivacy.PERMLEVEL &&
                    purge.getMinPermLevel() != modifiedPurge.getMinPermLevel()) {
                try {
                    purge.setMinPermLevel(PermLevel.getPermLevel(modifiedPurge.getMinPermLevel()).getNum());
                } catch (Exception e) {
                    Exchange.body().sendJson(ex, new Exceptions.GenericException("Nieprawidłowy permlevel"), 400);
                }
                purgeDao.save(purge);
                Exchange.body().sendJson(ex, new Successes.GenericSuccess(String.format("Pomyślnie ustawiono prywatność na perm level %s.", modifiedPurge.getMinPermLevel())));
                return;
            }
            Exchange.body().sendJson(ex, new Exceptions.GenericException("Nie wykryto dozwolonych zmian"), 400);
        });
        routes.get("/api/purge/list", ex -> {
            String gId = Exchange.queryParams().queryParam(ex, "guildId").orElse("");
            List<Purge> purges = purgeDao.getByGuild(gId);
            if (purges.isEmpty()) {
                Exchange.body().sendJson(ex, "[]");
                return;
            }
            String requester = Exchange.headers().getHeader(ex, "Requester-ID").orElse(null);
            if (requester == null) {
                Exchange.body().sendJson(ex, new Exceptions.GenericException("Brak Requester-ID"), 401);
                return;
            }
            Member member;
            try {
                Guild g = shardManager.getGuildById(gId);
                if (g == null) {
                    Exchange.body().sendJson(ex, new Exceptions.NoGuild(), 400);
                    return;
                }
                member = g.getMember(shardManager.retrieveUserById(requester).complete());
            } catch (Exception e) {
                Exchange.body().sendJson(ex, new Exceptions.NoUser(), 400);
                return;
            }
            if (UserUtil.getPermlevel(member, guildDao, shardManager).getNum() < PermLevel.MOD.getNum()) {
                Exchange.body().sendJson(ex, new Exceptions.GenericException("Brak uprawnień"), 403);
                return;
            }
            try { //próbujemy uaktualnić obiekty użytkowników, jeżeli się nie powiedzie to trudno
                for (Purge purge : purges) {
                    if (purge.getPurgedBy() != null) {
                        net.dv8tion.jda.api.entities.User xd = shardManager.getUserById(purge.getPurgedBy().getId());
                        if (xd != null) {
                            purge.setPurgedBy(new User(xd));
                        }
                    }
                }
            } catch (Exception e) {
                // nic nie robimy
            }
            Exchange.body().sendJson(ex, purges);
        });
    }
}
