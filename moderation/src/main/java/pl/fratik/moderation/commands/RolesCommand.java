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

import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.util.StringUtil;

import java.util.ArrayList;

public class RolesCommand extends ModerationCommand {

    public RolesCommand() {
        name = "roles";
        category = CommandCategory.MODERATION;
        aliases = new String[] {"listarol", "listarang"};
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        ArrayList<String> strArray = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("```asciidoc\n");
        for (Role role : context.getGuild().getRoles()) {
            if (role.equals(context.getGuild().getPublicRole())) continue;
            String rola = StringUtil.escapeMarkdown(role.getName()) + " :: " + role.getId() + "\n";
            if (sb.length() + rola.length() >= 1996) {
                sb.append("```");
                strArray.add(sb.toString());
                sb = new StringBuilder().append("```asciidoc\n").append(rola);
            } else sb.append(rola);
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

}
