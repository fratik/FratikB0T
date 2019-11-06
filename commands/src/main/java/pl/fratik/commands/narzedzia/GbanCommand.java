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

import net.dv8tion.jda.api.entities.Guild;
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
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.UserUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class GbanCommand extends Command {

    private final GbanDao gbanDao;
    private final ManagerArgumentow managerArgumentow;
    private static final String STRINGARGTYPE = "string";

    public GbanCommand(GbanDao gbanDao, ManagerArgumentow managerArgumentow) {
        this.gbanDao = gbanDao;
        this.managerArgumentow = managerArgumentow;
        name = "gban";
        category = CommandCategory.UTILITY;
        permLevel = PermLevel.GADMIN;
        LinkedHashMap<String, String> hmap = new LinkedHashMap<>();
        hmap.put("podmiot", STRINGARGTYPE);
        hmap.put("powod", STRINGARGTYPE);
        hmap.put("[...]", STRINGARGTYPE);
        uzycie = new Uzycie(hmap, new boolean[] {true, true, false});
        uzycieDelim = " ";
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String guild;
        String reason = Arrays.stream(Arrays.copyOfRange(context.getArgs(), 1, context.getArgs().length))
                .map(Object::toString).collect(Collectors.joining(uzycieDelim));
        guild = (String) context.getArgs()[0];
        User user = (User) managerArgumentow.getArguments().get("user").execute((String) context.getArgs()[0],
                context.getTlumaczenia(), context.getLanguage());
        try {
            Long.parseLong(String.valueOf(context.getArgs()[0]));
        } catch (Exception e) {
            CommonErrors.usage(context);
        }
        GbanData gdata;
        gdata = gbanDao.get(guild);
        if (user != null) gdata = gbanDao.get(user);
        if (gdata == null) throw new IllegalStateException("gdata == null");
        if (gdata.isGbanned()) {
            context.send(context.getTranslated("gban.already.gbanned"));
            return false;
        }
        if (user != null) {
            gdata.setGbanned(true);
            gdata.setIssuer(UserUtil.formatDiscrim(context.getSender()));
            gdata.setIssuerId(context.getSender().getId());
            gdata.setName(UserUtil.formatDiscrim(user));
            gdata.setReason(reason);
            gdata.setType(GbanData.Type.USER);
            gbanDao.save(gdata);
            context.send(context.getTranslated("gban.success.user", UserUtil.formatDiscrim(user)));
            return true;
        }
        gdata.setGbanned(true);
        gdata.setIssuer(UserUtil.formatDiscrim(context.getSender()));
        gdata.setIssuerId(context.getSender().getId());
        Guild serwer = context.getShardManager().getGuildById(Objects.requireNonNull(guild));
        if (serwer != null) gdata.setName(serwer.getName());
        gdata.setReason(reason);
        gdata.setType(GbanData.Type.GUILD);
        gbanDao.save(gdata);
        context.send(context.getTranslated("gban.success.guild", guild));
        return true;
    }
}
