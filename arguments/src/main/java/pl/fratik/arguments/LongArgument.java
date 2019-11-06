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

import org.jetbrains.annotations.NotNull;
import pl.fratik.core.arguments.Argument;
import pl.fratik.core.arguments.ArgumentContext;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

public class LongArgument extends Argument {
    LongArgument() {
        name = "long";
    }

    @Override
    public Long execute(@NotNull ArgumentContext context) {
        try {
            long test = Long.parseLong(context.getArg());
            if (test >= 0 && test < Long.MAX_VALUE) return test;
        } catch (Exception ignored) {
            /*lul*/
        }
        return null;
    }

    @Override
    public Long execute(String argument, Tlumaczenia tlumaczenia, Language language) {
        try {
            long test = Long.parseLong(argument);
            if (test >= 0 && test < Long.MAX_VALUE) return test;
        } catch (Exception ignored) {
            /*lul*/
        }
        return null;
    }
}