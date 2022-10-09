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
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import lombok.Getter;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.entity.SpotifyConfig;
import pl.fratik.core.entity.SpotifyDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class UserCredentials {

    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    private final String discordId;

    private final SpotifyApi api;
    private final SpotifyDao dao;

    public UserCredentials(String discordId, String accessToken, String refreshToken, SpotifyDao dao) {
        this.discordId = discordId;
        this.api = SpotifyApi.builder()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken).build();
        this.dao = dao;
        refreshAccessToken();
    }

    // FIXME: Nie trzeba cały czas odświeżać. Przy użyciu tokena można sprawdzić czy jest on przedawniony - jeżeli tak, odśwież go
    public void refreshAccessToken() {
        try {
            AuthorizationCodeCredentials cr = api.authorizationCodeRefresh(Ustawienia.instance.apiKeys.get("spotifyId"), Ustawienia.instance.apiKeys.get("spotifySecret"), api.getRefreshToken()).build().execute();
            api.setAccessToken(cr.getAccessToken());
            ses.schedule(this::refreshAccessToken, cr.getExpiresIn() - 120, TimeUnit.SECONDS);

            SpotifyConfig config = dao.get(discordId);
            if (config != null) { // trudno, nie będziemy zapisywać
                config.setAccessToken(cr.getAccessToken());
                dao.save(config);
            }

        } catch (Exception e) {
            ses.schedule(this::refreshAccessToken, 60, TimeUnit.SECONDS);
            e.printStackTrace();
        }
    }

}
