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
class VoiceServerUpdateEvent extends Event {
    private final String token;
    private final Guild guild;
    private final String endpoint;
    private final String sessionId;

    public VoiceServerUpdateEvent(JDAImpl jda, String token, Guild guild, String endpoint, String sessionId) {
        super(jda);
        this.token = token;
        this.guild = guild;
        this.endpoint = endpoint;
        this.sessionId = sessionId;
    }
}
