/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
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

package pl.fratik.api;

import lombok.Getter;
import pl.fratik.api.entity.Exceptions;

public interface SocketAdapter {
    String getChannelName();
    default void subscribe(SocketManager.Connection connection) throws RegisterException {}
    default void unsubscribe(SocketManager.Connection connection) {}

    @Getter
    class RegisterException extends Exception {
        private final Exceptions.Codes exceptionCode;

        public RegisterException() {
            this(null);
        }

        public RegisterException(Exceptions.Codes exceptionCode) {
            this.exceptionCode = exceptionCode;
        }
    }
}
