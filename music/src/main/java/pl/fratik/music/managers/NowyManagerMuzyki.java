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

package pl.fratik.music.managers;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.sentry.Sentry;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import lavalink.client.LavalinkUtil;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.Link;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.JDAEventHandler;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.music.entity.Queue;
import pl.fratik.music.entity.QueueDao;
import pl.fratik.music.lavalink.CustomLavalink;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class NowyManagerMuzyki {

    @Getter private final CustomLavalink lavaClient;
    private final EventBus eventBus;
    private final GuildDao guildDao;
    private static final Logger logger = LoggerFactory.getLogger(NowyManagerMuzyki.class);
    @Setter private static Tlumaczenia tlumaczenia;
    @Setter private static QueueDao queueDao;
    @NotNull private final ShardManager shardManager;
    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
    private final Map<String, ManagerMuzykiSerwera> kolejki = new HashMap<>();


    static {
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
    }

    public NowyManagerMuzyki(ShardManager shardManager, EventBus eventBus, GuildDao guildDao) {
        this.shardManager = shardManager;
        this.guildDao = guildDao;
        logger.info("Startuje Lavalink");
        lavaClient = new CustomLavalink(shardManager, Long.toString(Globals.clientId));
        eventBus.register(lavaClient);
        this.eventBus = eventBus;
        eventBus.register(this);

        for (Ustawienia.Lavalink.LavalinkNode nod : Ustawienia.instance.lavalink.nodes) {
            logger.info("Dodaje node {}", nod);
            lavaClient.addNode(URI.create("ws://" + nod.address + ":" + nod.wsPort), nod.password);
        }

        JDAEventHandler.setVdi(new Vdi(lavaClient.getVoiceInterceptor(), this));
    }

    public void loadQueues() {
        logger.debug("Ładuje kolejki sprzed restartu...");
        if (lavaClient.getNodes().stream().noneMatch(LavalinkSocket::isAvailable)) {
           logger.info("Czekam aż jakiś node będzie dostępny...");
           long czekam = 0;
           while (lavaClient.getNodes().stream().noneMatch(LavalinkSocket::isAvailable)) {
               try {
                   Thread.sleep(100);
                   czekam += 100;
               } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
               }
               if (czekam >= 5000) {
                   logger.error("Żaden node nie jest dostępny po 5s, przerywam!");
                   return;
               }
           }
           logger.info("Node dostępny!");
        }
        for (Queue queue : queueDao.getAllAutoClosed()) {
            try {
                if (!queue.isAutoZapisane()) {
                    logger.warn("Uhhhhh, kolejka nie auto zapisana przedarła się do auto zapisanych..? {}", queue);
                    continue;
                }
                Guild gild = shardManager.getGuildById(queue.getId());
                if (gild == null) {
                    logger.warn("Lol wywalili bota z {}", queue.getId());
                    continue;
                }
                logger.debug("Zaczynamy ładować {} piosenek z serwera {}", queue.getPiosenki().size(), queue.getId());
                ManagerMuzykiSerwera mms = getManagerMuzykiSerwera(gild);
                mms.loadQueue(queue);
            } catch (Exception e) {
                Sentry.getContext().addExtra("queue", queue);
                Sentry.capture(e);
                Sentry.clearContext();
                logger.error("Nie udało się załadować kolejki {}:", queue);
            }
            queueDao.delete(queue);
        }
        logger.debug("Gotowe!");
    }

    public ManagerMuzykiSerwera getManagerMuzykiSerwera(Guild guild) {
        return getManagerMuzykiSerwera(guild, true);
    }

    public ManagerMuzykiSerwera getManagerMuzykiSerwera(Guild guild, boolean create) {
        if (kolejki.containsKey(guild.getId())) return kolejki.get(guild.getId());
        else if (create) {
            ManagerMuzykiSerwera mms = new NowyManagerMuzykiSerwera(this, guild, lavaClient, tlumaczenia, queueDao, guildDao, executorService);
            kolejki.put(guild.getId(), mms);
            return kolejki.get(guild.getId());
        } else return null;
    }

    public List<AudioTrack> getAudioTracks(String url) {
        Ustawienia.Lavalink.LavalinkNode nod = Ustawienia.instance.lavalink.nodes.get(0);
        JSONObject td = null;
        try (Response resp = NetworkUtil.downloadResponse("http://" + nod.address + ":" + nod.restPort +
                "/loadtracks?identifier=" + URLEncoder.encode(url, "UTF-8"), nod.password)) {
            ResponseBody body = resp.body();
            if (body == null) throw new NullPointerException("body() null");
            td = new JSONObject(body.string());
            List<AudioTrack> list = new ArrayList<>();
            td.getJSONArray("tracks").forEach(o -> {
                try {
                    list.add(LavalinkUtil.toAudioTrack(((JSONObject) o).getString("track")));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            return list;
        } catch (JSONException exc) {
            if (td != null)
                Sentry.capture(new EventBuilder().withSentryInterface(new ExceptionInterface(exc))
                        .withMessage(exc.getMessage()).withExtra("JSON", td.toString()));
            throw exc;
        } catch (IOException exc) {
            throw new IllegalStateException(exc);
        }
    }

    public void getAudioTracksAsync(String url, Consumer<List<AudioTrack>> cons) {
        Thread xd = new Thread(() -> {
            try {
                cons.accept(getAudioTracks(url));
            } catch (Exception e) {
                logger.error("Nie udało się pobrać muzyki!", e);
                cons.accept(Collections.emptyList());
            }
        });
        xd.setName("AsyncAudioTrackResolver");
        xd.start();
    }

    public void shutdown() {
        for (ManagerMuzykiSerwera mms : kolejki.values()) {
            try {
                mms.shutdown();
            } catch (Exception e) {
                //nic
            }
        }
        lavaClient.shutdown();
        eventBus.unregister(lavaClient);
        eventBus.unregister(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(GenericGuildVoiceEvent e) {
        ManagerMuzykiSerwera mms = getManagerMuzykiSerwera(e.getGuild(), false);
        if (mms != null) mms.onEvent(e);
    }

    public void destroy(String id) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1000);
                Link link = lavaClient.getExistingLink(id);
                if (link != null) link.destroy();
                kolejki.remove(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }, "Niszczenie-kolejki-" + id);
        t.start();
    }

    static class Vdi implements VoiceDispatchInterceptor {
        private final VoiceDispatchInterceptor child;
        private final NowyManagerMuzyki mm;

        private Vdi(VoiceDispatchInterceptor child, NowyManagerMuzyki mm) {
            this.child = child;
            this.mm = mm;
        }

        @Override
        public void onVoiceServerUpdate(@Nonnull VoiceServerUpdate e) {
            logger.debug("VoiceServerUpdate dla serwera {}: session ID: {}, endpoint: {}, token: {}",
                    e.getGuild().getName(), e.getSessionId(), e.getEndpoint(), e.getToken());
            ManagerMuzykiSerwera mms = mm.getManagerMuzykiSerwera(e.getGuild());
            if (mms == null) {
                logger.warn("VoiceServerUpdate: mms == null..?");
                return;
            }
            child.onVoiceServerUpdate(e);
            mms.patchVoiceServerUpdate(e);
        }

        @Override
        public boolean onVoiceStateUpdate(@Nonnull VoiceStateUpdate update) {
            logger.debug("VoiceStateUpdate: {}", update);
            return child.onVoiceStateUpdate(update);
        }
    }
}
