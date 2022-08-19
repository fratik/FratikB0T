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

package pl.fratik.test;

import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;

import java.util.stream.Collectors;

public class ArgCommand extends NewCommand {
    public ArgCommand() {
        name = "arg";
        usage = "<wymagany_user:user> <jebac:string> [aaaaaa:number]";
    }

    @Override
    public void execute(NewCommandContext context) {
        context.reply(context.getArguments().entrySet().stream().map(e -> e.getKey() + ": `" + e.getValue().toString() + "`").collect(Collectors.joining("\n")));
    }
}
