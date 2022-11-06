/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.core.util;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.EnumSet;
import java.util.Set;

public class CommandUtil {

    private CommandUtil() {}

    @NotNull
    public static OptionData[] generateOptionData(NewCommand command, String subcommandGroupName, String subcommandName, String usage, Tlumaczenia tlumaczenia) {
        if (usage.isEmpty()) return new OptionData[0];
        String[] splat = usage.split(" ");
        OptionData[] options = new OptionData[splat.length];
        for (int i = 0; i < splat.length; i++) {
            String s = splat[i];
            boolean required;
            if (s.startsWith("<") && s.endsWith(">")) required = true;
            else if (s.startsWith("[") && s.endsWith("]")) required = false;
            else throw new IllegalArgumentException("Invalid argument " + s);
            s = s.substring(1, s.length() - 1);
            String[] argument = s.split(":");
            if (argument.length != 2) throw new IllegalArgumentException("Invalid argument " + s);
            String name = argument[0];
            String type = argument[1];
            boolean autoComplete;
            if (type.endsWith("!")) {
                autoComplete = true;
                type = type.substring(0, type.length() - 1);
            } else autoComplete = false;
            OptionType optionType;
            switch (type) {
                case "str":
                case "string":
                    optionType = OptionType.STRING;
                    break;
                case "int":
                case "integer":
                    optionType = OptionType.INTEGER;
                    break;
                case "bool":
                case "boolean":
                    optionType = OptionType.BOOLEAN;
                    break;
                case "user":
                    optionType = OptionType.USER;
                    break;
                case "channel":
                case "textchannel":
                case "voicechannel":
                    optionType = OptionType.CHANNEL;
                    break;
                case "role":
                    optionType = OptionType.ROLE;
                    break;
                case "number":
                case "double":
                    optionType = OptionType.NUMBER;
                    break;
                case "attachment":
                    optionType = OptionType.ATTACHMENT;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid type " + type);
            }
            String keyBase = getAsKey(command.getName());
            if (subcommandGroupName != null) keyBase += "." + getAsKey(subcommandGroupName);
            if (subcommandName != null) keyBase += "." + getAsKey(subcommandName);
            keyBase += "." + name;
            String translatedDescription = tlumaczenia.get(Language.DEFAULT, keyBase + ".description");
            options[i] = new OptionData(optionType, name, translatedDescription, required, autoComplete);
            if (optionType == OptionType.CHANNEL) {
                EnumSet<ChannelType> types = EnumSet.noneOf(ChannelType.class);
                if (type.equals("textchannel")) types.addAll(Set.of(ChannelType.TEXT, ChannelType.NEWS));
                if (type.equals("voicechannel")) types.add(ChannelType.VOICE);
                options[i].setChannelTypes(types);
            }
            command.updateOptionData(options[i]);
        }
        return options;
    }

    @NotNull
    public static String getAsKey(String text) {
        return text.toLowerCase().replace(" ", "_");
    }
}
