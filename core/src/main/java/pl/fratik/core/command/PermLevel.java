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

package pl.fratik.core.command;

import lombok.Getter;

public enum PermLevel {

    EVERYONE(0, "permlevel.everyone"),
    MOD(1, "permlevel.mod"),
    ADMIN(2, "permlevel.admin"),
    MANAGESERVERPERMS(3, "permlevel.manageserver"),
    OWNER(4, "permlevel.owner"),
    GADMIN(5, "permlevel.gadmin"),
    ZGA(6, "permlevel.zga"),
    BOTOWNER(10, "permlevel.botowner");

    @Getter private final int num;
    @Getter private final String languageKey;

    PermLevel(int uprawnieniaNumeryczne, String languageKey) {
        this.num = uprawnieniaNumeryczne;
        this.languageKey = languageKey;
    }

    public static PermLevel getPermLevel(int num) {
        if (num == 0) return EVERYONE;
        if (num == 1) return MOD;
        if (num == 2) return ADMIN;
        if (num == 3) return MANAGESERVERPERMS;
        if (num == 4) return OWNER;
        if (num == 5) return GADMIN;
        if (num == 6) return ZGA;
        if (num == 10) return BOTOWNER;
        throw new IllegalArgumentException("Nieprawidłowy poziom!");
    }

}
