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

package pl.fratik.arguments;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.stream.Collectors;

public class RoleArgument extends Argument {
    RoleArgument() {
        name = "role";
    }

    @Override
    public Role execute(@NotNull ArgumentContext context) {
        if (context.getGuild() == null) return null;
        List<Role> rola = context.getGuild().getRoles().stream()
                .filter(role -> context.getArg().equals(role.getName()) || context.getArg().equals(role.getId()) ||
                        context.getArg().equals(role.getAsMention())).collect(Collectors.toList());
        if (rola.size() != 1) return null;
        return rola.get(0);
    }

    @Override
    public Role execute(String argument, Tlumaczenia tlumaczenia, Language language, Guild guild) {
        if (guild == null) return null;
        List<Role> rola = guild.getRoles().stream()
                .filter(role -> argument.equals(role.getName()) || argument.equals(role.getId()) ||
                        argument.equals(role.getAsMention())).collect(Collectors.toList());
        if (rola.size() != 1) return null;
        return rola.get(0);
    }
}