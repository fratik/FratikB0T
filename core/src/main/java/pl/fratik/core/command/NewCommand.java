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

package pl.fratik.core.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommandUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class NewCommand {
    @Getter protected String name;
    @Getter protected String usage;
    @Getter protected DefaultMemberPermissions permissions = DefaultMemberPermissions.ENABLED;
    @Getter protected CommandType type = CommandType.NORMAL;
    @Getter protected boolean allowInDMs = false;
    @Getter protected int cooldown;
    @Getter protected final Map<String, Method> subcommands = new HashMap<>();

    public void execute(NewCommandContext context) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void onRegister() {}

    public void onUnregister() {}

    public boolean hasSubcommands() {
        for (Method method : getClass().getMethods()) {
            if (method.isAnnotationPresent(SubCommand.class) && method.getParameterCount() == 1) return true;
        }
        return false;
    }

    public CommandData generateCommandData(Tlumaczenia tlumaczenia) {
        subcommands.clear();
        if (usage != null && hasSubcommands())
            throw new IllegalArgumentException("The base command can't be used if there are subcommands present");
        SlashCommandData data = generateBasicCommandData(tlumaczenia);
        if (hasSubcommands()) {
            Map<String, SubcommandGroupData> groups = new HashMap<>();
            List<SubcommandData> mainSubs = new ArrayList<>();
            for (Method method : getClass().getMethods()) {
                if (method.isAnnotationPresent(SubCommand.class) && method.getParameterCount() == 1) {
                    SubcommandData scd;
                    String keyBase = CommandUtil.getAsKey(name);
                    SubCommand subcommand = method.getAnnotation(SubCommand.class);
                    SubcommandGroupData subcommandGroupData = null;
                    if (method.isAnnotationPresent(SubCommandGroup.class)) {
                        String groupName = method.getAnnotation(SubCommandGroup.class).name();
                        keyBase += "." + CommandUtil.getAsKey(groupName);
                        String finalKeyBase = keyBase;
                        subcommandGroupData = groups.compute(groupName, (key, scgd) -> {
                            if (scgd == null) {
                                scgd = new SubcommandGroupData(groupName,
                                        tlumaczenia.get(Language.DEFAULT, finalKeyBase + ".description"));
                            }
                            return scgd;
                        });
                    }
                    keyBase += "." + CommandUtil.getAsKey(subcommand.name());
                    scd = new SubcommandData(subcommand.name(),
                            tlumaczenia.get(Language.DEFAULT, keyBase + ".description"))
                            .addOptions(CommandUtil.generateOptionData(this,
                                    subcommandGroupData != null ? subcommandGroupData.getName() : null,
                                    subcommand.name(), subcommand.usage(), tlumaczenia));
                    if (subcommandGroupData != null) subcommandGroupData.addSubcommands(scd);
                    else mainSubs.add(scd);
                    subcommands.put((subcommandGroupData != null ? subcommandGroupData.getName() + "/" : "") + subcommand.name(), method);
                }
            }
            data.addSubcommandGroups(groups.values());
            data.addSubcommands(mainSubs);
        }
        if (usage != null) data.addOptions(getOptions(tlumaczenia));
        return data;
    }

    protected SlashCommandData generateBasicCommandData(Tlumaczenia tlumaczenia) {
        return Commands.slash(name, tlumaczenia.get(Language.DEFAULT, name + ".description"))
                .setGuildOnly(!allowInDMs)
                .setLocalizationFunction(tlumaczenia)
                .setDefaultPermissions(permissions);
    }

    protected OptionData[] getOptions(Tlumaczenia tlumaczenia) {
        return CommandUtil.generateOptionData(this, null, null, usage, tlumaczenia);
    }

    public void updateOptionData(OptionData option) {
        // no-op, niech komendy które tego potrzebują to nadpiszą
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SubcommandContainer {
        private final Method method;
        private final Map<String, String> optionMap;
    }
}
