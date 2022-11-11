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

import lavalink.client.io.Link;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class CustomVoiceInterceptor implements VoiceDispatchInterceptor {
    private final CustomLavalink lavalink;

    public CustomVoiceInterceptor(CustomLavalink lavalink) {
        this.lavalink = lavalink;
    }

    @Override
    public void onVoiceServerUpdate(@Nonnull VoiceServerUpdate update) {
        JSONObject content = new JSONObject(update.toData().getObject("d").toMap());

        // Get session
        Guild guild = update.getGuild();

        lavalink.getLink(guild).onVoiceServerUpdate(content, guild.getSelfMember().getVoiceState().getSessionId());
    }

    @Override
    public boolean onVoiceStateUpdate(@Nonnull VoiceStateUpdate update) {

        AudioChannel channel = update.getChannel();
        CustomLink link = lavalink.getLink(update.getGuildId());

        if (channel == null) {
            // Null channel means disconnected
            if (link.getState() != Link.State.DESTROYED) {
                link.onDisconnected();
            }
        } else {
            link.setChannel(channel.getId()); // Change expected channel
        }

        return link.getState() == Link.State.CONNECTED;
    }
}
