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

package pl.fratik.core.manager.implementation;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.*;
import java.util.stream.Stream;

public class NewManagerKomendImpl implements NewManagerKomend {
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;
    private final Map<Modul, Set<NewCommand>> commands;
    private final Map<String, NewCommand> synced;
    private final Logger logger;

    public NewManagerKomendImpl(ShardManager shardManager, Tlumaczenia tlumaczenia) {
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        commands = new HashMap<>();
        synced = new HashMap<>();
        logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void registerCommands(Modul modul, Collection<NewCommand> commands) {
        Set<NewCommand> set = this.commands.computeIfAbsent(modul, k -> new HashSet<>());
        set.addAll(commands);
    }

    @Override
    public void unregisterCommands(Collection<NewCommand> commands) {

    }

    @Override
    public void sync() {
        Set<CommandData> cmds = new HashSet<>();
        for (Set<NewCommand> moduleCommands : commands.values()) {
            for (NewCommand cmd : moduleCommands) {
                CommandData e = cmd.generateCommandData(tlumaczenia);
                synced.put(e.getName(), cmd);
                cmds.add(e);
            }
        }
        shardManager.getShardById(0).updateCommands().addCommands(cmds).complete();
    }

    @Override
    public void unregisterAll() {

    }

    @Override
    public void unregisterAll(Modul modul) {

    }

    @Override
    public Map<Modul, Set<NewCommand>> getRegistered() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public void shutdown() {

    }

    @Subscribe
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        NewCommand command = commandsStream().filter(c -> c.getName().equals(name)).findAny().orElse(null);
        if (command == null) {
            logger.warn("Nie znaleziono komendy {}", name);
            return;
        }
        command.execute(new NewCommandContext(shardManager, command, tlumaczenia, event.getInteraction()));
    }

    private Stream<NewCommand> commandsStream() {
        return commands.values().stream().flatMap(Set::stream);
    }
}
