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
import okhttp3.HttpUrl;

import java.util.List;
import java.util.Objects;

interface Urls {

    default HttpUrl currentUrl(HttpServerExchange exchange) {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(exchange.getRequestURL())).newBuilder();

        if (!"".equals(exchange.getQueryString())) {
            urlBuilder.encodedQuery(exchange.getQueryString());
        }
        return urlBuilder.build();
    }

    default HttpUrl host(HttpServerExchange exchange) {
        HttpUrl url = HttpUrl.parse(exchange.getRequestURL());
        List<String> pathSegments = Objects.requireNonNull(url).pathSegments();
        HttpUrl.Builder urlBuilder = url.newBuilder();
        for (int i = pathSegments.size() - 1; i >= 0; i--) {
            urlBuilder.removePathSegment(i);
        }
        urlBuilder.query(null);
        return urlBuilder.build();
    }

}
