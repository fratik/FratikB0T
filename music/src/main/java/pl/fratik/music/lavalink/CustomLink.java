/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.music.lavalink;

import lavalink.client.io.GuildUnavailableException;
import lavalink.client.io.Link;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.music.managers.ManagerMuzykiSerwera;

public class CustomLink extends Link {
    private final CustomLavalink lavalink;
    private final Logger log;
    @Setter private ManagerMuzykiSerwera mms;

    protected CustomLink(CustomLavalink lavalink, String guildId) {
        super(lavalink, guildId);
        this.lavalink = lavalink;
        log = LoggerFactory.getLogger(getClass());
    }

    public void connect(@NotNull AudioChannel audioChannel) {
        connect(audioChannel, true);
    }

    @SuppressWarnings("WeakerAccess")
    void connect(@NotNull AudioChannel channel, boolean checkChannel) {
        if (!channel.getGuild().equals(getJda().getGuildById(guild)))
            throw new IllegalArgumentException("The provided AudioChannel is not a part of the Guild that this AudioManager handles." +
                    "Please provide an AudioChannel from the proper Guild");
        if (channel.getJDA().isUnavailable(channel.getGuild().getIdLong()))
            throw new GuildUnavailableException("Cannot open an Audio Connection with an unavailable guild. " +
                    "Please wait until this Guild is available to open a connection.");
        final Member self = channel.getGuild().getSelfMember();
        if (!self.hasPermission(channel, Permission.VOICE_CONNECT) && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS))
            throw new InsufficientPermissionException(channel, Permission.VOICE_CONNECT);

        //If we are already connected to this VoiceChannel, then do nothing.
        GuildVoiceState voiceState = channel.getGuild().getSelfMember().getVoiceState();
        if (voiceState == null) return;

        if (checkChannel && channel.equals(voiceState.getChannel()))
            return;

        if (voiceState.inAudioChannel()) {
            int userLimit = 0; // userLimit is 0 if no limit is set!

            if (channel instanceof VoiceChannel) {
                VoiceChannel vc = (VoiceChannel) channel;
                userLimit = vc.getUserLimit();
            }

            if (!self.isOwner() && !self.hasPermission(Permission.ADMINISTRATOR)) {
                if (userLimit > 0                                                      // If there is a userlimit
                        && userLimit <= channel.getMembers().size()                    // if that userlimit is reached
                        && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)) // If we don't have voice move others permissions
                    throw new InsufficientPermissionException(channel, Permission.VOICE_MOVE_OTHERS, // then throw exception!
                            "Unable to connect to VoiceChannel due to userlimit! Requires permission VOICE_MOVE_OTHERS to bypass");
            }
        }

        setState(State.CONNECTING);
        queueAudioConnect(channel.getIdLong());
    }

    public JDA getJda() {
        return lavalink.getJdaFromSnowflake(String.valueOf(guild));
    }

    @Override
    protected void removeConnection() {
        // JDA podobno to ogarnia
    }

    @Override
    protected void queueAudioDisconnect() {
        Guild g = getJda().getGuildById(guild);

        if (g != null) getJda().getDirectAudioController().disconnect(g);
    }

    @Override
    protected void queueAudioConnect(long channelId) {
        AudioChannel channel = getJda().getChannelById(AudioChannel.class, channelId);
        if (channel != null) {
            getJda().getDirectAudioController().connect(channel);
        } else {
            log.warn("Attempted to connect, but AudioChannel {} was not found", channelId);
        }
    }

    @Override
    public void onVoiceWebSocketClosed(int code, String reason, boolean byRemote) {
        Guild g = getJda().getGuildById(guild);
        if (g != null && byRemote) new Thread(mms::nodeDisconnected, "AsyncNodeDisconnectedCaller").start();
    }
}
