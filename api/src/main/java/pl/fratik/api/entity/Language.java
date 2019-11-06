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

package pl.fratik.api.entity;

import lombok.Data;

import java.util.Locale;

@Data
public class Language {
    private String name;
    private String shortName;
    private String localized;
    private String emoji;
    private Locale locale;

    public Language(pl.fratik.core.tlumaczenia.Language l) {
        name = l.name();
        shortName = l.getShortName();
        localized = l.getLocalized();
        emoji = l.getEmoji();
        locale = l.getLocale();
    }
}
