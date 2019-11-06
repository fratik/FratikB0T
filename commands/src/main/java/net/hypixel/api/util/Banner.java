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

package net.hypixel.api.util;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Banner {

    @SerializedName("Base")
    private String base;
    @SerializedName("Patterns")
    private List<Pattern> patterns;

    public String getBase() {
        return base;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public String toString() {
        return "Banner{" +
                "base='" + base + '\'' +
                ", patterns=" + patterns +
                '}';
    }

    static class Pattern {
        @SerializedName("Pattern")
        private String pattern;
        @SerializedName("Color")
        private String color;

        public String getPattern() {
            return pattern;
        }

        public String getColor() {
            return color;
        }

        @Override
        public String toString() {
            return "Pattern{" +
                    "pattern='" + pattern + '\'' +
                    ", color='" + color + '\'' +
                    '}';
        }
    }
}
