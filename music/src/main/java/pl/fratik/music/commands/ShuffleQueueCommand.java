/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;

public class ShuffleQueueCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final GuildDao guildDao;

    public ShuffleQueueCommand(NowyManagerMuzyki managerMuzyki, GuildDao guildDao) {
        name = "shuffle";
        aliases = new String[] {"shufflequeue", "mieszajkolejka", "randomqueue"};
        requireConnection = true;
        this.managerMuzyki = managerMuzyki;
        this.guildDao = guildDao;
    }

    @Override
    public boolean execute(CommandContext context) {
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
            context.send(context.getTranslated("volume.dj"));
            return false;
        }

        if (mms.getKolejka().isEmpty()) {
            context.send(context.getTranslated("shufflequeue.queueempty"));
            return false;
        }

        mms.shuffleQueue();
        context.send(context.getTranslated("shufflequeue.success"));

        return true;
    }

}
