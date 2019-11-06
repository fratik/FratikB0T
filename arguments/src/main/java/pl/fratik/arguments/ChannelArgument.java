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
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.List;
import java.util.stream.Collectors;

public class ChannelArgument extends Argument {
    ChannelArgument() {
        name = "channel";
    }

    @Override
    public TextChannel execute(@NotNull ArgumentContext context) {
        if (context.getGuild() == null) return null;
        List<TextChannel> tchannel = context.getGuild().getTextChannels().stream()
                .filter(channel -> context.getArg().equals(channel.getName()) || context.getArg().equals(channel.getId()) ||
                        context.getArg().equals(channel.getAsMention())).collect(Collectors.toList());
        if (tchannel.size() != 1) return null;
        return tchannel.get(0);
    }

    @Override
    public TextChannel execute(String argument, Tlumaczenia tlumaczenia, Language language, Guild guild) {
        if (guild == null) return null;
        List<TextChannel> tchannel = guild.getTextChannels().stream()
                .filter(channel -> argument.equals(channel.getName()) || argument.equals(channel.getId()) ||
                        argument.equals(channel.getAsMention())).collect(Collectors.toList());
        if (tchannel.size() != 1) return null;
        return tchannel.get(0);
    }
}