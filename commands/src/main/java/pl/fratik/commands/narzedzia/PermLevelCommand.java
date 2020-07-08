/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.PermLevel;

public class PermLevelCommand extends Command {

    public PermLevelCommand() {
        name = "permlevel";
        category = CommandCategory.BASIC;
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public boolean execute(CommandContext context) {
        EmbedBuilder eb = context.getBaseEmbed();
        StringBuilder sb = new StringBuilder();
        sb.append("```asciidoc" + "\n");
        for (PermLevel v : PermLevel.values()) {
            sb.append(context.getTranslated(v.getLanguageKey()) + " :: " +
                    context.getTranslated("permlevel.lvl") + " " + v.getNum() + "\n");
        }
        sb.append("```");
        eb.setDescription(sb.toString());
        context.send(eb.build());
        return true;
    }

}
