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
import net.dv8tion.jda.api.entities.Guild;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

public abstract class Argument {
    @Getter protected String name;
    @Getter protected String[] aliases = new String[0];
    protected Object execute(ArgumentContext context) {
        throw new UnsupportedOperationException("Argument nie ma zaimplementowanej funkcji execute(ArgumentContext)");
    }
    public Object execute(String argument, Tlumaczenia tlumaczenia, Language language) {
        throw new UnsupportedOperationException("Argument nie ma zaimplementowanej funkcji execute(String, Tlumaczenia, Language)");
    }
    public Object execute(String argument, Tlumaczenia tlumaczenia, Language language, Guild guild) {
        throw new UnsupportedOperationException("Argument nie ma zaimplementowanej funkcji execute(String, Tlumaczenia, Language, Guild)");
    }
}