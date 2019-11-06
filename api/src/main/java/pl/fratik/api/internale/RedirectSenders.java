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

import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import io.undertow.util.Headers;

interface RedirectSenders {

    /*
     * Temporary redirect
     */
    default void temporary(HttpServerExchange exchange, String location) {
        exchange.setStatusCode(StatusCodes.FOUND);
        exchange.getResponseHeaders().put(Headers.LOCATION, location);
        exchange.endExchange();
    }

    /*
     * Permanent redirect
     */
    default void permanent(HttpServerExchange exchange, String location) {
        exchange.setStatusCode(StatusCodes.MOVED_PERMANENTLY);
        exchange.getResponseHeaders().put(Headers.LOCATION, location);
        exchange.endExchange();
    }

    /*
     * Temporary Redirect to the previous page based on the Referrer header.
     * This is very useful when you want to redirect to the previous
     * page after a form submission.
     */
    default void referer(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.FOUND);
        exchange.getResponseHeaders().put(Headers.LOCATION, exchange.getRequestHeaders().get(Headers.REFERER, 0));
        exchange.endExchange();
    }
}