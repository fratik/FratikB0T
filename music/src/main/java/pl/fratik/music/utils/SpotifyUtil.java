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

package pl.fratik.music.utils;

import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.UnauthorizedException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.data.AbstractDataRequest;
import io.sentry.Sentry;
import lombok.Getter;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.Nullable;
import pl.fratik.core.tlumaczenia.Language;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyUtil {

    private static final Pattern TRACK_REGEX = Pattern.compile("^(https://open.spotify.com/track/)([a-zA-Z0-9]+)(.*)$");
    private static final Pattern PLAYLIST_REGEX = Pattern.compile("^(https://open.spotify.com/playlist/)([a-zA-Z0-9]+)(.*)$");
    private static final Pattern ALBUM_REGEX = Pattern.compile("^(https://open.spotify.com/album/)([a-zA-Z0-9]+)(.*)$");
    private static final Pattern ARTISTS_REGEX = Pattern.compile("^(https://open.spotify.com/artist/)([a-zA-Z0-9]+)(.*)$");

    private static final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
    private static boolean failed = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ses::shutdown));
    }

    @Getter
    private final SpotifyApi api;
    private ScheduledFuture<?> schedule;

    public SpotifyUtil(SpotifyApi api) {
        this.api = api;
        refreshAccessToken();
    }

    public boolean isTrack(String url) {
        return TRACK_REGEX.matcher(url).matches();
    }

    public boolean isPlaylist(String url) {
        return PLAYLIST_REGEX.matcher(url).matches();
    }

    public boolean isAlbum(String url) {
        return ALBUM_REGEX.matcher(url).matches();
    }

    public boolean isArtists(String url) {
        return ARTISTS_REGEX.matcher(url).matches();
    }

    public Track getTrackFromUrl(String url) throws ParseException, IOException {
        Matcher reg = TRACK_REGEX.matcher(url);
        if (reg.find()) return e(getApi().getTrack(reg.group(2)));
        else return null;
    }

    public Paging<PlaylistTrack> getPlaylistFromUrl(String url) throws ParseException, IOException {
        Matcher reg = PLAYLIST_REGEX.matcher(url);
        if (reg.find()) return e(getApi().getPlaylistsItems(reg.group(2)));
        else return null;
    }

    public Album getAlbumFromUrl(String url) throws IOException, ParseException {
        Matcher m = ALBUM_REGEX.matcher(url);
        if (m.find()) return e(getApi().getAlbum(m.group(2)));
        else return null;
    }

    public Track[] getArtistsTracks(String url, @Nullable Language lang) throws IOException, ParseException {
        Matcher m = ARTISTS_REGEX.matcher(url);
        CountryCode code = CountryCode.PL;

        if (lang != null) {
            try {
                code = CountryCode.getByAlpha2Code(lang.getAlpha2());
            } catch (Exception ignored) { }
        }

        if (m.find()) return e(getApi().getArtistsTopTracks(m.group(2), code));
        else return null;
    }

    private <T, V extends AbstractDataRequest.Builder<T, ?>> T e(AbstractDataRequest.Builder<T, V> e) throws IOException, ParseException {
        return e(e, 0);
    }

    private <T, V extends AbstractDataRequest.Builder<T, ?>> T e(AbstractDataRequest.Builder<T, V> e, int tries) throws IOException, ParseException {
        if (failed) return null;
        tries++;
        if (tries > 5) return null;
        try {
            return e.build().execute();
        } catch (SpotifyWebApiException ex) {
            if (ex instanceof UnauthorizedException) {
                refreshAccessToken();
                e(e, tries);
            }
            Sentry.capture(ex);
            return null;
        }
    }

    private static int failedTries = 0;

    private synchronized void refreshAccessToken() {
        if (failed) return;
        try {
            ClientCredentials cr = api.clientCredentials().build().execute();
            api.setAccessToken(cr.getAccessToken());
            if (schedule != null) schedule.cancel(false);
            schedule = ses.schedule(this::refreshAccessToken, cr.getExpiresIn() - 300, TimeUnit.SECONDS);
            failedTries = 0;
        } catch (Exception e) {
            failedTries++;
            if (failedTries <= 5) {
                if (schedule != null) schedule.cancel(false);
                schedule = ses.schedule(this::refreshAccessToken, 60, TimeUnit.SECONDS);
            } else failed = true;
        }

    }

}
