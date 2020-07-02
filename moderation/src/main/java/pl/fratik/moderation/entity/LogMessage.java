/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.moderation.entity;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.entities.AbstractMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = false)
public class LogMessage extends AbstractMessage {
    @Setter private static ShardManager shardManager;
    private final long id;
    private final long guildId;
    private final long authorId;
    private final long channelId;
    private final OffsetDateTime editedTime;

    public LogMessage(Message message) {
        super(message.getContentRaw(), message.getNonce(), message.isTTS());
        this.id = message.getIdLong();
        this.guildId = message.getGuild().getIdLong();
        this.authorId = message.getAuthor().getIdLong();
        this.channelId = message.getChannel().getIdLong();
        this.editedTime = message.isEdited() ? message.getTimeEdited() : null;
    }

    @Override
    protected void unsupported() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Nullable
    @Override
    public MessageActivity getActivity() {
        return null;
    }

    @Override
    public long getIdLong() {
        return id;
    }

    @Nonnull
    @Override
    public User getAuthor() {
        return shardManager.retrieveUserById(authorId).complete();
    }

    @Nonnull
    @Override
    public Guild getGuild() {
        Guild guild = shardManager.getGuildById(guildId);
        if (guild == null) throw new IllegalStateException("bot wyrzucony!");
        return guild;
    }

    @Nonnull
    @Override
    public TextChannel getTextChannel() {
        TextChannel ch = shardManager.getTextChannelById(channelId);
        if (ch == null) throw new IllegalStateException("kanal nie istnieje!");
        return ch;
    }

    public long getAuthorId() {
        return authorId;
    }

    @Override
    public OffsetDateTime getTimeEdited() {
        return editedTime;
    }

    @Override
    public boolean isEdited() {
        return editedTime != null;
    }
}
