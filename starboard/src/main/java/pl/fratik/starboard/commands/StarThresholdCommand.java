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
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.starboard.entity.StarDataDao;
import pl.fratik.starboard.entity.StarsData;

public class StarThresholdCommand extends NewCommand {

    private final StarDataDao starDataDao;

    public StarThresholdCommand(StarDataDao starDataDao) {
        this.starDataDao = starDataDao;
        name = "starthreshold";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
        usage = "<ilosc:integer>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        StarsData std = starDataDao.get(context.getGuild());
        int ilosc = context.getArguments().get("ilosc").getAsInt();

        if (ilosc >= 0) {
            context.replyEphemeral("starthreshold.ilosc.error");
            return;
        }

        std.setStarThreshold(ilosc);
        context.replyEphemeral(context.getTranslated("starthreshold.set", ilosc));
        starDataDao.save(std);
    }

}
