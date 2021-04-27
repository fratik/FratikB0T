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

package pl.fratik.music.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.*;
import io.sentry.Sentry;
import lavalink.client.io.Link;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;
import pl.fratik.music.utils.SpotifyUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final SearchManager searchManager;
    private final GuildDao guildDao;
    private final SpotifyUtil spotifyUtil;

    public PlayCommand(NowyManagerMuzyki managerMuzyki, SearchManager searchManager, GuildDao guildDao, SpotifyUtil spotifyUtil) {
        this.managerMuzyki = managerMuzyki;
        this.searchManager = searchManager;
        this.guildDao = guildDao;
        this.spotifyUtil = spotifyUtil;
        name = "play";
        uzycie = new Uzycie("utwor", "string", true);
        aliases = new String[] {"odtworz", "p", "add"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
            context.reply(context.getTranslated("play.dj"));
            return false;
        }
        GuildVoiceState memVS = context.getMember().getVoiceState();
        GuildVoiceState selfVS = context.getGuild().getSelfMember().getVoiceState();
        if (memVS == null || !memVS.inVoiceChannel()) {
            context.reply(context.getTranslated("play.not.connected"));
            return false;
        }
        if (managerMuzyki.getLavaClient().getLink(context.getGuild()).getState() == Link.State.CONNECTED &&
                selfVS != null && !Objects.equals(memVS.getChannel(), selfVS.getChannel())) {
            context.reply(context.getTranslated("music.different.channels"));
            return false;
        }
        VoiceChannel kanal = memVS.getChannel();
        if (kanal == null) throw new IllegalStateException("połączony ale nie na kanale, co do");
        EnumSet<Permission> upr = context.getGuild().getSelfMember().getPermissions(kanal);
        if (!Stream.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).allMatch(upr::contains)) {
            context.reply(context.getTranslated("play.no.permissions"));
            return false;
        }
        String identifier;
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms.isPaused()) {
            context.reply(context.getTranslated("play.use.pause"));
            return false;
        }
        String url = (String) context.getArgs()[0];
        if (CommonUtil.URL_PATTERN.matcher(url).matches()) {
            List<String> iteml = new ArrayList<>();

            try {
                if (spotifyUtil.isTrack(url)) {
                    Track track = spotifyUtil.getTrackFromUrl(url);
                    url = track.getArtists()[0].getName() + " " + track.getName();
                } else if (spotifyUtil.isPlaylist(url)) {
                    context.getTextChannel().sendTyping().queue();
                    Paging<PlaylistTrack> album = spotifyUtil.getPlaylistFromUrl(url);
                    List<PlaylistTrack> items = Arrays.stream(album.getItems())
                            .filter(s -> !s.getIsLocal())
                            .collect(Collectors.toList());

                    for (PlaylistTrack item : items) {
                        try {
                            Track track = (Track) item.getTrack();
                            iteml.add(track.getArtists()[0].getName() + " " + track.getName());
                        } catch (Exception e) {
                            Sentry.capture(e);
                        }
                    }

                    if (iteml.isEmpty()) {
                        context.send(context.getTranslated("play.spotify.playlistsearch.nofound"));
                        return false;
                    }
                } else if (spotifyUtil.isAlbum(url)) {
                    Album album = spotifyUtil.getAlbumFromUrl(url);
                    for (TrackSimplified item : album.getTracks().getItems()) {
                        iteml.add(item.getArtists()[0].getName() + " " + item.getName());
                    }
                } else if (spotifyUtil.isArtists(url)) {
                    Track[] tracks = spotifyUtil.getArtistsTracks(url, context.getLanguage());
                    for (Track track : tracks) {
                        iteml.add(track.getArtists()[0].getName() + " " + track.getName());
                    }
                }
            } catch (NullPointerException nul) {
                context.send(context.getTranslated("play.spotify.search.nofound"));
                return false;
            } catch (Exception e) {
                Sentry.capture(e);
                context.send(context.getTranslated("play.spotify.search.error", e.getMessage()));
                return false;
            }

            if (!iteml.isEmpty()) {
                int dodanePiosenki = 0;
                if (!mms.isConnected()) {
                    mms.setAnnounceChannel(context.getTextChannel());
                    mms.connect(kanal);
                }
                for (String s : iteml) {
                    try {
                        List<AudioTrack> result = managerMuzyki.getAudioTracks("ytsearch:" + s);
                        if (result.isEmpty()) continue;
                        AudioTrack piosenka = result.get(0);
                        dodanePiosenki++;
                        mms.addToQueue(context.getSender(), piosenka, context.getLanguage(), null);
                    } catch (Exception ignored) { }
                }
                if (dodanePiosenki == 0) mms.disconnect();
                else {
                    if (!mms.isPlaying()) mms.play();
                }
                context.reply(context.getTranslated("play.queued.multiple", dodanePiosenki));
                return true;
            }
            else {
                SearchManager.SearchResult wynik = searchManager.searchYouTube(url);
                if (wynik == null || wynik.getEntries().isEmpty()) {
                    context.reply(context.getTranslated("play.no.results"));
                    return false;
                }
                identifier = wynik.getEntries().get(0).getUrl();
            }
        } else identifier = url;
        if (!mms.isConnected()) {
            mms.setAnnounceChannel(context.getTextChannel());
            mms.connect(kanal);
        }
        if (!mms.isConnected()) return false;
        managerMuzyki.getAudioTracksAsync(identifier, audioTrackList -> {
            if (audioTrackList.isEmpty()) {
                context.reply(context.getTranslated("play.not.found"));
                mms.disconnect();
                return;
            }
            if (audioTrackList.size() == 1) {
                AudioTrack at = audioTrackList.get(0);
                mms.addToQueue(context.getSender(), at, context.getLanguage(), null);
            } else {
                for (AudioTrack at : audioTrackList) mms.addToQueue(context.getSender(), at, context.getLanguage());
                context.reply(context.getTranslated("play.queued.playlist", audioTrackList.size()));
            }
            if (!mms.isPlaying()) mms.play();
            else context.getTextChannel().sendMessage(context.getTranslated("play.queued",
                    audioTrackList.get(0).getInfo().title)).reference(context.getMessage()).queue();
        });
        return true;
    }

}
