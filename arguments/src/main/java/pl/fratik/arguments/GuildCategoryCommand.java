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

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.stream.Collectors;

public class GuildCategoryArgument extends Argument {

    public GuildCategoryCommand() {
        name = "guildcategory";
    }

    @Override
    public Category execute(@NotNull ArgumentContext context) {
        if (context.getGuild() == null) return null;
        List<Category> cat = context.getGuild().getCategories().stream()
                .filter(category -> context.getArg().equals(category.getName())
                        || context.getArg().equals(category.getId())).collect(Collectors.toList());
        if (cat.size() != 1) return null;
        return cat.get(0);
    }

    @Override
    public Category execute(String argument, Tlumaczenia tlumaczenia, Language language, Guild guild) {
        if (guild == null) return null;
        List<Category> cat = guild.getCategories().stream()
                .filter(category -> argument.equals(category.getName())
                        || argument.equals(category.getId())).collect(Collectors.toList());
        if (cat.size() != 1) return null;
        return cat.get(0);
    }

}
