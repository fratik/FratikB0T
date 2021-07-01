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

package pl.fratik.music;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.wrapper.spotify.SpotifyApi;
import io.undertow.server.RoutingHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.LoggerFactory;
import pl.fratik.api.entity.Exceptions;
import pl.fratik.api.entity.Successes;
import pl.fratik.api.internale.Exchange;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.Command;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.entity.SpotifyDao;
import pl.fratik.core.event.ConnectedEvent;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.music.commands.*;
import pl.fratik.music.entity.QueueDao;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;
import pl.fratik.music.serializer.QueueDeserializer;
import pl.fratik.music.utils.SpotifyUtil;

import java.util.ArrayList;
import java.util.EnumSet;

import static pl.fratik.api.Module.getJson;

@SuppressWarnings("FieldCanBeLocal")
public class Module implements Modul {
    @Inject private ManagerKomend managerKomend;
    @Inject private EventWaiter eventWaiter;
    @Inject private GuildDao guildDao;
    @Inject private MemberDao memberDao;
    @Inject private ShardManager shardManager;
    @Inject private EventBus eventBus;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private RedisCacheManager redisCacheManager;
    @Inject private ManagerModulow managerModulow;
    private NowyManagerMuzyki managerMuzyki;
    private ArrayList<Command> commands;
    private QueueDao queueDao;
    private SpotifyDao spotifyDao;

    public Module() {
        commands = new ArrayList<>();
    }

    @Override
    public boolean startUp() {
        EnumSet<Permission> permList = Permission.getPermissions(Globals.permissions);
        if (!permList.contains(Permission.VOICE_CONNECT)) permList.add(Permission.VOICE_CONNECT);
        if (Globals.permissions != Permission.getRaw(permList)) {
            LoggerFactory.getLogger(Module.class).debug("Zmieniam long uprawnień: {} -> {}", Globals.permissions, Permission.getRaw(permList));
            Globals.permissions = Permission.getRaw(permList);
        }
        eventBus.register(this);
        queueDao = new QueueDao(managerBazyDanych, eventBus);
        spotifyDao = new SpotifyDao(managerBazyDanych, eventBus);
        NowyManagerMuzyki.setTlumaczenia(tlumaczenia);
        NowyManagerMuzyki.setQueueDao(queueDao);
        managerMuzyki = new NowyManagerMuzyki(shardManager, eventBus, guildDao);
        API.setMm(managerMuzyki);
        SearchManager searchManager = new SearchManager(Ustawienia.instance.apiKeys.get("yt"), Ustawienia.instance.apiKeys.get("yt2"), managerMuzyki, redisCacheManager);
        MusicCommand.setManagerMuzyki(managerMuzyki);
        QueueCommand.setSearchManager(searchManager);
        NowplayingCommand.setSearchManager(searchManager);
        QueueDeserializer.setManagerMuzyki(managerMuzyki);
        QueueDeserializer.setShardManager(shardManager);

        SpotifyUtil spotifyUtil = null;

        String cliendId = Ustawienia.instance.apiKeys.get("spotifyId");
        String clientSecret = Ustawienia.instance.apiKeys.get("spotifySecret");

        if (cliendId != null && clientSecret != null) {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(cliendId)
                    .setClientSecret(clientSecret)
                    .build();
            spotifyUtil = new SpotifyUtil(spotifyApi, spotifyDao);
        }

        commands = new ArrayList<>();

        commands.add(new PlayCommand(managerMuzyki, searchManager, guildDao, spotifyUtil));
        commands.add(new SkipCommand(managerMuzyki, guildDao, redisCacheManager));
        commands.add(new YoutubeCommand(managerMuzyki, searchManager, eventWaiter, guildDao));
        commands.add(new VolumeCommand(managerMuzyki, guildDao));
        commands.add(new QueueCommand(managerMuzyki, eventWaiter, eventBus));
        commands.add(new PlaylistCommand(managerMuzyki, queueDao));
        commands.add(new RepeatCommand(managerMuzyki, managerArgumentow, guildDao));
        commands.add(new LeaveCommand(managerMuzyki));
        commands.add(new NowplayingCommand(managerMuzyki));
        commands.add(new PauseCommand(managerMuzyki, guildDao));
        commands.add(new ShuffleCommand(managerMuzyki, guildDao));
        if (spotifyUtil != null)
            commands.add(new SpotifyStatsCommand(spotifyUtil, eventWaiter, eventBus));
        if (Ustawienia.instance.apiKeys.containsKey("genius"))
            commands.add(new TekstCommand(eventWaiter, eventBus, managerMuzyki));
        commands.add(new NodesCommand());

        commands.forEach(managerKomend::registerCommand);

        Modul modul = managerModulow.getModules().get("api");
        try {
            RoutingHandler routes = (RoutingHandler) modul.getClass().getDeclaredMethod("getRoutes").invoke(modul);
            SpotifyUtil finalSpotifyUtil = spotifyUtil;

            routes.post("/api/user/{userId}/spotify", ex -> {
                String userId = Exchange.pathParams().pathParam(ex, "userId").orElse(null);
                try {
                    try {
                        if (userId == null || shardManager.retrieveUserById(userId).complete() == null) {
                            Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                            return;
                        }
                    } catch (Exception e) {
                        Exchange.body().sendErrorCode(ex, Exceptions.Codes.INVALID_USER);
                        return;
                    }
                    JsonObject json = getJson(ex);
                    finalSpotifyUtil.addUser(userId, json.get("code").getAsString());
                    Exchange.body().sendJson(ex, new Successes.GenericSuccess("zapisano"));
                } catch (Exception e) {
                    Exchange.body().sendErrorCode(ex, Exceptions.Codes.UNKNOWN_ERROR);
                }
            });
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Nie udało się doczepić do modułu api!", e);
        }

        if (shardManager.getShards().stream().anyMatch(s -> !s.getStatus().equals(JDA.Status.CONNECTED))) return true;
        managerMuzyki.loadQueues();

        return true;
    }

    @Subscribe
    private void onConnected(ConnectedEvent e) {
        if (managerMuzyki == null) return;
        managerMuzyki.loadQueues();
    }

    @Override
    public boolean shutDown() {
        EnumSet<Permission> permList = Permission.getPermissions(Globals.permissions);
        permList.remove(Permission.VOICE_CONNECT);
        if (Globals.permissions != Permission.getRaw(permList)) {
            LoggerFactory.getLogger(Module.class).debug("Zmieniam long uprawnień: {} -> {}", Globals.permissions, Permission.getRaw(permList));
            Globals.permissions = Permission.getRaw(permList);
        }
        try {
            eventBus.unregister(this);
            managerMuzyki.shutdown();
        } catch (Exception ignored) {/*lul*/}
        commands.forEach(managerKomend::unregisterCommand);
        return true;
    }
}
