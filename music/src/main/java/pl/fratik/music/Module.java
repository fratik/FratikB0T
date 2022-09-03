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
import com.google.inject.Inject;
import com.wrapper.spotify.SpotifyApi;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.event.ConnectedEvent;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.NewManagerKomend;
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

@SuppressWarnings("FieldCanBeLocal")
public class Module implements Modul {
    @Inject private NewManagerKomend managerKomend;
    @Inject private EventWaiter eventWaiter;
    @Inject private GuildDao guildDao;
    @Inject private UserDao userDao;
    @Inject private ShardManager shardManager;
    @Inject private EventBus eventBus;
    @Inject private Tlumaczenia tlumaczenia;
    @Inject private ManagerArgumentow managerArgumentow;
    @Inject private ManagerBazyDanych managerBazyDanych;
    @Inject private RedisCacheManager redisCacheManager;
    private NowyManagerMuzyki managerMuzyki;
    private ArrayList<NewCommand> commands;
    private QueueDao queueDao;

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
            spotifyUtil = new SpotifyUtil(spotifyApi);
        }

        commands = new ArrayList<>();

        commands.add(new PlayCommand(managerMuzyki, searchManager, guildDao, spotifyUtil, userDao, redisCacheManager));
        commands.add(new SkipCommand(managerMuzyki, guildDao, redisCacheManager));
        commands.add(new YoutubeCommand(managerMuzyki, searchManager, eventWaiter, guildDao, userDao, redisCacheManager));
        commands.add(new VolumeCommand(managerMuzyki, guildDao));
        commands.add(new QueueCommand(managerMuzyki, eventWaiter, eventBus));
        commands.add(new PlaylistCommand(managerMuzyki, queueDao));
        commands.add(new RepeatCommand(managerMuzyki, guildDao));
        commands.add(new LeaveCommand(managerMuzyki));
        commands.add(new NowplayingCommand(managerMuzyki));
        commands.add(new PauseCommand(managerMuzyki, guildDao));
        commands.add(new ShuffleCommand(managerMuzyki, guildDao));
        if (Ustawienia.instance.apiKeys.containsKey("genius"))
            commands.add(new TekstCommand(eventWaiter, eventBus, managerMuzyki));
        commands.add(new NodesCommand());

        managerKomend.registerCommands(this, commands);

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

        managerKomend.unregisterAll(this);
        return true;
    }
}
