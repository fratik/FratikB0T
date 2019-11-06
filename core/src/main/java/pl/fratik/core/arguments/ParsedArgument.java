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

package pl.fratik.core.arguments;

import lombok.Getter;
import lombok.Setter;

public class ParsedArgument extends Argument {

    @Getter private final Argument arg;
    @Getter @Setter private boolean repeating;
    @Getter @Setter private boolean required;

    public ParsedArgument(Argument arg, boolean repeating, boolean required) {
        this.arg = arg;
        name = arg.getName();
        aliases = arg.getAliases();
        this.repeating = repeating;
        this.required = required;
    }

    @Override
    public Object execute(ArgumentContext context) {
        return arg.execute(context);
    }

}
