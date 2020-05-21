/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package pl.fratik.core.tlumaczenia;

import lombok.Getter;
import pl.fratik.core.entity.Emoji;

import java.util.Locale;

public enum Language {
    DEFAULT(null, null, null, null),
    ENGLISH("en-US", "English (US)", "UNICODE:\uD83C\uDDFA\uD83C\uDDF8", new Locale("en_US")),
    POLISH("pl", "Polski", "UNICODE:\uD83C\uDDF5\uD83C\uDDF1", new Locale("pl_PL"))//,
    /*POLISH_WULG("pl-WG", "Polski (wulgarny)", "663853676053659687", new Locale("pl_WG")),
    PONGLISH("pl-EN", "Ponglish", "665552851820478515", new Locale("pl_EN"))*/;

    @Getter private final String shortName;
    @Getter private final String localized;
    private final String emoji;
    @Getter private final Locale locale;

    Language(String shortName, String localized, String emoji, Locale locale) {
        this.shortName = shortName;
        this.localized = localized;
        this.emoji = emoji;
        this.locale = locale;
    }

    public Emoji getEmoji() {
        return Emoji.resolve(emoji, Tlumaczenia.getShardManager());
    }
}
