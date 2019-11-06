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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;

import java.util.ArrayList;

public class ArgumentContext {
    @Getter private final ParsedArgument argument;
    @Getter private final MessageReceivedEvent event;
    @Getter private final String arg;
    @Getter private final Tlumaczenia tlumaczenia;
    @Getter private final Language language;
    @Getter private final Guild guild;

    public ArgumentContext(ParsedArgument argument, MessageReceivedEvent event, String arg, Tlumaczenia tlumaczenia, Guild guild) {
        this.argument = argument;
        this.event = event;
        this.arg = arg;
        this.tlumaczenia = tlumaczenia;
        this.language = tlumaczenia.getLanguage(event.getMember());
        this.guild = guild;
    }

    public String getTranslated(String key) {
        return tlumaczenia.get(language, key);
    }

    public String getTranslated(String key, String ...argi) {
        return tlumaczenia.get(language, key, argi);
    }

    public String getTranslated(String key, Object ...argi) {
        ArrayList<String> parsedArgi = new ArrayList<>();
        for (Object argu : argi) {
            parsedArgi.add(argu.toString());
        }
        return tlumaczenia.get(language, key, parsedArgi.toArray());
    }

}
