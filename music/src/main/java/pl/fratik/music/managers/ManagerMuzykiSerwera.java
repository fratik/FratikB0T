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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.music.entity.Piosenka;
import pl.fratik.music.entity.PiosenkaImpl;
import pl.fratik.music.entity.Queue;
import pl.fratik.music.entity.RepeatMode;

import java.util.List;

public interface ManagerMuzykiSerwera {
    default void addToQueue(User requester, AudioTrack track, Language language) {
        addToQueue(new PiosenkaImpl(requester.getAsTag(), track, language));
    }

    default void addToQueue(String requester, AudioTrack track, Language language) {
        addToQueue(new PiosenkaImpl(requester, track, language));
    }

    default void addToQueue(User requester, AudioTrack track, Language language, String thumbnailUrl) {
        addToQueue(new PiosenkaImpl(requester.getAsTag(), track, language, thumbnailUrl));
    }

    default void addToQueue(String requester, AudioTrack track, Language language, String thumbnailUrl) {
        addToQueue(new PiosenkaImpl(requester, track, language, thumbnailUrl));
    }

    void addToQueue(Piosenka piosenka);

    void play();

    void play(Piosenka utwor);

    void connect(AudioChannel channel);

    void disconnect();

    boolean pause();

    void skip();

    void setVolume(int volume);

    int getVolume();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isPlaying();

    boolean isPaused();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isConnected();

    void setAnnounceChannel(MessageChannel announceChannel);

    String getPosition();

    List<Piosenka> getKolejka();

    Piosenka getAktualnaPiosenka();

    RepeatMode getRepeatMode();

    MessageChannel getAnnounceChannel();

    AudioChannel getChannel();

    List<String> getSkips();

    void setRepeatMode(RepeatMode repeatMode);

    long getPositionLong();

    void loadQueue(Queue queue);

    void shutdown();

    void patchVoiceServerUpdate(VoiceDispatchInterceptor.VoiceServerUpdate e);

    default void nodeDisconnected() {
        throw new UnsupportedOperationException();
    }

    void onEvent(GenericEvent e);
    
    void shuffleQueue();

}
