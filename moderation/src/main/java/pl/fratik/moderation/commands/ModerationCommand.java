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

import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.moderation.listeners.ModLogListener;

abstract class ModerationCommand extends Command {
    protected final boolean needsPerms;

    protected ModerationCommand(boolean needsPerms) {
        this.needsPerms = needsPerms;
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public PermLevel getPermLevel() {
        return PermLevel.MOD;
    }

    @Override
    public boolean isIgnoreGaPerm() {
        return true;
    }

    @Override
    public boolean isAllowPermLevelEveryone() {
        return false;
    }

    @Override
    public boolean preExecute(CommandContext context) {
        if (!context.isDirect() && !context.canTalk()) return false;
        if (needsPerms && !ModLogListener.checkPermissions(context.getGuild())) {
            context.reply(context.getTranslated("moderation.bot.no.permissions"));
            return false;
        }
        return super.preExecute(context);
    }
}
