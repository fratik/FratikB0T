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

package pl.fratik.api.entity;

import lombok.Getter;

public abstract class Exceptions {

    public enum Codes {
        UNAUTHORIZED(401, 401),                   // Nie zalogowany
        FORBIDDEN(403, 403),                      // Brak dostępu
        UNKNOWN_ERROR(500),                       // Nieznany błąd
        NO_BODY(1016),                            // Brak ,,body'' w requeście

        NO_PARAM(1002),                           // Brakuje parametru(-ow) w requescie
        NOT_IN_FDEV(1003),                        // Bot nie na fdevie

        INVALID_USER(1004),                       // Zły użytkownik
        INVALID_LANG(1005),                       // Zły język
        INVALID_FORMAT(1006),                     // Zły format
        INVALID_GUILD(1007),                      // Zły serwer
        INVALID_PERM_LVL(1008),                   // Zły perm level

        INVALID_RUNDKA_ID(1009),                  // Złe ID rundki
        RUNDKA_NO_REPLY(1010),                    // Brak odpowiedzi w rundzie
        NO_RUNDKA(1011),                          // Nie ma żadnej aktywnej rundki
        INVALID_PURGE_ID(1012),                   // Złe ID purga
        PURGE_NO_REQUESTER_ID(1013),              // Brak Requester-ID dla prywatnego purge

        JOIN_BANNED(1014),                        // Użytkownik jest zbanowany
        JOIN_ERROR(1015),                         // Błąd przy dołączaniu

        /* WebSocket */

        WS_INTERNAL_ERROR(2000, 1011),            // Wewnętrzny błąd
        WS_INVALID_MESSAGE_FORMAT(2001, 1007),    // Nieprawidłowy format wiadomości socketowej
        WS_INVALID_MESSAGE_TYPE(2002, 1007),      // Nieprawidłowy typ wiadomości
        WS_INVALID_CHANNEL(2003, 4001),           // Nieprawidłowy kanał
        WS_INVALID_SIGNATURE(2004, 4001),         // Nieprawidłowy podpis (logowanie)
        WS_NOT_LOGGED_IN(2005, 4002),             // Niezalogowany
        WS_LOGGED_OUT(2006, 4002);                // Zewnętrzne źródło wylogowało

        @Getter private final int code;
        @Getter private final int httpCode;

        Codes(int code) {
            this(code, 500);
        }

        Codes(int code, int httpCode) {
            this.code = code;
            this.httpCode = httpCode;
        }

        public String getJson() {
            return getJson(this);
        }

        public static String getJson(Codes code) {
            return String.format("{\"success\": false, \"code\": %s}", code.getCode());
        }

    }

}
