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
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.entity.GbanDao;
import pl.fratik.core.entity.GbanData;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.UserUtil;

public class UngbanCommand extends Command {

    private final GbanDao gbanDao;
    private final ManagerArgumentow managerArgumentow;

    public UngbanCommand(GbanDao gbanDao, ManagerArgumentow managerArgumentow) {
        this.gbanDao = gbanDao;
        this.managerArgumentow = managerArgumentow;
        name = "ungban";
        category = CommandCategory.UTILITY;
        permLevel = PermLevel.GADMIN;
        uzycie = new Uzycie("podmiot", "string", true);
        uzycieDelim = " ";
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String guild;
        guild = (String) context.getArgs()[0];
        User user = (User) managerArgumentow.getArguments().get("user").execute((String) context.getArgs()[0],
                context.getTlumaczenia(), context.getLanguage());
        GbanData gdata;
        gdata = gbanDao.get(guild);
        if (user != null) gdata = gbanDao.get(user);
        if (gdata == null) throw new IllegalStateException("gdata == null");
        if (!gdata.isGbanned()) {
            context.send(context.getTranslated("ungban.no.gban"));
            return false;
        }
        if (user != null) {
            gdata.setGbanned(false);
            gdata.setIssuer(null);
            gdata.setIssuerId(null);
            gdata.setName(null);
            gdata.setReason(null);
            gbanDao.save(gdata);
            context.send(context.getTranslated("ungban.success.user", UserUtil.formatDiscrim(user)));
            return true;
        }
        gdata.setGbanned(false);
        gdata.setIssuer(null);
        gdata.setIssuerId(null);
        gdata.setName(null);
        gdata.setReason(null);
        gbanDao.save(gdata);
        context.send(context.getTranslated("ungban.success.guild", guild));
        return true;
    }
}
