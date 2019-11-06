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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.entity.UserConfig;
import pl.fratik.core.entity.UserDao;
import pl.fratik.core.entity.Uzycie;

public class IgnoreCommand extends Command {
    private final UserDao userDao;

    public IgnoreCommand(UserDao userDao) {
        this.userDao = userDao;
        name = "ignore";
        aliases = new String[] {"unignore"};
        category = CommandCategory.UTILITY;
        cooldown = 15;
        uzycie = new Uzycie("osoba", "user", true);
        aliases = new String[] {"ignoruj", "wezgoodemnie"};
        uzycieDelim = " ";
        allowInDMs = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        User ignore = (User) context.getArgs()[0];
        UserConfig uc = userDao.get(context.getSender());
        if (uc.getPrivIgnored().contains(ignore.getId())) {
            uc.getPrivIgnored().remove(ignore.getId());
            userDao.save(uc);
            context.send(context.getTranslated("ignore.unignored"));
            return false;
        }
        uc.getPrivIgnored().add(ignore.getId());
        userDao.save(uc);
        context.send(context.getTranslated("ignore.ignored"));
        return true;
    }
}
