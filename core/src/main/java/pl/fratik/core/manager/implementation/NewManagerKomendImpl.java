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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.event.CommandSyncEvent;
import pl.fratik.core.manager.NewManagerKomend;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class NewManagerKomendImpl implements NewManagerKomend {
    private final ShardManager shardManager;
    private final Tlumaczenia tlumaczenia;
    private final Map<Modul, Set<NewCommand>> commands;
    private final Logger logger;
    private final EventBus eventBus;

    public NewManagerKomendImpl(ShardManager shardManager, Tlumaczenia tlumaczenia, EventBus eventBus) {
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        this.eventBus = eventBus;
        commands = new HashMap<>();
        logger = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void registerCommands(Modul modul, Collection<NewCommand> commands) {
        Set<NewCommand> set = this.commands.computeIfAbsent(modul, k -> new HashSet<>());
        set.addAll(commands);
        for (NewCommand command : commands) {
            command.registerSubcommands();
            try {
                command.onRegister();
            } catch (Exception e) {
                logger.warn("Wystąpił błąd podczas onRegister!", e);
            }
        }
    }

    @Override
    public void unregisterCommands(Collection<NewCommand> commands) {
        unregister(commands::contains);
    }

    @Override
    public void sync() {
        Set<CommandData> cmds = new HashSet<>();
        Set<CommandData> guildCmds = new HashSet<>();
        for (Set<NewCommand> moduleCommands : commands.values()) {
            for (NewCommand cmd : moduleCommands) {
                logger.debug("Rejestruję komendę {}", cmd.getName());
                CommandData e = cmd.generateCommandData(tlumaczenia);
                if (cmd.getType() == CommandType.SUPPORT_SERVER) guildCmds.add(e);
                else cmds.add(e);
            }
        }
        Guild guild = shardManager.getGuildById(Ustawienia.instance.botGuild);
        if (guild == null) logger.warn("Nie znaleziono serwera {}", Ustawienia.instance.botGuild);
        else guild.updateCommands().addCommands(guildCmds).complete();
        shardManager.getShardById(0).updateCommands().addCommands(cmds).complete();
        eventBus.post(new CommandSyncEvent(cmds, guildCmds));
    }

    @Override
    public void unregisterAll() {
        unregister(cmd -> true);
    }

    @Override
    public void unregisterAll(Modul modul) {
        unregister(commands.getOrDefault(modul, Set.of())::contains);
    }

    private void unregister(Predicate<NewCommand> checker) {
        for (Iterator<Map.Entry<Modul, Set<NewCommand>>> iterator = this.commands.entrySet().iterator(); iterator.hasNext();) {
            Set<NewCommand> komendy = iterator.next().getValue();
            for (Iterator<NewCommand> iter = komendy.iterator(); iter.hasNext();) {
                NewCommand cmd = iter.next();
                if (checker.test(cmd)) {
                    try {
                        cmd.onUnregister();
                    } catch (Exception e) {
                        logger.warn("Wystąpił błąd podczas onUnregister!", e);
                    }
                    iter.remove();
                }
            }
            if (komendy.isEmpty()) iterator.remove();
        }
    }

    @Override
    public Map<Modul, Set<NewCommand>> getRegistered() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public void shutdown() {
        unregisterAll();
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        NewCommand command = commandsStream()
                .filter(c -> event.isGuildCommand() ? c.getType() == CommandType.SUPPORT_SERVER : c.getType() == CommandType.NORMAL)
                .filter(c -> c.getName().equals(name)).findAny().orElse(null);
        if (command == null) {
            logger.warn("Nie znaleziono komendy {}", name);
            return;
        }
        if (command.getType() == CommandType.SUPPORT_SERVER && !event.getGuild().getId().equals(Ustawienia.instance.botGuild))
            return;
        NewCommandContext ctx = new NewCommandContext(shardManager, command, tlumaczenia, event.getInteraction());
        if (!command.permissionCheck(ctx)) return;
        if (event.getSubcommandName() != null) {
            String subname = (event.getSubcommandGroup() != null ? event.getSubcommandGroup() + "/" : "") + event.getSubcommandName();
            Method method = command.getSubcommands().get(subname);
            if (method == null) {
                logger.warn("Nie znaleziono subkomendy {} w komendzie {}", subname, name);
                return;
            }
            try {
                method.invoke(command, ctx);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Komenda nieprawidłowo zarejestrowana", e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
                else throw new IllegalStateException("Błąd w subkomendzie", e.getCause());
            }
            return;
        }
        command.execute(ctx);
    }

    @Override
    public Stream<NewCommand> commandsStream() {
        return commands.values().stream().flatMap(Set::stream);
    }
}
