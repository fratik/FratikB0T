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
import io.undertow.server.handlers.ExceptionHandler;
import org.slf4j.LoggerFactory;
import pl.fratik.api.entity.Exceptions;

public class ExceptionHandlers {

    private ExceptionHandlers() {}

    public static void handleWebException(HttpServerExchange exchange) {
        WebException ex = (WebException) exchange.getAttachment(ExceptionHandler.THROWABLE);
        exchange.setStatusCode(ex.getStatusCode());
        Exchange.body().sendHtml(exchange, "<h1>" + ex.getMessage() + "</h1>");
    }

    public static void handleApiException(HttpServerExchange exchange) {
        ApiException ex = (ApiException) exchange.getAttachment(ExceptionHandler.THROWABLE);
        exchange.setStatusCode(ex.getStatusCode());
        Exchange.body().sendJson(exchange, "{\"message\": \"" + ex.getMessage() + "\"}");
    }

    public static void handleAllExceptions(HttpServerExchange exchange) {
        LoggerFactory.getLogger(ExceptionHandlers.class).error("Błąd w request'cie!",
                exchange.getAttachment(ExceptionHandler.THROWABLE));
        Exchange.body().sendJson(exchange, new Exceptions.GenericException("Internal Server Error!"), 500);
    }

    public static void throwWebException(HttpServerExchange exchange) {
        throw new WebException(500, "Web Server Error");
    }

    public static void throwApiException(HttpServerExchange exchange) {
        throw new ApiException(503, "API Server Error");
    }

    public static void throwException(HttpServerExchange exchange) {
        throw new RuntimeException();
    }

    public static void ok(HttpServerExchange exchange) {
        Exchange.body().sendText(exchange, "ok");
    }

}
