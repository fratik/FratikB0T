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

package pl.fratik.invite.commands;

import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.invite.cache.InvitesCache;
import pl.fratik.invite.entity.InviteDao;

public abstract class AbstractInvitesCommand extends Command {
    protected final InviteDao inviteDao;
    protected final InvitesCache invitesCache;

    protected AbstractInvitesCommand(InviteDao inviteDao, InvitesCache invitesCache) {
        this.inviteDao = inviteDao;
        this.invitesCache = invitesCache;
    }

    protected boolean checkEnabled(CommandContext context) {
        if (!invitesCache.tracksInvites(context.getGuild())) {
            context.reply(context.getTranslated("invites.disabled"));
            return false;
        }
        return true;
    }
}
