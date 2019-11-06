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

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

public class StarThresholdCommand extends Command {

    private final StarDataDao starDataDao;

    public StarThresholdCommand(StarDataDao starDataDao) {
        this.starDataDao = starDataDao;
        name = "starthreshold";
        permLevel = PermLevel.ADMIN;
        category = CommandCategory.STARBOARD;
        uzycie = new Uzycie("ilosc", "integer", true);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        StarsData std = starDataDao.get(context.getGuild());
        int ilosc = (int) context.getArgs()[0];
        std.setStarThreshold(ilosc);
        context.send(context.getTranslated("starthreshold.set", ilosc));
        starDataDao.save(std);
        return true;
    }

}
