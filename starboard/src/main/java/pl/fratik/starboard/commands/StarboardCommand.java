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

package pl.fratik.starboard.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.starboard.StarManager;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

public class StarboardCommand extends NewCommand {

    private final StarDataDao starDataDao;

    public StarboardCommand(StarDataDao starDataDao) {
        this.starDataDao = starDataDao;
        name = "starboard";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    @SubCommand(name = "usun")
    public void usun(NewCommandContext context) {
        context.defer(true);
        StarsData std = starDataDao.get(context.getGuild());
        std.setStarboardChannel(null);
        starDataDao.save(std);
        context.sendMessage(context.getTranslated("starboard.unset"));
    }

    @SubCommand(name = "ustaw", usage = "<kanal:textchannel>")
    public void ustaw(NewCommandContext context) {
        context.defer(true);
        StarsData std = starDataDao.get(context.getGuild());
        GuildChannelUnion kanal = context.getArguments().get("kanal").getAsChannel();

        if (!(kanal instanceof GuildMessageChannel)) {
            context.sendMessage(context.getTranslated("starboard.not.msgchannel", kanal.getAsMention()));
            return;
        }

        GuildMessageChannel messageChannel = (GuildMessageChannel) kanal;

        if (!StarManager.checkPermissions(messageChannel)) {
            context.sendMessage(context.getTranslated("starboard.noperms"));
            return;
        }

        std.setStarboardChannel(kanal.getId());
        context.sendMessage(context.getTranslated("starboard.success"));
        starDataDao.save(std);
    }
}
