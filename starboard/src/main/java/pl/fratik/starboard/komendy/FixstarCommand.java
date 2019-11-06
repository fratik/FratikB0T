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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.starboard.StarManager;
import pl.fratik.starboard.entity.StarDataDao;

public class FixstarCommand extends Command {

    private final StarManager starManager;
    private final StarDataDao starDataDao;

    public FixstarCommand(StarManager starManager, StarDataDao starDataDao) {
        this.starManager = starManager;
        this.starDataDao = starDataDao;
        name = "fixstar";
        category = CommandCategory.STARBOARD;
        uzycie = new Uzycie("wiadomosc", "message", true);
        permissions.add(Permission.MESSAGE_HISTORY);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        starManager.fixStars((Message) context.getArgs()[0], starDataDao.get(context.getGuild()));
        context.send(context.getTranslated("fixstar.success"));
        return true;
    }
}
