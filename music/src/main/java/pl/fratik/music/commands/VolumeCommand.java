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

package pl.fratik.music.commands;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;

public class VolumeCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final GuildDao guildDao;

    public VolumeCommand(NowyManagerMuzyki managerMuzyki, GuildDao guildDao) {
        this.managerMuzyki = managerMuzyki;
        this.guildDao = guildDao;
        name = "volume";
        requireConnection = true;
        uzycie = new Uzycie("glosnosc", "integer");
        aliases = new String[] {"vol"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
            context.send(context.getTranslated("volume.dj"));
            return false;
        }
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            context.send(context.getTranslated("volume.get", mms.getVolume() + "%"));
            return false;
        }
        Integer glosnosc = (Integer) context.getArgs()[0];
        if (glosnosc < 1 || glosnosc > 150) {
            context.send(context.getTranslated("volume.limit"));
            return false;
        }
        mms.setVolume(glosnosc);
        context.send(context.getTranslated("volume.success", ((Integer) context.getArgs()[0]).toString() + "%"));
        return true;
    }
}
