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

package pl.fratik.core;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.util.concurrent.ServiceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import io.sentry.Sentry;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.dv8tion.jda.internal.JDAImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.ConnectedEvent;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.manager.implementation.ManagerArgumentowImpl;
import pl.fratik.core.manager.implementation.ManagerBazyDanychImpl;
import pl.fratik.core.manager.implementation.ManagerModulowImpl;
import pl.fratik.core.manager.implementation.NewManagerKomendImpl;
import pl.fratik.core.service.FratikB0TService;
import pl.fratik.core.service.ScheduleService;
import pl.fratik.core.service.StatusService;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.*;
import pl.fratik.core.webhook.WebhookManager;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static pl.fratik.core.Statyczne.WERSJA;

class FratikB0T {

    private Ustawienia ustawienia;
    private final Logger logger;
    private ManagerModulow moduleManager;
    private ManagerBazyDanych mbd;
    private NewManagerKomendImpl managerKomend;
    private ServiceManager glownyService;
    private ServiceManager statusService;
    private ServiceManager scheduleService;
    private EventWaiter eventWaiter;

    private static final File cfg = new File("config.json");
    private static boolean shutdownThreadRegistered = false;
    @Getter private static Thread shutdownThread;

    FratikB0T(String token) {
        logger = LoggerFactory.getLogger(FratikB0T.class);
        AsyncEventBus eventBus = new AsyncEventBus(Executors.newFixedThreadPool(16), EventBusErrorHandler.instance) {
            @Override
            public void post(Object event) {
                super.post(event);
            }
        };

        logger.info("Ładuje jądro v{}...", Statyczne.CORE_VERSION);
        registerShutdownThread();
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        if (!cfg.exists()) {
            try {
                if (cfg.createNewFile()) {
                    ustawienia = new Ustawienia();
                    Files.write(cfg.toPath(), gson.toJson(ustawienia).getBytes(StandardCharsets.UTF_8));
                    logger.info("Konfiguracja stworzona, ustaw bota!");
                    System.exit(1);
                }
            } catch (Exception e) {
                logger.error("Nie udało się stworzyć konfiguracji!", e);
                System.exit(1);
            }
        }

        try {
            ustawienia = gson.fromJson(new FileReader(cfg), Ustawienia.class);
        } catch (Exception e) {
            logger.error("Nie udało się odczytać konfiguracji!", e);
            System.exit(1);
        }

        Ustawienia.instance = ustawienia;

        if (ustawienia.apiKeys.containsKey("sentry-dsn")) {
            logger.info("Włączam wsparcie Sentry...");
            Sentry.init(ustawienia.apiKeys.get("sentry-dsn")).setRelease("FratikB0T@" + WERSJA);
        }

        logger.info("Loguje się...");
        try {
            JDAEventHandler eventHandler = new JDAEventHandler(eventBus);

            String[] shards = ustawienia.shard.shardString.split(":");
            if (shards.length != 3) {
                logger.error("Nieprawidłowy shard string, prawidłowy format to \"<min shard>:<max shard>:<ilość shard'ów>\"");
                System.exit(1);
            } else {
                int min = Integer.parseUnsignedInt(shards[0]);
                int max = Integer.parseUnsignedInt(shards[1]);
                int count = Integer.parseUnsignedInt(shards[2]);

                logger.info("Oczekiwanie na shard'y...");
                DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(token,
                        GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_INVITES,
                        GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                        GatewayIntent.MESSAGE_CONTENT);
                builder.setShardsTotal(count);
                builder.setShards(min, max);
                builder.setEnableShutdownHook(false);
                builder.setAudioSendFactory(new NativeAudioSendFactory());
                builder.setAutoReconnect(true);
                try {
                    builder.setChunkingFilter(ChunkingFilter.include(Long.parseLong(Ustawienia.instance.botGuild)));
                } catch (NumberFormatException e) {
                    logger.error("Nieprawidłowe ID serwera bota!");
                    System.exit(1);
                }
                builder.setMemberCachePolicy(MemberCachePolicy.ALL);
                builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
                builder.setActivity(Activity.playing(String.format("Ładowanie... (%s shard(ów))", count)));
                builder.addEventListeners(eventHandler);
                builder.setVoiceDispatchInterceptor(eventHandler);
                builder.setBulkDeleteSplittingEnabled(false);
                builder.setCallbackPool(Executors.newFixedThreadPool(4));
                builder.enableCache(CacheFlag.EMOJI);
                MessageRequest.setDefaultMentions(EnumSet.of(Message.MentionType.EMOJI, Message.MentionType.CHANNEL));
                MessageRequest.setDefaultMentionRepliedUser(false);
                ShardManager shardManager = builder.build();
                ManagerArgumentow managerArgumentow = new ManagerArgumentowImpl();
                mbd = new ManagerBazyDanychImpl();
                mbd.load();
                UserDao userDao = new UserDao(mbd, eventBus);
                MemberDao memberDao = new MemberDao(mbd, eventBus);
                GuildDao guildDao = new GuildDao(mbd, eventBus);
                GbanDao gbanDao = new GbanDao(mbd, eventBus);
                ScheduleDao scheduleDao = new ScheduleDao(mbd, eventBus);
                this.eventWaiter = new EventWaiter(Executors.newSingleThreadScheduledExecutor(), false);
                WebhookManager webhookManager = new WebhookManager(guildDao);
                while (shardManager.getShards().stream().noneMatch(s -> {
                    try {
                        s.getSelfUser();
                    } catch (IllegalStateException e) {
                        return false;
                    }
                    return true;
                })) {
                    Thread.sleep(100);
                }

                Optional<JDA> shard = shardManager.getShards().stream().filter(s -> ((JDAImpl) s).hasSelfUser()).findAny();

                if (shard.isEmpty()) {
                    logger.error("Nie znaleziono shard'a?");
                    System.exit(1);
                }

                Globals.clientId = shard.get().getSelfUser().getIdLong();

                if (shard.get().getSelfUser().getId().equals(Ustawienia.instance.productionBotId)) {
                    Globals.inDiscordBotsServer = Globals.production = true;
                }

                shard.get().retrieveApplicationInfo().queue(appInfo -> {
                    Globals.owner = appInfo.getOwner().getName() + "#" + appInfo.getOwner().getDiscriminator();
                    Globals.ownerId = appInfo.getOwner().getIdLong();
                });
                RedisCacheManager redisCacheManager = new RedisCacheManager(Globals.clientId);
                eventBus.register(redisCacheManager);
                UserUtil.setGcCache(redisCacheManager.new CacheRetriever<GuildConfig>(){}.getCache());
                UserUtil.setGbanCache(redisCacheManager.new CacheRetriever<GbanData>(){}.getCache());
                UserUtil.setTimeZoneCache(redisCacheManager.new CacheRetriever<String>("utz"){}.getCache());
                UserUtil.setColorCache(redisCacheManager.new CacheRetriever<Integer>(){}.getCache(900));
                GuildUtil.setGbanCache(redisCacheManager.new CacheRetriever<GbanData>(){}.getCache());
                GuildUtil.setTimeZoneCache(redisCacheManager.new CacheRetriever<String>("gtz"){}.getCache());
                NetworkUtil.setUpContentInformationCache(redisCacheManager);
                Tlumaczenia.setShardManager(shardManager);
                Tlumaczenia tlumaczenia = new Tlumaczenia(userDao, guildDao, redisCacheManager);
                managerKomend = new NewManagerKomendImpl(shardManager, tlumaczenia, eventBus);
                moduleManager = new ManagerModulowImpl(shardManager, mbd, guildDao, webhookManager, memberDao, userDao,
                        redisCacheManager, gbanDao, scheduleDao, managerKomend, managerArgumentow, eventWaiter,
                        tlumaczenia, eventBus);
                glownyService = new ServiceManager(List.of(new FratikB0TService(shardManager, eventBus,
                        eventWaiter, tlumaczenia, managerKomend, mbd, guildDao, moduleManager, gbanDao)));
                statusService = new ServiceManager(List.of(new StatusService(shardManager)));
                scheduleService = new ServiceManager(List.of(new ScheduleService(shardManager, scheduleDao,
                        tlumaczenia, eventBus)));


                glownyService.startAsync();
                glownyService.awaitHealthy();

                shardManager.setStatus(OnlineStatus.ONLINE);
                shardManager.setActivity(Activity.playing("Dzień doberek! | v" + WERSJA));
                statusService.startAsync();
                scheduleService.startAsync();

                logger.info("Połączono, zalogowano jako {}!", shard.get().getSelfUser());
                eventBus.post(new ConnectedEvent(){});
            }
        } catch (Exception e) {
            logger.error("Nie udało się wystartować FratikB0Ta", e);
            System.exit(2);
        }
    }

