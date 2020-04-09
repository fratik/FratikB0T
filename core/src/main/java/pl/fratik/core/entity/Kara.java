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

import java.awt.*;

public enum Kara {

    KICK(0, new Color(0x800080)),
    BAN(2, new Color(0xff7f00)),
    WARN(3, new Color(0xffff00)),
    REMOVEADMIN(4),
    REMOVEMOD(5),
    UNBAN(6, new Color(0xff00)),
    UNWARN(7, new Color(0x800080)),
    MUTE(8, new Color(0xffffff)),
    UNMUTE(9, new Color(0xff00)),
    NOTATKA(10, new Color(0xa9a9a9));

    @Getter private final int numerycznie;
    @Getter private Color kolor;
    @Getter private final boolean dlaWarnow;

    Kara(int num, Color color) {
        numerycznie = num;
        kolor = color;
        dlaWarnow = false;
    }

    Kara(int num) {
        numerycznie = num;
        dlaWarnow = true;
    }

    public static Kara getByNum(int num) {
        switch (num) {
            case 0: return KICK;
            case 1:
            case 2: return BAN;
            case 3: return WARN;
            case 4: return REMOVEADMIN;
            case 5: return REMOVEMOD;
            case 6: return UNBAN;
            case 7: return UNWARN;
            case 8: return MUTE;
            case 9: return UNMUTE;
            case 10: return NOTATKA;
            default: return null;
        }
    }
}
