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

import net.dv8tion.jda.api.entities.Guild;
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

import java.util.Objects;

public class GbanCommand extends NewCommand {

    private final GbanDao gbanDao;
    private final ManagerArgumentow managerArgumentow;

    public GbanCommand(GbanDao gbanDao, ManagerArgumentow managerArgumentow) {
        this.gbanDao = gbanDao;
        this.managerArgumentow = managerArgumentow;
        name = "gban";
        type = CommandType.SUPPORT_SERVER;
        usage = "<podmiot:string> <powod:string>";
        permissions = DefaultMemberPermissions.DISABLED;
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!UserUtil.isStaff(context.getSender(), context.getShardManager())) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return;
        }
        String guild = context.getArguments().get("podmiot").getAsString();
        String reason = context.getArguments().get("powod").getAsString();
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
        gdata = gbanDao.get(guild);
        if (user != null) gdata = gbanDao.get(user);
        if (gdata == null) throw new IllegalStateException("gdata == null");
        if (gdata.isGbanned()) {
            context.sendMessage(context.getTranslated("gban.already.gbanned"));
            return;
        }
        if (user != null) {
            gdata.setGbanned(true);
            gdata.setIssuer(UserUtil.formatDiscrim(context.getSender()));
            gdata.setIssuerId(context.getSender().getId());
            gdata.setName(UserUtil.formatDiscrim(user));
            gdata.setReason(reason);
            gdata.setType(GbanData.Type.USER);
            gbanDao.save(gdata);
            context.sendMessage(context.getTranslated("gban.success.user", UserUtil.formatDiscrim(user)));
            return;
        }
        gdata.setGbanned(true);
        gdata.setIssuer(UserUtil.formatDiscrim(context.getSender()));
        gdata.setIssuerId(context.getSender().getId());
        Guild serwer = context.getShardManager().getGuildById(Objects.requireNonNull(guild));
        if (serwer != null) gdata.setName(serwer.getName());
        gdata.setReason(reason);
        gdata.setType(GbanData.Type.GUILD);
        gbanDao.save(gdata);
        context.sendMessage(context.getTranslated("gban.success.guild", guild));
    }
}