    private void registerShutdownThread() {
        if (shutdownThreadRegistered) return;
        shutdownThreadRegistered = true;
        shutdownThread = new Thread(() -> {
            Globals.wylaczanie = true;
            logger.info("Shutdown Hook uruchomiony!");
            StatusService.setCustomPresence(OnlineStatus.DO_NOT_DISTURB,
                    Activity.playing("Wyłączanie bota w toku..."));
            EmbedPaginator.shutdown();
            if (moduleManager != null) {
                logger.debug("Wyłączam moduły...");
                Iterator<String> iterator = moduleManager.getModules().keySet().iterator();
                while (iterator.hasNext()) {
                    String nazwa = iterator.next();
                    moduleManager.stopModule(nazwa);
                    moduleManager.unload(nazwa, false);
                    iterator.remove();
                }
            }
            if (managerKomend != null) {
                logger.debug("Opróżniam executor'a manager'u komend...");
                managerKomend.shutdown();
            }
            if (statusService != null) {
                logger.debug("Zatrzymuje serwis statusów...");
                statusService.stopAsync();
                try {
                    statusService.awaitStopped(1, TimeUnit.MINUTES);
                    logger.debug("Gotowe!");
                } catch (TimeoutException ignored) {/*lul*/}
            }
            if (scheduleService != null) {
                logger.debug("Zatrzymuje serwis schedule...");
                scheduleService.stopAsync();
                try {
                    scheduleService.awaitStopped(1, TimeUnit.MINUTES);
                    logger.debug("Gotowe!");
                } catch (TimeoutException ignored) {/*lul*/}
            }
            if (eventWaiter != null) eventWaiter.shutdown();
            if (mbd != null) {
                logger.debug("Zamykam bazę danych...");
                mbd.shutdown();
            }
            if (glownyService != null) {
                logger.debug("Zatrzymuje główny serwis i JDA...");
                glownyService.stopAsync();
                try {
                    glownyService.awaitStopped(1, TimeUnit.MINUTES);
                    logger.debug("Gotowe!");
                } catch (TimeoutException ignored) {/*lul*/}
            }
            logger.info("Pomyślnie pozamykano wszystko!");
        });
        shutdownThread.setName("FratikB0T Shutdown Hook");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }
}
