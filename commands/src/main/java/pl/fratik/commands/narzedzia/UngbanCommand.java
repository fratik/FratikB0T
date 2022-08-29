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
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.entity.GbanDao;
import pl.fratik.core.entity.GbanData;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.util.UserUtil;

public class UngbanCommand extends NewCommand {

    private final GbanDao gbanDao;
    private final ManagerArgumentow managerArgumentow;

    public UngbanCommand(GbanDao gbanDao, ManagerArgumentow managerArgumentow) {
        this.gbanDao = gbanDao;
        this.managerArgumentow = managerArgumentow;
        name = "ungban";
        permissions = DefaultMemberPermissions.DISABLED;
        type = CommandType.SUPPORT_SERVER;
        usage = "<podmiot:string>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        String guild = context.getArguments().get("podmiot").getAsString();
        context.deferAsync(false);
        User user;
        try {
            long id = Long.parseLong(guild);
            user = context.getShardManager().retrieveUserById(id)
                    .onErrorMap(ErrorResponse.UNKNOWN_USER::test, x -> null).complete();
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("gban.invalid.id"));
            return;
        }
        GbanData gdata;
        if (user != null) gdata = gbanDao.get(user);
        else gdata = gbanDao.get(guild);
        if (gdata == null) throw new IllegalStateException("gdata == null");
        if (!gdata.isGbanned()) {
            context.sendMessage(context.getTranslated("ungban.no.gban"));
            return;
        }
        gdata.setGbanned(false);
        gdata.setIssuer(null);
        gdata.setIssuerId(null);
        gdata.setName(null);
        gdata.setReason(null);
        gbanDao.save(gdata);
        if (user != null) context.sendMessage(context.getTranslated("ungban.success.user", UserUtil.formatDiscrim(user)));
        else context.sendMessage(context.getTranslated("ungban.success.guild", guild));
    }
}
