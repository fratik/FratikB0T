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
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.music.entity.RepeatMode;
import pl.fratik.music.managers.ManagerMuzykiSerwera;
import pl.fratik.music.managers.NowyManagerMuzyki;

public class RepeatCommand extends MusicCommand {
    private final NowyManagerMuzyki managerMuzyki;
    private final ManagerArgumentow managerArgumentow;
    private final GuildDao guildDao;
    private RepeatModeArgument arg;

    public RepeatCommand(NowyManagerMuzyki managerMuzyki, ManagerArgumentow managerArgumentow, GuildDao guildDao) {
        this.managerMuzyki = managerMuzyki;
        this.managerArgumentow = managerArgumentow;
        this.guildDao = guildDao;
        name = "repeat";
        requireConnection = true;
        aliases = new String[] {"loop"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!hasFullDjPerms(context.getMember(), context.getShardManager(), guildDao)) {
            context.send(context.getTranslated("repeat.dj"));
            return false;
        }
        ManagerMuzykiSerwera mms = managerMuzyki.getManagerMuzykiSerwera(context.getGuild());
        mms.setRepeatMode((RepeatMode) context.getArgs()[0]);
        context.send(context.getTranslated("repeat.set"));
        return true;
    }

    @Override
    public void onRegister() {
        arg = new RepeatModeArgument();
        managerArgumentow.registerArgument(arg);
        uzycie = new Uzycie("tryb", "repeatmode", true);
    }

    @Override
    public void onUnregister() {
        try {managerArgumentow.unregisterArgument(arg);} catch (Exception e) {/*lul*/}
        arg = null;
    }

    protected static class RepeatModeArgument extends Argument {
        RepeatModeArgument() {
            this.name = "repeatmode";
        }

        @Override
        public RepeatMode execute(ArgumentContext context) {
            if (context.getArg().equalsIgnoreCase("once") ||
                    context.getArg().equalsIgnoreCase(context.getTlumaczenia()
                            .get(context.getLanguage(), "repeat.mode.once"))) return RepeatMode.ONCE;
            if (context.getArg().equalsIgnoreCase("off") ||
                    context.getArg().equalsIgnoreCase(context.getTlumaczenia()
                            .get(context.getLanguage(), "repeat.mode.off"))) return RepeatMode.OFF;
            return null;
        }

    }

}
