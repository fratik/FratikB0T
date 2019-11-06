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

import io.undertow.server.HttpHandler;

import java.util.function.Function;

public class MiddlewareBuilder {
    private final Function<HttpHandler, HttpHandler> function;

    private MiddlewareBuilder(Function<HttpHandler, HttpHandler> function) {
        if (null == function) {
            throw new IllegalArgumentException("Middleware Function can not be null");
        }
        this.function = function;
    }

    public static MiddlewareBuilder begin(Function<HttpHandler, HttpHandler> function) {
        return new MiddlewareBuilder(function);
    }

    public MiddlewareBuilder next(Function<HttpHandler, HttpHandler> before) {
        return new MiddlewareBuilder(function.compose(before));
    }

    public HttpHandler complete(HttpHandler handler) {
        return function.apply(handler);
    }
}
