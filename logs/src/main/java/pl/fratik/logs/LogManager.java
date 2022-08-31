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

package pl.fratik.logs;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.entity.GbanData;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.event.CommandDispatchedEvent;
import pl.fratik.core.event.ConnectedEvent;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.event.LvlupEvent;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.NetworkUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static pl.fratik.core.Statyczne.CORE_VERSION;
import static pl.fratik.core.Statyczne.WERSJA;
import static pl.fratik.core.Ustawienia.instance;

class LogManager {

    private final ExecutorService executor;
    private final ShardManager shardManager;
    private final GuildDao guildDao;
    private final Tlumaczenia tlumaczenia;
    private boolean triggeredConnectEvent;
    private final Map<String, WebhookClient> webhooki;

    LogManager(ShardManager shardManager, GuildDao guildDao, Tlumaczenia tlumaczenia) {
        triggeredConnectEvent = false;
        this.shardManager = shardManager;
        this.guildDao = guildDao;
        this.tlumaczenia = tlumaczenia;
        executor = Executors.newFixedThreadPool(4);
        webhooki = new HashMap<>();
        if (shardManager.getShardsRunning() == shardManager.getShardsTotal()) onConnectEvent(new ConnectedEvent() {});
    }

    @Subscribe
    @AllowConcurrentEvents
    private void onCommandDispatched(CommandDispatchedEvent e) {
        String webhook = instance.logSettings.webhooki.commands;
        String webhookGa = instance.logSettings.webhooki.ga;
        String webhookGaLvl = instance.logSettings.webhooki.gaPermLvl;
        if (webhook == null) return;
        executor.submit(() -> {
            WebhookClient cl = getWebhook(webhook);
            if (e.getContext().getGuild() != null)
                cl.send(String.format("%s(%s) %s[%s] %s[%s]", e.getContext().getCommand().getName(),
                        e.getContext().getArguments().values().stream().map(OptionMapping::getAsString).collect(Collectors.joining(",")),
                        e.getContext().getSender().getName(), e.getContext().getSender().getId(),
                        e.getContext().getGuild().getName(), e.getContext().getGuild().getId()));
            else
                cl.send(String.format("%s(%s) %s[%s] Direct Messages", e.getContext().getCommand().getName(),
                        e.getContext().getArguments().values().stream().map(OptionMapping::getAsString).collect(Collectors.joining(",")),
                        e.getContext().getSender().getName(), e.getContext().getSender().getId()));
        });
    }

    private WebhookClient getWebhook(String webhook) {
        WebhookClient tak = webhooki.get(webhook);
        if (tak == null) {
            tak = new WebhookClientBuilder(webhook).setHttpClient(NetworkUtil.getClient()).build();
            webhooki.put(webhook, tak);
        }
        return tak;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onConnectEvent(ConnectedEvent e) {
        if (triggeredConnectEvent) return;
        triggeredConnectEvent = true;
        String webhook = instance.logSettings.webhooki.bot;
        if (webhook == null) return;
        executor.submit(() -> {
            WebhookClient cl = getWebhook(webhook);
            cl.send(String.format("Bot został połączony (lub moduł został zrestartowany, patrz czy jest " +
                    "komunikat o rozłączeniu)! Wersja bota: %s; wersja rdzenia: %s", WERSJA, CORE_VERSION));
        });
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGuildJoin(GuildJoinEvent e) {
        String webhook = instance.logSettings.webhooki.bot;
        if (webhook == null) return;
        executor.submit(() -> {
            WebhookClient cl = getWebhook(webhook);
            cl.send(String.format("Dołączono na %s (ID: %s)", e.getGuild().getName(), e.getGuild().getId()));
        });
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGuildLeave(GuildLeaveEvent e) {
        String webhook = instance.logSettings.webhooki.bot;
        if (webhook == null) return;
        executor.submit(() -> {
            WebhookClient cl = getWebhook(webhook);
            Member owner = e.getGuild().getOwner();
            if (owner == null) cl.send(String.format("Opuszczono %s (ID: %s, właściciel [%s])", e.getGuild().getName(),
                    e.getGuild().getId(), e.getGuild().getOwnerId()));
            else cl.send(String.format("Opuszczono %s (ID: %s, właściciel(ka): %s[%s])", e.getGuild().getName(),
                    e.getGuild().getId(), owner.getUser().getAsTag(), e.getGuild().getOwnerId()));
        });
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onLevelUp(LvlupEvent e) {
        String webhook = instance.logSettings.webhooki.lvlup;
        if (webhook == null) return;
        executor.submit(() -> {
            WebhookClient cl = getWebhook(webhook);
            cl.send(String.format("%s[%s] właśnie osiągnął/ęła poziom %s na serwerze %s[%s]!",
                    e.getMember().getUser().getAsTag(), e.getMember().getUser().getId(), e.getLevel(),
                    e.getMember().getGuild().getName(), e.getMember().getGuild().getId()));
        });
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGban(DatabaseUpdateEvent e) {
        if (!(e.getEntity() instanceof GbanData)) return;
        String webhook = instance.logSettings.webhooki.gban;
        if (webhook == null) return;
        executor.submit(() -> {
            GbanData entity = (GbanData) e.getEntity();
            WebhookClient cl = getWebhook(webhook);
            cl.send(String.format("@everyone (wyłącz pingi w razie bólu dupy) %s[%s] właśnie zgbanował(a) %s[%s].",
                    entity.getIssuer(), entity.getIssuerId(), entity.getId(), entity.getName()));
        });
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGenericLogEvent(GenericLogEvent e) {
        String webhook = instance.logSettings.webhooki.get(e.getChannel());
        if (webhook == null) return;
        executor.submit(() -> {
            WebhookClient cl = getWebhook(webhook);
            cl.send(e.getMessage());
        });
    }

    void shutdown() {
        String webhook = instance.logSettings.webhooki.bot;
        if (webhook == null) return;
        executor.submit(() -> {
            WebhookClient cl = getWebhook(webhook);
            cl.send("Bot się wyłącza!");
            for (Map.Entry<String, WebhookClient> hook : webhooki.entrySet()) {
                hook.getValue().close();
                webhooki.remove(hook.getKey());
            }
        });
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
