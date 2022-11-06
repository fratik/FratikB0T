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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.Link;
import lavalink.client.player.IPlayer;
import lavalink.client.player.event.PlayerEventListenerAdapter;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.TimeUtil;
import pl.fratik.music.entity.Piosenka;
import pl.fratik.music.entity.Queue;
import pl.fratik.music.entity.QueueDao;
import pl.fratik.music.entity.RepeatMode;
import pl.fratik.music.lavalink.CustomLavalink;
import pl.fratik.music.lavalink.CustomLink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NowyManagerMuzykiSerwera implements ManagerMuzykiSerwera {

    private static final Logger LOGGER = LoggerFactory.getLogger(NowyManagerMuzykiSerwera.class);
    private final NowyManagerMuzyki nowyManagerMuzyki;
    private final Guild guild;
    private final CustomLavalink lavaClient;
    private final Tlumaczenia tlumaczenia;
    private final QueueDao queueDao;
    private final GuildDao guildDao;
    private final ScheduledExecutorService executorService;
    private boolean init;
    private final java.util.Queue<Piosenka> kolejka = new ConcurrentLinkedQueue<>();
    @Getter private AudioChannel channel;
    private IPlayer player;
    @Getter private Piosenka aktualnaPiosenka;
    @Getter @Setter private MessageChannel announceChannel;
    @Getter @Setter private RepeatMode repeatMode = RepeatMode.OFF;
    private boolean shutdown;
    private boolean exception;
    @Getter private List<String> skips;
    private boolean paused;
    private CustomLink link;
    private ScheduledFuture<?> future;
    private Listener lisner;

    NowyManagerMuzykiSerwera(NowyManagerMuzyki nowyManagerMuzyki, Guild guild, CustomLavalink lavaClient, Tlumaczenia tlumaczenia, QueueDao queueDao, GuildDao guildDao, ScheduledExecutorService executorService) {
        this.nowyManagerMuzyki = nowyManagerMuzyki;
        this.guild = guild;
        this.lavaClient = lavaClient;
        this.tlumaczenia = tlumaczenia;
        this.queueDao = queueDao;
        this.guildDao = guildDao;
        this.executorService = executorService;
    }

    @Override
    public void addToQueue(Piosenka piosenka) {
        kolejka.add(piosenka);
    }

    @Override
    public void play() {
        if (!init) throw new IllegalStateException("nie init");
        if (kolejka.isEmpty()) throw new IllegalStateException("Kolejka nie ma piosenek!");
        play(kolejka.poll());
    }

    @Override
    public void play(Piosenka utwor) {
        utwor.getAudioTrack().setPosition(0);
        if (player == null) throw new IllegalStateException("Nie ma playera!");
        while (link.getState() != Link.State.CONNECTED) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        skips = new ArrayList<>();
        aktualnaPiosenka = utwor;
        player.playTrack(aktualnaPiosenka.getAudioTrack());
    }

    @Override
    public void connect(AudioChannel channel) {
        this.channel = channel;
        link = lavaClient.getLink(guild);
        link.setMms(this);
        if (init) throw new IllegalStateException("Już połączony!");
        player = link.getPlayer();
        LOGGER.debug("Otwieram połączenie audio, wyczekuj VoiceServerUpdate");
        try {
            link.connect(channel);
        } catch (InsufficientPermissionException e) {
            if (announceChannel != null) {
                announceChannel.sendMessage("Bot nie może się połączyć, bo nie ma uprawnień. Sprawdź czy może " +
                        "wejść na kanał, a jeżeli tak to czy limit użytkowników nie jest za mały!").queue();
                return;
            }
            throw e;
        }
        int czekam = 0;
        while (!init) {
            try {
                Thread.sleep(5);
                czekam++;
                if (czekam == 1000) {
                    LOGGER.error("{}: Nie udało się połączyć po 5s!", guild.getId());
                    announceChannel.sendMessage("Nie udało się połączyć po 5s! Anuluje!").queue();
                    disconnect();
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    @Override
    public void disconnect() {
        if (channel != null) channel = null;
        if (player == null) throw new IllegalStateException("Nie ma playera!");
        paused = false;
        aktualnaPiosenka = null;
        shutdown = true;
        if (lisner != null) player.removeListener(lisner);
        lisner = null;
        if (player.getPlayingTrack() != null) player.stopTrack();
        link.disconnect();
        kolejka.clear();
        player = null;
        link.resetPlayer();
        init = false;
        nowyManagerMuzyki.destroy(guild.getId());
    }

    @Override
    public boolean pause() {
        paused = !paused;
        player.setPaused(paused);
        return paused;
    }

    @Override
    public void skip() {
        if (player == null) throw new IllegalStateException("Nie ma playera!");
        if (link.getState() != Link.State.CONNECTED) throw new IllegalStateException("State != CONNECTED");
        if (repeatMode == RepeatMode.ONCE) {
            play(aktualnaPiosenka);
            return;
        }
        if (player.getPlayingTrack() != null) player.stopTrack();
        else {
            if (kolejka.isEmpty()) return;
            aktualnaPiosenka = kolejka.poll();
            player.playTrack(aktualnaPiosenka.getAudioTrack());
        }
    }

    @Override
    public void setVolume(int volume) {
        player.setVolume(volume);
    }

    @Override
    public int getVolume() {
        return player.getVolume();
    }

    @Override
    public boolean isPlaying() {
        return isConnected() && player != null && link.getState() == Link.State.CONNECTED && player.getPlayingTrack() != null;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean isConnected() {
        return link != null && link.getState() == Link.State.CONNECTED;
    }

    @Override
    public String getPosition() {
        return TimeUtil.getStringFromMillis(player.getTrackPosition());
    }

    @Override
    public List<Piosenka> getKolejka() {
        return new ArrayList<>(kolejka);
    }

    @Override
    public long getPositionLong() {
        return player.getTrackPosition();
    }

    @Override
    public void loadQueue(Queue queue) {
        kolejka.clear();
        if (!queue.getPiosenki().isEmpty()) kolejka.addAll(queue.getPiosenki());
        if (queue.isAutoZapisane()) {
            announceChannel = queue.getAnnounceChannel();
            aktualnaPiosenka = queue.getAktualnaPiosenka();
            repeatMode = queue.getRepeatMode();
            if (player == null || link.getState() != Link.State.CONNECTED) connect(queue.getVoiceChannel());
            int czekam = 0;
            while (player == null || link.getState() != Link.State.CONNECTED) {
                try {
                    Thread.sleep(5);
                    czekam++;
                    if (czekam == 1000) {
                        LOGGER.error("{}: Nie udało się połączyć po 5s!", guild.getId());
                        announceChannel.sendMessage("Nie udało się połączyć po 5s! Anuluje!").queue();
                        disconnect();
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            aktualnaPiosenka.getAudioTrack().setPosition(queue.getAktualnaPozycja());
            player.playTrack(aktualnaPiosenka.getAudioTrack());
            setVolume(queue.getVolume());
            if (queue.isPauza()) pause();
            announceChannel.sendMessage(tlumaczenia.get(aktualnaPiosenka.getRequesterLanguage(), "play.shutting.down.complete")).queue();
        }
    }

    @Override
    public void shutdown() {
        if (shutdown) return;
        announceChannel.sendMessage(tlumaczenia.get(aktualnaPiosenka.getRequesterLanguage(), "play.shutting.down")).complete();
        Queue queue = queueDao.get(guild.getId());
        if (queue == null) queue = new Queue(guild.getId());
        queue.autoSave(this);
        queueDao.save(queue);
        kolejka.clear();
        disconnect();
    }

    @Override
    public void patchVoiceServerUpdate(VoiceDispatchInterceptor.VoiceServerUpdate e) {
        shutdown = false;
        if (lisner == null) lisner = new Listener(this);
        player.addListener(lisner);
        init = true;
    }

    @Override
    public void nodeDisconnected() {
        exception = true;
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (announceChannel == null || aktualnaPiosenka == null) {
            exception = false;
            return;
        }
        announceChannel.sendMessage(tlumaczenia.get(aktualnaPiosenka.getRequesterLanguage(),
                "play.song.error.node.disconnected")).queue();
        AtomicInteger tries = new AtomicInteger(0);
        LavalinkSocket nod = link.getNode(false);
        while (nod == null || !nod.isAvailable()) {
            try {
                Thread.sleep(100);
                nod = link.getNode(false);
                if (tries.getAndAdd(1) >= 600) {
                    //poddaję się
                    if (announceChannel != null)
                        announceChannel.sendMessage(tlumaczenia.get(aktualnaPiosenka.getRequesterLanguage(),
                                "play.song.error.node.disconnected.cant.found")).queue();
                    exception = false;
                    disconnect();
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        announceChannel.sendMessage(tlumaczenia.get(aktualnaPiosenka.getRequesterLanguage(), "play.song.error.node.disconnected.restarted")).queue();
        exception = false;
    }

    @Override
    public void onEvent(GenericEvent e) {
        if (e instanceof GenericGuildVoiceEvent && channel != null && channel.getMembers().stream()
                .allMatch(m -> m.getUser().isBot())) {
            if (future != null) future.cancel(false);
            future = executorService.schedule(() -> {
                if (!isConnected()) return;
                if (channel.getMembers().stream().allMatch(m -> m.getUser().isBot())) {
                    announceChannel.sendMessage(tlumaczenia.get(tlumaczenia.getLanguage(guild),
                            "play.everyone.left")).queue();
                    disconnect();
                }
            }, 30, TimeUnit.SECONDS);
        }
    }

    @Override
    public void shuffleQueue() {
        ArrayList<Piosenka> piosenki = new ArrayList<>(getKolejka());
        kolejka.clear();
        Collections.shuffle(piosenki);
        kolejka.addAll(piosenki);
    }

    static class Listener extends PlayerEventListenerAdapter {

        private final NowyManagerMuzykiSerwera mms;

        private Listener(NowyManagerMuzykiSerwera mms) {
            this.mms = mms;
        }

        @Override
        public void onTrackStart(IPlayer player, AudioTrack track) {
            if (mms.repeatMode != RepeatMode.OFF) return;
            MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
            messageCreateBuilder.setContent(mms.tlumaczenia.get(mms.getAktualnaPiosenka().getRequesterLanguage(), "play.playing",
                mms.getAktualnaPiosenka().getAudioTrack().getInfo().title, mms.getAktualnaPiosenka().getRequester()));

            mms.announceChannel.sendMessage(messageCreateBuilder.build()).queue();
            try {
                String muzycznyKanal = mms.guildDao.get(mms.guild).getKanalMuzyczny();
                TextChannel ch = mms.guild.getTextChannelById(muzycznyKanal);
                if (ch == null) return;
                ch.getManager().setTopic(mms.tlumaczenia.get(mms.tlumaczenia.getLanguage(ch.getGuild()),
                        "play.playing.topic", mms.getAktualnaPiosenka().getAudioTrack().getInfo().title,
                        mms.getAktualnaPiosenka().getRequester())).queue(null, tuTezNull -> {});
            } catch (Exception e) {
                // nic
            }
        }

        @Override
        public void onTrackEnd(IPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (mms.shutdown || mms.exception) return;
            if (endReason != AudioTrackEndReason.STOPPED && !endReason.mayStartNext) return;
            if (mms.link.getState() == Link.State.NOT_CONNECTED) {
                mms.announceChannel.sendMessage(mms.tlumaczenia.get(mms.aktualnaPiosenka.getRequesterLanguage(), "play.disconnected")).queue();
                mms.disconnect();
                try {
                    String muzycznyKanal = mms.guildDao.get(mms.guild).getKanalMuzyczny();
                    TextChannel ch = mms.guild.getTextChannelById(muzycznyKanal);
                    if (ch == null) return;
                    ch.getManager().setTopic(mms.tlumaczenia.get(mms.tlumaczenia.getLanguage(ch.getGuild()),
                            "play.queue.empty.topic", mms.getAktualnaPiosenka().getAudioTrack().getInfo().title,
                            mms.getAktualnaPiosenka().getRequester())).queue(null, tuTezNull -> {});
                } catch (Exception e) {
                    // nic
                }
                return;
            }
            if (mms.repeatMode == RepeatMode.ONCE) {
                mms.play(mms.aktualnaPiosenka);
                return;
            }
            if (!mms.kolejka.isEmpty()) mms.play();
            else {
                mms.announceChannel.sendMessage(mms.tlumaczenia.get(mms.aktualnaPiosenka.getRequesterLanguage(), "play.queue.empty")).queue();
                try {
                    String muzycznyKanal = mms.guildDao.get(mms.guild).getKanalMuzyczny();
                    TextChannel ch = mms.guild.getTextChannelById(muzycznyKanal);
                    if (ch == null) return;
                    ch.getManager().setTopic(mms.tlumaczenia.get(mms.tlumaczenia.getLanguage(ch.getGuild()),
                            "play.queue.empty.topic", mms.getAktualnaPiosenka().getAudioTrack().getInfo().title,
                            mms.getAktualnaPiosenka().getRequester())).queue(null, tuTezNull -> {});
                } catch (Exception e) {
                    // nic
                }
                mms.disconnect();
            }
        }

        @Override
        public void onTrackException(IPlayer player, AudioTrack track, Exception exception) {
            LOGGER.error("Wyebało!", exception);
            mms.exception = true;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mms.announceChannel.sendMessage(mms.tlumaczenia.get(mms.aktualnaPiosenka.getRequesterLanguage(), "play.song.error")).queue();
            if (!mms.kolejka.isEmpty()) mms.play();
            else {
                mms.announceChannel.sendMessage(mms.tlumaczenia.get(mms.aktualnaPiosenka.getRequesterLanguage(), "play.queue.empty")).queue();
                mms.disconnect();
            }
            mms.exception = false;
        }
    }
}
