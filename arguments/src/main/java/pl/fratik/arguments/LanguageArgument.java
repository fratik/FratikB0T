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

public class LanguageArgument extends Argument {
    LanguageArgument() {
        name = "language";
    }

    @Override
    public Language execute(@NotNull ArgumentContext context) {
        Language[] languages = Language.values();
        for (Language l : languages) {
            if (!l.equals(Language.DEFAULT)) {
                if (context.getArg().equals(l.getEmoji())) return l;
                if (context.getArg().equalsIgnoreCase(l.getLocalized())) return l;
                if (context.getArg().equalsIgnoreCase(l.getShortName())) return l;
            }
        }
        return null;
    }
}