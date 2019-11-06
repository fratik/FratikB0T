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
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

public class MemberArgument extends Argument {
    MemberArgument() {
        name = "member";
    }

    @Override
    public Member execute(@NotNull ArgumentContext context) {
        try {
            if (context.getGuild().getMemberById(context.getArg()) != null)
                return context.getGuild().getMemberById(context.getArg());
            if (!context.getEvent().getMessage().getMentionedMembers().isEmpty() && context.getEvent().getMessage().getMentionedMembers().get(0) != null)
                return context.getEvent().getMessage().getMentionedMembers().get(0);
        } catch (Exception ignored) {
            if (!context.getEvent().getMessage().getMentionedMembers().isEmpty() && context.getEvent().getMessage().getMentionedMembers().get(0) != null)
                return context.getEvent().getMessage().getMentionedMembers().get(0);
        }
        return null;
    }

    @Override
    public Object execute(String argument, Tlumaczenia tlumaczenia, Language language, Guild guild) {
        try {
            if (guild.getMemberById(argument) != null)
                return guild.getMemberById(argument);
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}