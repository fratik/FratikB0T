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

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.starboard.StarManager;
import pl.fratik.starboard.entity.StarDataDao;

public class FixstarCommand extends NewCommand {

    private final StarManager starManager;
    private final StarDataDao starDataDao;

    public FixstarCommand(StarManager starManager, StarDataDao starDataDao) {
        this.starManager = starManager;
        this.starDataDao = starDataDao;
        name = "fixstar";
        usage = "<id_wiadomosci:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        context.defer(true);
        Message message;
        try {
            message = context.getChannel().retrieveMessageById(context.getArguments().get("id_wiadomosci").getAsString()).complete();
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("fixstar.error"));
            return;
        }
        starManager.fixStars(message, starDataDao.get(context.getGuild()));
        context.sendMessage(context.getTranslated("fixstar.success"));
    }
}
