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
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.music.entity.RepeatMode;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;

import java.util.List;

public class SkipCommand extends MusicCommand {

    private final NowyManagerMuzyki managerMuzyki;
    private final GuildDao guildDao;

    public SkipCommand(NowyManagerMuzyki managerMuzyki, GuildDao guildDao) {
        this.managerMuzyki = managerMuzyki;
        this.guildDao = guildDao;
        name = "skip";
        requireConnection = true;
        aliases = new String[] {"pomin", "pass", "passer"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms.getRepeatMode() != RepeatMode.OFF) {
            context.send(context.getTranslated("skip.on.repeat"));
            return false;
        }
        synchronized (context.getGuild()) {
            if (mms.getChannel().getMembers().size() > 4) {
                List<String> skips = mms.getSkips();
                if (skips.contains(context.getSender().getId())) {
                    context.send(context.getTranslated("skip.already.voted"));
                    return false;
                }
                skips.add(context.getSender().getId());
                int total = mms.getChannel().getMembers().size() - 1;
                int size = skips.size();
                if (size < total * 0.4) {
                    context.send("\uD83D\uDD38 | " + context.getTranslated("skip.votes", size, (int) Math.ceil(total * 0.4)));
                    return true;
                }
            } else if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
                context.send(context.getTranslated("skip.dj"));
                return false;
            }
            context.send(context.getTranslated("skip.success"));
            mms.skip();
        }
        return true;
    }

    @SubCommand(name = "force", aliases = {"wymus", "-f"})
    public boolean force(@NotNull CommandContext context) {
        if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
            context.send(context.getTranslated("skip.forced.error"));
            return false;
        }
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        if (mms.getRepeatMode() != RepeatMode.OFF) {
            context.send(context.getTranslated("skip.on.repeat"));
            return false;
        }
        context.send(context.getTranslated("skip.success.forced"));
        mms.skip();
        return true;
    }
}
