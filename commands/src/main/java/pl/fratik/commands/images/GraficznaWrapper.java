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

package pl.fratik.commands.images;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GraficznaWrapper extends NewCommand {
    private final Set<NewCommand> commands;

    public GraficznaWrapper(Set<NewCommand> commands) {
        this.commands = commands;
        name = "zdjecia";
    }

    public void xDxDxD(NewCommandContext context) {
        commands.stream()
                .filter(c -> c.getName().equals(context.getCommandPath().substring(name.length() + 1)))
                .findFirst().orElseThrow().execute(context);
    }

    @Override
    public CommandData generateCommandData(Tlumaczenia tlumaczenia) {
        SlashCommandData data = generateBasicCommandData(tlumaczenia);
        List<SubcommandData> subcommands = new ArrayList<>();
        for (NewCommand command : commands) {
            SlashCommandData commandData = (SlashCommandData) command.generateCommandData(tlumaczenia);
            subcommands.add(new SubcommandData(commandData.getName(), commandData.getDescription())
                    .setNameLocalizations(commandData.getNameLocalizations().toMap())
                    .setDescriptionLocalizations(commandData.getDescriptionLocalizations().toMap())
                    .addOptions(commandData.getOptions())
            );
        }
        data.addSubcommands(subcommands);
        return data;
    }

    @Override
    public void registerSubcommands() {
        subcommands.clear();
        for (NewCommand command : commands) {
            try {
                subcommands.put(command.getName(), getClass().getDeclaredMethod("xDxDxD", NewCommandContext.class));
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
