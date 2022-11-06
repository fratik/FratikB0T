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

package pl.fratik.starboard.event;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;
import pl.fratik.starboard.entity.StarData;

@Getter
public class StarEvent {
    @Nullable private final User user;
    private final Message message;
    private final int gwiazdki;
    private final MessageChannel channel;
    private final GuildMessageChannel starboardChannel;
    private final String starboardMessageId;
    @Setter
    private boolean cancelled;

    public StarEvent(@Nullable User user, Message message, int gwiazdki, MessageChannel channel, GuildMessageChannel starboardChannel, String starboardMessageId) {
        this.user = user;
        this.message = message;
        this.gwiazdki = gwiazdki;
        this.channel = channel;
        this.starboardChannel = starboardChannel;
        this.starboardMessageId = starboardMessageId;
    }

    public StarEvent(@Nullable User user, Message message, int gwiazdki, MessageChannel channel, String starboardChannel, String starboardMessageId) {
        this.user = user;
        this.message = message;
        this.gwiazdki = gwiazdki;
        this.channel = channel;
        if (starboardChannel != null) this.starboardChannel = message.getGuild().getTextChannelById(starboardChannel);
        else this.starboardChannel = null;
        this.starboardMessageId = starboardMessageId;
    }

    public StarEvent(@Nullable User user, Message message, int gwiazdki, MessageChannel channel, String starboardChannel, Message starboardMessage) {
        this.user = user;
        this.message = message;
        this.gwiazdki = gwiazdki;
        this.channel = channel;
        if (starboardChannel != null) this.starboardChannel = message.getGuild().getTextChannelById(starboardChannel);
        else this.starboardChannel = null;
        this.starboardMessageId = starboardMessage.getId();
    }

    public StarEvent(@Nullable User user, Message message, StarData starData, GuildMessageChannel starboardChannel) {
        this.user = user;
        this.message = message;
        this.gwiazdki = starData.getStarredBy().size();
        this.channel = message.getChannel();
        this.starboardChannel = starboardChannel;
        this.starboardMessageId = starData.getStarboardMessageId();
    }

    public StarEvent(@Nullable User user, Message message, StarData starData, String starboardChannel) {
        this.user = user;
        this.message = message;
        this.gwiazdki = starData.getStarredBy().size();
        this.channel = message.getChannel();
        if (starboardChannel != null) this.starboardChannel = message.getGuild().getTextChannelById(starboardChannel);
        else this.starboardChannel = null;
        this.starboardMessageId = starData.getStarboardMessageId();
    }
}
