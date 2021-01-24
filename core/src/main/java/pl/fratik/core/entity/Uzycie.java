/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
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

package pl.fratik.core.entity;

import lombok.Setter;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.arguments.ParsedArgument;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.manager.ManagerArgumentow;

import java.util.*;

public class Uzycie {

    @Setter private static ManagerArgumentow managerArgumentow;
    private final LinkedHashMap<String, ParsedArgument> arguments = new LinkedHashMap<>();
    private static final String NIZNAR = "Nie znaleziono argumentu ";

    public Uzycie() {
        // po prostu ignoruj i chuj
    }

    public Uzycie(String key, String value) {
        this(key, value, false);
    }

    public Uzycie(String key, String value, boolean required) {
        if (managerArgumentow == null) throw new RuntimeException("managerArgumentów nie może być null!");
        Argument arg = managerArgumentow.getArguments().get(value);
        if (arg == null) throw new IllegalArgumentException(NIZNAR + value);

        arguments.put(key, new ParsedArgument(arg, false, required));
    }

    @SuppressWarnings({"SameParameterValue", "squid:S1192", "squid:S1319"})
    public Uzycie(LinkedHashMap<String, String> uzycia, boolean[] requiredDlaUzyc) {
        String lastTag = "";
        boolean repeatingPlaced = false;
        int i = 0;
        if (requiredDlaUzyc.length != uzycia.entrySet().size()) throw new IllegalArgumentException("Niezgodność arrayów!");
        for (Map.Entry<String, String> entry : uzycia.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.equals("[...]") && arguments.isEmpty()) throw new IllegalArgumentException("Repeating tag znaleziony " +
                    "przed pierwszym argumentem");
            if (repeatingPlaced) {
                throw new IllegalArgumentException("Nie można postawić taga po repeating tagu!");
            }
            if (key.equals("[...]")) {
                repeatingPlaced = true;
            }
            Argument arg = managerArgumentow.getArguments().get(value);
            if (arg == null) throw new IllegalArgumentException(NIZNAR + value);

            if (!repeatingPlaced) arguments.put(key, new ParsedArgument(arg, false, requiredDlaUzyc[i]));
            else arguments.replace(lastTag, new ParsedArgument(arg, true, requiredDlaUzyc[i-1]));
            lastTag = key;
            i++;
        }
    }

    public Object[] resolveArgs(CommandContext context) throws ArgsMissingException {
        ArrayList<Object> args = new ArrayList<>();
        Iterator<ParsedArgument> iterator = arguments.values().iterator();
        int aj = 0;
        for (String arg : context.getRawArgs()) {
            if (!iterator.hasNext()) break;
            ParsedArgument a = iterator.next();
            if (a.isRepeating()) {
                ArrayList<String> argList = new ArrayList<>(Arrays.asList(context.getRawArgs())
                        .subList(aj, context.getRawArgs().length));
                while (!argList.isEmpty()) {
                    ArgumentContext ctx2 = new ArgumentContext(a, context.getEvent(), argList.get(0), context.getTlumaczenia(), context.getGuild());
                    args.add(a.execute(ctx2));
                    argList.remove(0);
                }
            } else {
                ArgumentContext ctx = new ArgumentContext(a, context.getEvent(), arg, context.getTlumaczenia(), context.getGuild());
                args.add(a.execute(ctx));
            }
            aj++;
        }
        for (int i = 0; i < arguments.size(); i++) {
            ParsedArgument arg = new ArrayList<>(arguments.values()).get(i);
            if (!arg.isRequired()) continue;
            Object a;
            try {
                a = args.get(i);
            } catch (IndexOutOfBoundsException e) {
                throw new ArgsMissingException("Nieprawidłowy wymagany argument nr " + (i + 1) + "!", arg,
                        context.getCommand(), context.getLanguage(), context.getTlumaczenia());
            }
            if (a == null) {
                throw new ArgsMissingException("Nieprawidłowy wymagany argument nr " + (i + 1) + "!", arg,
                        context.getCommand(), context.getLanguage(), context.getTlumaczenia());
            }
        }
        return args.toArray();
    }



}
