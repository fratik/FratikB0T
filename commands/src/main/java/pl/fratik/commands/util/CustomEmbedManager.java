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

package pl.fratik.commands.util;

import com.google.gson.JsonObject;
import io.undertow.server.RoutingHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pl.fratik.api.Module;
import pl.fratik.api.entity.Exceptions;
import pl.fratik.api.internale.Exchange;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.core.util.UserUtil;

import java.util.Random;

public class CustomEmbedManager {

    private final static Random RADOM = new Random();

    private final Cache<EmbedBuilder> embeds;

    public CustomEmbedManager(RedisCacheManager rcm, Modul apiModule, ShardManager shardManager) {
        embeds = rcm.new CacheRetriever<EmbedBuilder>(){}.getCache();

        RoutingHandler routes;
        try {
            routes = (RoutingHandler) apiModule.getClass().getDeclaredMethod("getRoutes").invoke(apiModule);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Nie udało się doczepić do modułu api!", e);
            return;
        }

        routes.post("/api/embed/create", ex -> {
            JsonObject json = Module.getJson(ex);
            String requester = Exchange.headers().getHeader(ex, "Requester-ID").orElse(null);
            if (json == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_BODY);
                return;
            }
            if (requester == null) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.NO_REQUESTER_ID);
                return;
            }

            try {
                User user = shardManager.retrieveUserById(requester).complete();
                EmbedBuilder eb = GsonUtil.fromJSON(json.toString(), EmbedBuilder.class);
                eb.setFooter(user.getAsTag(), user.getEffectiveAvatarUrl());
                int code = RADOM.nextInt(1_000_000);
                embeds.put(String.valueOf(code), eb);
                Exchange.body().sendJson(ex, new JSONObject().put("success", true).put("code", code).toString());
            } catch (Exception e) {
                Exchange.body().sendErrorCode(ex, Exceptions.Codes.UNKNOWN_ERROR);
            }

        });

    }

    @Nullable
    public EmbedBuilder getEmbed(String code) {
        return embeds.getIfPresent(code);
    }

}
