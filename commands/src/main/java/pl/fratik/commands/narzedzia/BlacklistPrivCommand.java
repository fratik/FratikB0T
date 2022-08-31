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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.util.UserUtil;

public class BlacklistPrivCommand extends NewCommand {
    private final UserDao userDao;

    public BlacklistPrivCommand(UserDao userDao) {
        this.userDao = userDao;
        name = "blacklistpriv";
        usage = "<osoba:user>";
        permissions = DefaultMemberPermissions.DISABLED;
        type = CommandType.SUPPORT_SERVER;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!UserUtil.isStaff(context.getSender(), context.getShardManager())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return;
        }
        User ignore = context.getArguments().get("osoba").getAsUser();
        UserConfig uc = userDao.get(ignore);
        uc.setPrivBlacklist(true);
        userDao.save(uc);
        context.reply(context.getTranslated("blacklistpriv.success"));

    }
}
