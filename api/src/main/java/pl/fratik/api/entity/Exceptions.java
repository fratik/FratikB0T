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

@SuppressWarnings({"squid:S1068", "squid:S1170", "squid:S2166", "unused"})
public abstract class Exceptions {
    private final boolean success = false;
    @SuppressWarnings("FieldCanBeLocal")
    public static class GenericException extends Exceptions {
        private final String error;
        public GenericException(String errorMessage) {
            error = errorMessage;
        }
    }
    public static class NoLanguageException extends Exceptions {
        private final String error = "nie ma parametru języka";
    }
    public static class InvalidLanguageException extends Exceptions {
        private final String error = "nieprawidłowy język";
    }
    public static class NoGuildParam extends Exceptions {
        private final String error = "nie podano serwera";
    }
    public static class NoGuild extends Exceptions {
        private final String error = "nie znaleziono serwera";
    }
    public static class NoUserParam extends Exceptions {
        private final String error = "nie podano użytkownika";
    }
    public static class NoUser extends Exceptions {
        private final String error = "nie znaleziono użytkownika";
    }
    public static class NoRundka extends Exceptions {
        private final String error = "żadna rundka nie jest w toku";
    }
    public static class NotInFdev extends Exceptions {
        private final String error = "nie na fdev";
    }
}
