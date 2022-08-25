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

package pl.fratik.moderation.commands;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.util.StringUtil;

import java.util.ArrayList;

public class RolaCommand extends ModerationCommand {

    private final GuildDao guildDao;

    public RolaCommand(GuildDao guildDao) {
        super(false);
        this.guildDao = guildDao;
        name = "rola";
    }

    @SubCommand(name = "przydziel", usage = "<rola:role>")
    public void przydziel(@NotNull NewCommandContext context) {
        Role rola = context.getArguments().get("rola").getAsRole();
        context.defer(true);
        GuildConfig gc = guildDao.get(context.getGuild());

        if (!gc.getUzytkownicyMogaNadacSobieTeRange().contains(rola.getId())) {
            context.sendMessage(context.getTranslated("rola.not.defined"));
            return;
        }

        Integer maxRolesSize = gc.getMaxRoliDoSamododania();
        if (maxRolesSize == null) maxRolesSize = 10;

        if (maxRolesSize > 0) {
            int memberRolesSize = (int) context.getMember().getRoles().stream().filter(r -> filtr(r, gc)).count();
            if (memberRolesSize >= maxRolesSize) {
                context.sendMessage(context.getTranslated("rola.maxroles", maxRolesSize, memberRolesSize));
                return;
            }
        }
        try {
            context.getGuild().addRoleToMember(context.getMember(), rola).complete();
            context.sendMessage(context.getTranslated("rola.success"));
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("rola.failed"));
        }
    }

    @SubCommand(name="lista")
    public void list(@NotNull NewCommandContext context) {
        ArrayList<String> strArray = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("```asciidoc\n");
        InteractionHook hook = context.defer(true);
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
            context.sendMessage(context.getTranslated("rola.list.noroles"));
            return;
        }
        sb.append("```");
        strArray.add(sb.toString());
        for (String str : strArray) hook.sendMessage(str).setEphemeral(true).queue();
    }

    private static boolean filtr(Role r, GuildConfig gc) {
        return gc.getUzytkownicyMogaNadacSobieTeRange().contains(r.getId());
    }

}
