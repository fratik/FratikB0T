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

package pl.fratik.core.command;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;

public abstract class NsfwCommand extends Command {
    @Override
    public boolean preExecute(CommandContext context) {
        boolean isNsfw;
        if (context.getMessageChannel().getType() == ChannelType.TEXT) isNsfw = context.getTextChannel().isNSFW();
        else if (context.getMessageChannel() instanceof ThreadChannel &&
                ((ThreadChannel) context.getMessageChannel()).getParentChannel() instanceof TextChannel)
            isNsfw = ((TextChannel) ((ThreadChannel) context.getMessageChannel()).getParentChannel()).isNSFW();
        else isNsfw = false;
        if (!isNsfw) {
            context.reply(context.getTranslated("generic.not.nsfw"));
            return false;
        }
        return super.preExecute(context);
    }
}
