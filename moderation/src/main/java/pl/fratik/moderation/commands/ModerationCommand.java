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

package pl.fratik.moderation.commands;

import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.moderation.listeners.ModLogListener;

abstract class ModerationCommand extends NewCommand {
    protected final boolean needsPerms;

    protected ModerationCommand(boolean needsPerms) {
        this.needsPerms = needsPerms;
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (needsPerms && !ModLogListener.checkPermissions(context.getGuild())) {
            context.replyEphemeral(context.getTranslated("moderation.bot.no.permissions"));
            return false;
        }
        return true;
    }
}
