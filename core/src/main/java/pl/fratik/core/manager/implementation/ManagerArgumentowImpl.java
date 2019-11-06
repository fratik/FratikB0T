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

package pl.fratik.core.manager.implementation;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.manager.ManagerArgumentow;

import java.util.*;

public class ManagerArgumentowImpl implements ManagerArgumentow {

    @Getter private static ManagerArgumentow instance;
    private static final Logger logger = LoggerFactory.getLogger(ManagerArgumentow.class);

    @Getter private Set<Argument> registered;
    @Getter private Map<String, Argument> arguments;

    public ManagerArgumentowImpl() {
        instance = this; //NOSONAR
        this.registered = new HashSet<>();
        this.arguments = new HashMap<>();
    }

    @Override
    public void registerArgument(Argument argument) {
        if (argument == null) return;

        List<String> aliases = Arrays.asList(argument.getAliases());

        if (arguments.containsKey(argument.getName()) || (!aliases.isEmpty() && arguments.keySet().containsAll(aliases)))
            throw new IllegalArgumentException("Alias lub nazwa już zarejestrowana!");

        registered.add(argument);
        arguments.put(argument.getName(), argument);

        aliases.forEach(alias -> {
            logger.debug("Zarejestrowano argument: {} -> {}", argument.getName(), argument);
            arguments.put(alias, argument);
        });
    }

    @Override
    public void unregisterArgument(Argument argument) {
        if (argument == null) return;
        arguments.values().removeIf(arg -> argument == arg);
        registered.removeIf(arg -> argument == arg);
        arguments.values().removeIf(arg -> arg.getName().equals(argument.getName()));
        registered.removeIf(arg -> arg.getName().equals(argument.getName()));
    }

    @Override
    public void unregisterAll() {
        registered = new HashSet<>();
        arguments = new HashMap<>();
    }

}
