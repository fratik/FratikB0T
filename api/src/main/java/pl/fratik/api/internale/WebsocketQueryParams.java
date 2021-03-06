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

package pl.fratik.api.internale;

import io.undertow.util.QueryParameterUtils;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.util.Deque;
import java.util.Optional;

public interface WebsocketQueryParams {

    default Optional<String> queryParam(WebSocketHttpExchange exchange, String name) {
        return Optional.ofNullable(QueryParameterUtils.parseQueryString(exchange.getQueryString(), null).get(name))
                .map(Deque::getFirst);
    }

    default Optional<Long> queryParamAsLong(WebSocketHttpExchange exchange, String name) {
        return queryParam(exchange, name).map(Long::parseLong);
    }

    default Optional<Integer> queryParamAsInteger(WebSocketHttpExchange exchange, String name) {
        return queryParam(exchange, name).map(Integer::parseInt);
    }

    default Optional<Boolean> queryParamAsBoolean(WebSocketHttpExchange exchange, String name) {
        return queryParam(exchange, name).map(Boolean::parseBoolean);
    }
}
