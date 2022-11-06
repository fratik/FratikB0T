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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.jetbrains.annotations.NotNull;
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

    public void send(String cnt, @NotNull GuildChannel channel) {
        SelfUser su = channel.getJDA().getSelfUser();
        send(new WebhookMessageBuilder().setContent(cnt).setAvatarUrl(su.getAvatarUrl())
                .setUsername(su.getName()).build(), channel);
    }

    public ReadonlyMessage send(WebhookMessage m, GuildChannel channel) {
        long threadId;
        if (channel instanceof ThreadChannel) threadId = channel.getIdLong();
        else threadId = 0;
        GuildConfig.Webhook whc = getWebhook(channel);
        try (WebhookClient wh = new WebhookClientBuilder(Long.parseLong(whc.getId()), whc.getToken()).setThreadId(threadId).setWait(true).build()) {
            return wh.send(m).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof HttpException && ((HttpException) e.getCause()).getCode() == 404) {
                try {
                    if (channel instanceof TextChannel) createWebhook((TextChannel) channel, true);
                    else if (channel instanceof ThreadChannel)
                        createWebhook((TextChannel) ((ThreadChannel) channel).getParentChannel(), true);
                    return send(m, channel);
                } catch (Exception ignored) {
                    return null;
                }
            }
            throw new RuntimeException(e);
        }
    }

    public GuildConfig.Webhook getWebhook(GuildChannel channel) {
        return whCache.get(resolveId(channel), id -> {
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
                    if (channel instanceof TextChannel) tak = createWebhook((TextChannel) channel, false);
                    else if (channel instanceof ThreadChannel)
                        tak = createWebhook((TextChannel) ((ThreadChannel) channel).getParentChannel(), false);
                } catch (PermissionException ignored) {}
            }
            return tak;
        });
    }

    private String resolveId(GuildChannel channel) {
        if (channel instanceof ThreadChannel) return ((ThreadChannel) channel).getParentChannel().getId();
        else return channel.getId();
    }

    public boolean hasWebhook(GuildChannel channel) {
        GuildConfig.Webhook wh = whCache.getIfPresent(resolveId(channel));
        if (wh == null) {
            Map<String, GuildConfig.Webhook> whki = guildDao.get(channel.getGuild()).getWebhooki();
            if (whki != null) return whki.containsKey(resolveId(channel));
            else return false;
        }
        return true;
    }

    private GuildConfig.Webhook createWebhook(TextChannel channel, boolean clearCache) {
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
            if (hasWebhook(channel)) {
                GuildConfig gc = guildDao.get(channel.getGuild());
                gc.getWebhooki().remove(channel.getId());
                if (clearCache) whCache.invalidate(channel.getId());
                guildDao.save(gc);
            }
            throw new PermissionException("Nie ma perma MANAGE_WEBHOOKS!");
        }
        GuildConfig gc = guildDao.get(channel.getGuild());
        Webhook tak = channel.createWebhook("FratikB0T Messages " + channel.getId()).complete();
        GuildConfig.Webhook whc = new GuildConfig.Webhook(tak.getId(), tak.getToken());
        gc.getWebhooki().put(channel.getId(), whc);
        guildDao.save(gc);
        if (clearCache) whCache.invalidate(channel.getId());
        return whc;
    }
}
