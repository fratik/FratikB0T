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
import com.wrapper.spotify.model_objects.specification.*;
import io.sentry.Sentry;
import io.sentry.event.User;
import lavalink.client.io.Link;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;
import pl.fratik.music.managers.SearchManager;
import pl.fratik.music.utils.SpotifyUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final SearchManager searchManager;
    private final GuildDao guildDao;
    private final SpotifyUtil spotifyUtil;
    private final UserDao userDao;
    private final Cache<UserConfig> userCache;

    public PlayCommand(NowyManagerMuzyki managerMuzyki, SearchManager searchManager, GuildDao guildDao, SpotifyUtil spotifyUtil, UserDao userDao, RedisCacheManager rcm) {
        this.managerMuzyki = managerMuzyki;
        this.searchManager = searchManager;
        this.guildDao = guildDao;
        this.spotifyUtil = spotifyUtil;
        this.userDao = userDao;
        this.userCache = rcm.new CacheRetriever<UserConfig>(){}.getCache();
        name = "play";
        usage = "<link:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        UserConfig userConfig = userCache.get(context.getSender().getId(), userDao::get);
        if (!userConfig.isYtWarningSeen()) {
            context.replyEphemeral(context.getTranslated("youtube.warning"));
            userConfig.setYtWarningSeen(true);
            userDao.save(userConfig);
            return;
        }
        GuildVoiceState memVS = context.getMember().getVoiceState();
        GuildVoiceState selfVS = context.getGuild().getSelfMember().getVoiceState();
        if (memVS == null || !memVS.inAudioChannel()) {
            context.replyEphemeral(context.getTranslated("play.not.connected"));
            return;
        }
        if (Optional.ofNullable(managerMuzyki.getLavaClient().getExistingLink(context.getGuild()))
                .map(Link::getState).orElse(null) == Link.State.CONNECTED &&
                selfVS != null && !Objects.equals(memVS.getChannel(), selfVS.getChannel())) {
            context.replyEphemeral(context.getTranslated("music.different.channels"));
            return;
        }
        AudioChannel kanal = memVS.getChannel();
        if (kanal == null) throw new IllegalStateException("połączony ale nie na kanale, co do");
        EnumSet<Permission> upr = context.getGuild().getSelfMember().getPermissions(kanal);
        if (!Stream.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).allMatch(upr::contains)) {
            context.replyEphemeral(context.getTranslated("play.no.permissions"));
            return;
        }
        String identifier = null;
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms.isPaused()) {
            context.replyEphemeral(context.getTranslated("play.use.pause"));
            return;
        }
        String url = context.getArguments().get("link").getAsString();
        context.defer(false);
        if (CommonUtil.URL_PATTERN.matcher(url).matches()) {
            if (spotifyUtil != null) {
                List<String> iteml = new ArrayList<>();

                try {
                    if (spotifyUtil.isTrack(url)) {
                        Track track = spotifyUtil.getTrackFromUrl(url);
                        SearchManager.SearchResult wynik;
                        wynik = searchManager.searchYouTube(track.getArtists()[0].getName() + " " + track.getName());
                        if (wynik == null || wynik.getEntries().isEmpty()) {
                            context.sendMessage(context.getTranslated("play.no.results"));
                            return;
                        }
                        identifier = wynik.getEntries().get(0).getUrl();
                    } else if (spotifyUtil.isPlaylist(url)) {
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
                            context.sendMessage(context.getTranslated("play.spotify.playlistsearch.nofound"));
                            return;
                        }
                    } else if (spotifyUtil.isAlbum(url)) {
                        context.defer(false);
                        Album album = spotifyUtil.getAlbumFromUrl(url);
                        for (TrackSimplified item : album.getTracks().getItems()) {
                            iteml.add(item.getArtists()[0].getName() + " " + item.getName());
                        }
                    } else if (spotifyUtil.isArtists(url)) {
                        context.defer(false);
                        Track[] tracks = spotifyUtil.getArtistsTracks(url, context.getLanguage());
                        for (Track track : tracks) {
                            iteml.add(track.getArtists()[0].getName() + " " + track.getName());
                        }
                    }
                } catch (NullPointerException nul) {
                    context.sendMessage(context.getTranslated("play.spotify.search.nofound"));
                    return;
                } catch (Exception e) {
                    Sentry.getContext().setUser(new User(context.getSender().getId(),
                        UserUtil.formatDiscrim(context.getSender()), null, null));
                    Sentry.getContext().addExtra("url", url);
                    Sentry.capture(e);
                    Sentry.clearContext();
                    context.sendMessage(context.getTranslated("play.spotify.search.error"));
                    return;
                }

                if (!iteml.isEmpty()) {
                    int dodanePiosenki = 0;
                    if (!mms.isConnected()) {
                        if (!context.getChannel().canTalk()) context.sendMessage(context.getTranslated("play.no.perms.warning"));
                        mms.setAnnounceChannel(context.getChannel());
                        mms.connect(kanal);
                    }
                    for (String s : iteml) {
                        try {
                            List<AudioTrack> result = managerMuzyki.getAudioTracks("ytsearch:" + s);
                            if (result.isEmpty()) continue;
                            AudioTrack piosenka = result.get(0);
                            dodanePiosenki++;
                            mms.addToQueue(context.getSender(), piosenka, context.getLanguage(), null);
                        } catch (Exception ignored) {
                        }
                    }
                    if (dodanePiosenki == 0 && !mms.isPlaying()) mms.disconnect();
                    else if (!mms.isPlaying()) mms.play();
                    context.sendMessage(context.getTranslated("play.queued.multiple", dodanePiosenki));
                    return;
                }
            }
            if (identifier == null) identifier = url; // jeżeli SpotifyUtil nic nie wykryje, spróbuj po linku
        } else {
            SearchManager.SearchResult wynik = searchManager.searchYouTube(url);
            if (wynik == null || wynik.getEntries().isEmpty()) {
                context.sendMessage(context.getTranslated("play.no.results"));
                return;
            }
            identifier = wynik.getEntries().get(0).getUrl();
        }
        if (!mms.isConnected()) {
            if (!context.getChannel().canTalk()) context.sendMessage(context.getTranslated("play.no.perms.warning"));
            mms.setAnnounceChannel(context.getChannel());
            mms.connect(kanal);
        }
        if (!mms.isConnected()) return;
        managerMuzyki.getAudioTracksAsync(identifier, audioTrackList -> {
            if (audioTrackList.isEmpty()) {
                context.sendMessage(context.getTranslated("play.not.found"));
                mms.disconnect();
                return;
            }
            if (audioTrackList.size() == 1) {
                AudioTrack at = audioTrackList.get(0);
                mms.addToQueue(context.getSender(), at, context.getLanguage(), null);
            } else {
                for (AudioTrack at : audioTrackList) mms.addToQueue(context.getSender(), at, context.getLanguage());
                context.sendMessage(context.getTranslated("play.queued.playlist", audioTrackList.size()));
            }
            context.sendMessage(context.getTranslated("play.queued", audioTrackList.get(0).getInfo().title));
            if (!mms.isPlaying()) mms.play();
        });
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!hasFullDjPerms(context.getMember(), guildDao)) {
            context.replyEphemeral(context.getTranslated("play.dj"));
            return false;
        }
        return super.permissionCheck(context);
    }
}
