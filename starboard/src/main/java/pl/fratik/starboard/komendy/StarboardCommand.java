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

package pl.fratik.starboard.komendy;

import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.starboard.StarManager;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

public class StarboardCommand extends Command {

    private final StarDataDao starDataDao;

    public StarboardCommand(StarDataDao starDataDao) {
        this.starDataDao = starDataDao;
        name = "starboard";
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.STARBOARD;
        uzycie = new Uzycie("kanal", "channel", false);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        StarsData std = starDataDao.get(context.getGuild());
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            if (std.getStarboardChannel() != null) {
                std.setStarboardChannel(null);
                starDataDao.save(std);
                context.send(context.getTranslated("starboard.unset"));
                return true;
            } else {
                CommonErrors.usage(context);
                return false;
            }
        }
        TextChannel kanal = (TextChannel) context.getArgs()[0];
        if (!StarManager.checkPermissions(kanal)) {
            context.send(context.getTranslated("starboard.noperms"));
            return false;
        }
        std.setStarboardChannel(kanal.getId());
        context.send(context.getTranslated("starboard.success"));
        starDataDao.save(std);
        return true;
    }

}
