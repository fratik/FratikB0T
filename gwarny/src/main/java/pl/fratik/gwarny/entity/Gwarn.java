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

package pl.fratik.gwarny.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Data
@AllArgsConstructor
public class Gwarn {
    @Nonnull
    private final String nadane;
    @Nullable
    private String powod;
    private boolean active = true;
    private long timestamp;

    public Gwarn(@Nonnull String nadane, @Nullable String powod) {
        this.nadane = nadane;
        if (powod != null && powod.length() > 1024) throw new IllegalArgumentException("Powód dłuższy od 1024 znaków");
        this.powod = powod;
    }

    public void setPowod(String powod) {
        if (powod != null && powod.length() > 1024) throw new IllegalArgumentException("Powód dłuższy od 1024 znaków");
        this.powod = powod;
    }
}
