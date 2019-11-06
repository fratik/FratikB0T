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

package pl.fratik.moderation.utils;

import lombok.Getter;

@Getter
@Deprecated
public class Response {
    private long delta;
    private final boolean valid;

    Response(long delta, boolean valid) {
        this.delta = delta;
        this.valid = valid;
    }

    Response(boolean valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return String.format("%s {delta=%d,valid=%s}", super.toString(), delta, valid);
    }
}
