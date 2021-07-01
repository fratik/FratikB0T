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

import com.wrapper.spotify.model_objects.specification.Track;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.core.util.UserUtil;
import pl.fratik.music.utils.SpotifyUtil;
import pl.fratik.music.utils.UserCredentials;

import java.time.Instant;

public class SpotifyStatsCommand extends Command {

    private final SpotifyUtil spotifyUtil;

    public SpotifyStatsCommand(SpotifyUtil spotifyUtil) {
        name = "spotifystats";
        aliases = new String[] {"spotify"};
        permLevel = PermLevel.GADMIN;
        this.spotifyUtil = spotifyUtil;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        UserCredentials user = spotifyUtil.getUser(context.getSender().getId());

        if (user == null) {
            EmbedBuilder eb = context.getBaseEmbed();
            eb.setColor(UserUtil.getPrimColor(context.getMember().getUser()));
            eb.setTimestamp(Instant.now());
            String link = String.format("https://accounts.spotify.com/authorize?response_type=code&client_id=%s&scope=user-top-read&redirect_uri=%s/api/spotify/callback", spotifyUtil.getApi().getClientId(), Ustawienia.instance.botUrl);
            eb.setDescription("Aby zobaczyć swoje statystyki Spotify musisz połączyć swoje konto Discord z kontem Spotify! " +
                    "Możesz to zrobić wchodząc " + String.format("[tutaj](%s)", link));
            context.reply(eb.build());
            return false;
        }

        try {
            Track tracks = user.getApi().getUsersTopTracks().time_range("short_term").limit(10).build().execute().getItems()[0];
            context.send(GsonUtil.toJSON(tracks));
            return true;
        } catch (Exception e) {
            context.reply("Wystąpił błąd!");
            e.printStackTrace();
        }

        return true;
    }


}
