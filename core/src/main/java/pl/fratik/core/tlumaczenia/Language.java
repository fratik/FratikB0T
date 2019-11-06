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

package pl.fratik.core.tlumaczenia;

import lombok.Getter;

import java.util.Locale;

public enum Language {
    DEFAULT(null, null, null, null),
//    ENGLISH("en-US", "English (US)", "\uD83C\uDDFA\uD83C\uDDF8", new Locale("en_US")),
    POLISH("pl", "Polski", "\uD83C\uDDF5\uD83C\uDDF1", new Locale("pl_PL"));

    @Getter private final String shortName;
    @Getter private final String localized;
    @Getter private final String emoji;
    @Getter private final Locale locale;

    Language(String shortName, String localized, String emoji, Locale locale) {
        this.shortName = shortName;
        this.localized = localized;
        this.emoji = emoji;
        this.locale = locale;
    }

}
