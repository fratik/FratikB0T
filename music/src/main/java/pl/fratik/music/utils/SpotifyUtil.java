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

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.Track;
import lombok.Getter;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class SpotifyUtil {

    private static final Pattern TRACK_REGEX = Pattern.compile("^(https://open.spotify.com/track/)([a-zA-Z0-9]+)(.*)$");
    private static final Pattern PLAYLIST_REGEX = Pattern.compile("^(https://open.spotify.com/playlist/)([a-zA-Z0-9]+)(.*)$");

    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    @Getter
    private final SpotifyApi api;

    public SpotifyUtil(SpotifyApi api) {
        this.api = api;
        refreshAccessToken();
    }

    public boolean isTrack(String url) {
        return TRACK_REGEX.matcher(url).matches();
    }

    public boolean isAlbum(String url) {
        return PLAYLIST_REGEX.matcher(url).matches();
    }

    public Track getTrackFromUrl(String url) throws ParseException, SpotifyWebApiException, IOException {
        return getApi().getTrack(TRACK_REGEX.matcher(url).group(2)).build().execute();
    }

    public Album getAlbumFromUrl(String url) throws ParseException, SpotifyWebApiException, IOException {
        return getApi().getAlbum(PLAYLIST_REGEX.matcher(url).group(2)).build().execute();
    }

    private void refreshAccessToken() {
        try {
            ClientCredentials cr = api.clientCredentials().build().execute();
            api.setAccessToken(cr.getAccessToken());
            ses.schedule(this::refreshAccessToken, cr.getExpiresIn(), TimeUnit.SECONDS);
        } catch (Exception e) {
            ses.schedule(this::refreshAccessToken, 60, TimeUnit.SECONDS);
        }

    }

}
