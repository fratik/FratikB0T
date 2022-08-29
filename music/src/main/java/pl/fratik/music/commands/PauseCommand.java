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

package pl.fratik.music.commands;

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;

public class PauseCommand extends MusicCommand {
    private final NowyManagerMuzyki managerMuzyki;
    private final GuildDao guildDao;

    public PauseCommand(NowyManagerMuzyki managerMuzyki, GuildDao guildDao) {
        this.managerMuzyki = managerMuzyki;
        this.guildDao = guildDao;
        name = "pause";
        requireConnection = true;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        boolean paused = mms.pause();
        if (paused) context.reply(context.getTranslated("pause.paused"));
        else context.reply(context.getTranslated("pause.resumed"));
    }

    @Override
    public boolean permissionCheck(NewCommandContext context) {
        if (!hasFullDjPerms(context.getMember(), guildDao)) {
            context.replyEphemeral(context.getTranslated("pause.dj"));
            return false;
        }
        return super.permissionCheck(context);
    }
}
