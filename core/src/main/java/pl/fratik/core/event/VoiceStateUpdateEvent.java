/*
 * Copyright (C) 2019 FratikB0T Contributors
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

package pl.fratik.core.event;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.internal.JDAImpl;

@Getter
class VoiceStateUpdateEvent extends Event {
    @SuppressWarnings("squid:S00107")
    public VoiceStateUpdateEvent(JDAImpl jda, Guild guild, Long channelId, String userId, String sessionId, boolean deaf, boolean mute, boolean selfDeaf, boolean selfMute, boolean suppress) {
        super(jda);
        this.guild = guild;
        this.channelId = channelId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.deaf = deaf;
        this.mute = mute;
        this.selfDeaf = selfDeaf;
        this.selfMute = selfMute;
        this.suppress = suppress;
    }

    private final Guild guild;
    private final Long channelId;
    private final String userId;
    private final String sessionId;
    private final boolean deaf;
    private final boolean mute;
    private final boolean selfDeaf;
    private final boolean selfMute;
    private final boolean suppress;
}