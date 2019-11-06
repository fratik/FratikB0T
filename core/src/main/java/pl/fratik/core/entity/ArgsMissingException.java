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

package pl.fratik.core.entity;

import lombok.Getter;
import pl.fratik.core.arguments.ParsedArgument;
import pl.fratik.core.command.Command;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

public class ArgsMissingException extends Exception {

    @Getter private final transient ParsedArgument missingArgument;
    @Getter private final transient Command command;
    @Getter private final transient Language language;
    @Getter private final transient Tlumaczenia tlumaczenia;

    ArgsMissingException(String message, ParsedArgument missingArgument, Command command, Language language, Tlumaczenia tlumaczenia) {
        super(message);
        this.missingArgument = missingArgument;
        this.command = command;
        this.language = language;
        this.tlumaczenia = tlumaczenia;
    }
}
