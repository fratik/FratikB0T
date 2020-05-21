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

package net.hypixel.api.exceptions;

@SuppressWarnings("unused")
public class HypixelAPIException extends RuntimeException {

    public HypixelAPIException() {
    }

    public HypixelAPIException(String message) {
        super(message);
    }

    public HypixelAPIException(String message, Throwable cause) {
        super(message, cause);
    }

    public HypixelAPIException(Throwable cause) {
        super(cause);
    }

    public HypixelAPIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
