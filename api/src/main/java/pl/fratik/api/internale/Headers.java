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

package pl.fratik.api.internale;

import io.undertow.attribute.RequestHeaderAttribute;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.Optional;

public interface Headers {

    default Optional<String> getHeader(HttpServerExchange exchange, HttpString header) {
        RequestHeaderAttribute reqHeader = new RequestHeaderAttribute(header);
        return Optional.ofNullable(reqHeader.readAttribute(exchange));
    }

    default Optional<String> getHeader(HttpServerExchange exchange, String header) {
        RequestHeaderAttribute reqHeader = new RequestHeaderAttribute(new HttpString(header));
        return Optional.ofNullable(reqHeader.readAttribute(exchange));
    }

    default void setHeader(HttpServerExchange exchange, HttpString header, String value) {
        exchange.getResponseHeaders().add(header, value);
    }

    default void setHeader(HttpServerExchange exchange, String header, String value) {
        exchange.getResponseHeaders().add(new HttpString(header), value);
    }
}

