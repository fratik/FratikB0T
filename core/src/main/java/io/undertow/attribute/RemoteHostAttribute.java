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

package io.undertow.attribute;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * The remote Host address (if resolved)
 *
 * @author Stuart Douglas
 */
class RemoteHostAttribute implements ExchangeAttribute {

    private static final String REMOTE_HOST_NAME_SHORT = "%h";
    private static final String REMOTE_HOST = "%{REMOTE_HOST_PROXIED}";

    private static final ExchangeAttribute INSTANCE = new RemoteHostAttribute();

    private RemoteHostAttribute() {

    }

    @Override
    public String readAttribute(final HttpServerExchange exchange) {
        RequestHeaderAttribute reqHeader = new RequestHeaderAttribute(new HttpString("X-Forwarded-For"));
        Optional<String> header = Optional.ofNullable(reqHeader.readAttribute(exchange));
        if (header.isPresent() && header.get().length() != 0) {
            return header.get();
        }
        final InetSocketAddress sourceAddress = exchange.getSourceAddress();
        return sourceAddress.getHostString();
    }

    @Override
    public void writeAttribute(final HttpServerExchange exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Remote host", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Remote host";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REMOTE_HOST) || token.equals(REMOTE_HOST_NAME_SHORT)) {
                return RemoteHostAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
