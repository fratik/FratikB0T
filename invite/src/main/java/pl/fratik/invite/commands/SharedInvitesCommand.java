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

package pl.fratik.invite.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteDao;

import java.time.Instant;
import java.util.Map;

public abstract class SharedInvitesCommand extends AbstractInvitesCommand {
    protected final GuildDao guildDao;
    protected final EventWaiter eventWaiter;
    protected final EventBus eventBus;

    protected SharedInvitesCommand(InviteDao inviteDao, InvitesCache invitesCache, GuildDao guildDao, EventWaiter eventWaiter, EventBus eventBus) {
        super(inviteDao, invitesCache);
        this.guildDao = guildDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
    }

    @NotNull
    protected EmbedBuilder renderEmbed(@NotNull NewCommandContext context, StringBuilder sb, Map<Role, Integer> sorted, Instant now) {
        return new EmbedBuilder()
                .setAuthor(context.getTranslated("invites.roles", sorted.size()))
                .setColor(UserUtil.getPrimColor(context.getSender()))
                .setDescription(sb.toString())
                .setTimestamp(now);
    }
}
