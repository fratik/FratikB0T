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

package pl.fratik.moderation.entity;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.AbstractMessage;
import net.dv8tion.jda.internal.entities.UserImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.TimeZone;

@EqualsAndHashCode(callSuper = false)
public class LogMessage extends AbstractMessage {
    @Setter private static ShardManager shardManager;
    private final long id;
    private final long guildId;
    private final long authorId;
    private final String authorAvatar; //tylko jesli webhook
    private final String authorName; //tylko jesli webhook
    private final long channelId;
    private final long createdTime;
    private final long editedTime;
    private final boolean isWebhook;

    public LogMessage(Message message) {
        super(message.getContentRaw(), message.getNonce(), message.isTTS());
        this.id = message.getIdLong();
        this.guildId = message.getGuild().getIdLong();
        this.channelId = message.getChannel().getIdLong();
        this.createdTime = message.getTimeCreated().toInstant().toEpochMilli();
        this.editedTime = message.isEdited() ? Objects.requireNonNull(message.getTimeEdited()).toInstant().toEpochMilli() : -1;
        this.isWebhook = message.isWebhookMessage();
        this.authorId = message.getAuthor().getIdLong();
        if (isWebhook) {
            authorAvatar = message.getAuthor().getAvatarId();
            authorName = message.getAuthor().getName();
        } else {
            authorAvatar = null;
            authorName = null;
        }
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
        if (isWebhook) {
            UserImpl ussr = new UserImpl(authorId, null) {
                @NotNull
                @Override
                public JDAImpl getJDA() {
                    throw new UnsupportedOperationException("fake user");
                }
            };
            ussr.setName(authorName);
            ussr.setAvatarId(authorAvatar);
            ussr.setDiscriminator("0000");
            ussr.setFake(true);
            ussr.setBot(true);
            return ussr;
        }
        return shardManager.retrieveUserById(authorId).complete();
    }

    @Nonnull
    @Override
    public Guild getGuild() {
        Guild guild = shardManager.getGuildById(guildId);
        if (guild == null) throw new IllegalStateException("bot wyrzucony!");
        return guild;
    }

    @NotNull
    @Override
    public MessageChannelUnion getChannel() {
        GuildChannel ch = shardManager.getGuildChannelById(channelId);
        if (!(ch instanceof MessageChannelUnion)) throw new IllegalStateException("kanal nie istnieje!");
        return (MessageChannelUnion) ch;
    }

    public long getAuthorId() {
        return authorId;
    }

    @NotNull
    @Override
    public OffsetDateTime getTimeCreated() {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(createdTime), TimeZone.getTimeZone("GMT").toZoneId());
    }

    @Override
    public OffsetDateTime getTimeEdited() {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(editedTime), TimeZone.getTimeZone("GMT").toZoneId());
    }

    @Override
    public boolean isEdited() {
        return editedTime != -1;
    }

    @Override
    public boolean isWebhookMessage() {
        return isWebhook;
    }
}
