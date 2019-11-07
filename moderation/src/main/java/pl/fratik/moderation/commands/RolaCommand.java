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

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.util.CommonErrors;
import pl.fratik.core.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RolaCommand extends ModerationCommand {

    private final GuildDao guildDao;

    public RolaCommand(GuildDao guildDao) {
        this.guildDao = guildDao;
        name = "rola";
        category = CommandCategory.MODERATION;
        uzycie = new Uzycie("rola", "role");
        aliases = new String[] {"ranga", "rola", "dodajrange", "usunrange", "dodajrole", "usunrole", "dajrole", "wezrole", "dajrange", "wezrange", "proszerole"};
        permissions.add(Permission.MANAGE_ROLES);
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (context.getArgs().length == 0 || context.getArgs()[0] == null) {
            CommonErrors.usage(context);
            return false;
        }
        Role rola = (Role) context.getArgs()[0];
        GuildConfig gc = guildDao.get(context.getGuild());

        if (!gc.getUzytkownicyMogaNadacSobieTeRange().contains(rola.getId())) {
            context.send(context.getTranslated("rola.not.defined"));
            return false;
        }
        try {
            Integer maxRolesSize = gc.getMaxRoliDoZamododania();
            if (maxRolesSize == null) maxRolesSize = 100;

            if (maxRolesSize == 0) {
                context.send(context.getTranslated("rola.disabled"));
                return false;
            }

            Integer memberRolesSize = (int) context.getMember().getRoles().stream().filter(r -> filtr(r, gc)).count();
            if (memberRolesSize >= maxRolesSize) {
                context.send(context.getTranslated("rola.maxroles", maxRolesSize, memberRolesSize , context.getPrefix()));
                return false;
            }

            context.getGuild().addRoleToMember(context.getMember(), rola).complete();
            context.send(context.getTranslated("rola.success"));
        } catch (Exception e) {
            context.send(context.getTranslated("rola.failed"));
            return false;
        }
        return true;
    }

    @SubCommand(name="list", aliases = "lista")
    public boolean list(@NotNull CommandContext context) {
        ArrayList<String> strArray = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("```asciidoc\n");
        GuildConfig gc = guildDao.get(context.getGuild());
        int ilosc = 0;
        for (Role role : context.getGuild().getRoles()) {
            if (!gc.getUzytkownicyMogaNadacSobieTeRange().contains(role.getId()) || role.equals(context.getGuild().getPublicRole()))
                continue;
            ilosc++;
            String rola = StringUtil.escapeMarkdown(role.getName()) + " :: " + role.getId() + "\n";
            if (sb.length() + rola.length() >= 1996) {
                sb.append("```");
                strArray.add(sb.toString());
                sb = new StringBuilder().append("```asciidoc\n").append(rola);
            } else sb.append(rola);
        }
        if (ilosc == 0) {
            context.send(context.getTranslated("rola.list.noroles"));
            return false;
        }
        sb.append("```");
        strArray.add(sb.toString());
        for (String str : strArray) {
            context.send(str);
        }
        return true;
    }

    @Override
    public PermLevel getPermLevel() {
        return PermLevel.EVERYONE;
    }

    private static boolean filtr(Role r, GuildConfig gc) {
        if (gc.getUzytkownicyMogaNadacSobieTeRange().contains(r.getId())) { return true; }
        return false;
    }

}
