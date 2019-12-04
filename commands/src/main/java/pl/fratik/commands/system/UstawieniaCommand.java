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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.commands.util.OldSettingsRenderer;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;

@SuppressWarnings("FieldCanBeLocal")
public class UstawieniaCommand extends Command {
    private final EventWaiter eventWaiter;
    private final UserDao userDao;
    private final GuildDao guildDao;
    private final ManagerArgumentow managerArgumentow;
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;

    public UstawieniaCommand(EventWaiter eventWaiter, UserDao userDao, GuildDao guildDao, ManagerArgumentow managerArgumentow, ShardManager shardManager, Tlumaczenia tlumaczenia) {
        name = "ustawienia";
        aliases = new String[] {"conf", "settings"};
        type = CommandType.MODERATION;
        category = CommandCategory.MODERATION;
        allowInDMs = false;

        this.eventWaiter = eventWaiter;
        this.userDao = userDao;
        this.guildDao = guildDao;
        this.managerArgumentow = managerArgumentow;
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        permissions.add(Permission.MESSAGE_MANAGE);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        new OldSettingsRenderer(eventWaiter, userDao, guildDao, tlumaczenia, managerArgumentow, shardManager, context).create();
        return true;
//        context.send(context.getTranslated("ustawienia.pls.use.dashboard",
//                Ustawienia.instance.botUrl + "/dashboard/" + context.getGuild().getId() + "/manage"), false);
//        return false;
    }
}
