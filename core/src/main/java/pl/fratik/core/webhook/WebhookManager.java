/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.core.webhook;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.exception.HttpException;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.exceptions.PermissionException;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WebhookManager {

    private final GuildDao guildDao;
    private final Cache<String, GuildConfig.Webhook> whCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).build();

    public WebhookManager(GuildDao guildDao) {
        this.guildDao = guildDao;
    }

    public void send(String cnt, TextChannel channel) {
        SelfUser su = channel.getJDA().getSelfUser();
        send(new WebhookMessageBuilder().setContent(cnt).setAvatarUrl(su.getAvatarUrl())
                .setUsername(su.getName()).build(), channel);
    }

    public ReadonlyMessage send(WebhookMessage m, TextChannel channel) {
        GuildConfig.Webhook whc = getWebhook(channel);
        try (WebhookClient wh = new WebhookClientBuilder(Long.parseLong(whc.getId()), whc.getToken()).setWait(true).build()) {
            ReadonlyMessage msg = wh.send(m).get();
//            Thread.sleep(1000);
            return msg;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof HttpException) {
                if (((HttpException) e.getCause()).getCode() == 404) {
                    try {
                        createWebhook(channel, true);
                        send(m, channel);
                        return null;
                    } catch (Exception ignored) {}
                }
            }
            throw new RuntimeException(e);
        }
    }

    public GuildConfig.Webhook getWebhook(TextChannel channel) {
        return whCache.get(channel.getId(), id -> {
            Map<String, GuildConfig.Webhook> whki = guildDao.get(channel.getGuild()).getWebhooki();
            if (whki == null) {
                GuildConfig gc = guildDao.get(channel.getGuild());
                gc.setWebhooki(new HashMap<>());
                guildDao.save(gc);
                whki = gc.getWebhooki();
            }
            GuildConfig.Webhook tak = whki.get(id);
            if (tak == null) {
                try {
                    tak = createWebhook(channel, false);
                } catch (PermissionException ignored) {}
            }
            return tak;
        });
    }

    public boolean hasWebhook(TextChannel channel) {
        GuildConfig.Webhook wh = whCache.getIfPresent(channel.getId());
        if (wh == null) {
            Map<String, GuildConfig.Webhook> whki = guildDao.get(channel.getGuild()).getWebhooki();
            if (whki != null) return whki.containsKey(channel.getId());
            else return false;
        }
        return true;
    }

    private GuildConfig.Webhook createWebhook(TextChannel channel, boolean clearCache) {
        if (!channel.getGuild().getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS))
            throw new PermissionException("Nie ma perma MANAGE_WEBHOOKS!");
        Webhook tak = channel.createWebhook("FratikB0T Messages " + channel.getId()).complete();
        GuildConfig.Webhook whc = new GuildConfig.Webhook(tak.getId(), tak.getToken());
        GuildConfig gc = guildDao.get(channel.getGuild());
        gc.getWebhooki().put(channel.getId(), whc);
        guildDao.save(gc);
        if (clearCache) whCache.invalidate(channel.getId());
        return whc;
    }
}
