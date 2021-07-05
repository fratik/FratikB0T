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

import io.sentry.Sentry;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Ustawienia;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class SocketManager implements SocketAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SocketManager.class);

    private final ShardManager shardManager;

    private final Map<SocketAdapter, Map<String, Method>> events = new HashMap<>();

    private Socket socket = null;

    public SocketManager(ShardManager shardManager) {
        this.shardManager = shardManager;
        registerAdapter(this);
    }

    public void registerAdapter(SocketAdapter adapter) {
        int count = 0;
        for (Method method : adapter.getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(SocketEvent.class)) {
                    SocketEvent socketEvent = method.getAnnotation(SocketEvent.class);
                    String name = socketEvent.eventName().isEmpty() ? method.getName() : socketEvent.eventName();
                    SocketAdapter a = events.entrySet().stream().filter(m -> m.getValue().containsKey(name))
                            .findAny().map(Map.Entry::getKey).orElse(null);
                    if (a != null) {
                        logger.error("Adapter {} zarejestrował już handler dla {}!", a, name);
                        continue;
                    }
                    events.compute(adapter, (h, map) -> {
                        if (map == null) map = new HashMap<>();
                        map.put(name, method);
                        return map;
                    });
                    count++;
                }
            } catch (Exception e) {
                logger.error("Nie udało się zarejestrować eventu", e);
                Sentry.capture(e);
            }
        }
        logger.info("Zarejestrowano {} socket eventów - łącznie {}!", count, events.values().stream().mapToInt(Map::size).sum());
    }

    public void unregisterAdapter(SocketAdapter adapter) {
        if (!events.containsKey(adapter)) throw new IllegalArgumentException("Adapter nie jest zarejestrowany!");
        events.remove(adapter);
    }

    public void start() {
        if (socket != null) socket.disconnect();
        try {
            socket = IO.socket(Ustawienia.instance.socketAdress).open();
            socket.io().reconnectionDelay(5_000);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    connect(this, null);
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    disconnect(this, null);
                }
            });

            socket.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
//                        @SuppressWarnings("SuspiciousMethodCalls")
//                        Method method = events.values().stream().filter(m -> m.containsKey(args[0]))
//                                .findAny().map(m -> m.get(args[0])).orElse(null);

                        SocketAdapter sa = null;
                        Method m = null;

                        for (Map.Entry<SocketAdapter, Map<String, Method>> entry : events.entrySet()) {
                            Map<String, Method> value = entry.getValue();
                            if (value.containsKey(args[0])) {
                                sa = entry.getKey();
                                m = value.get(args[0]);
                            }
                        }

                        if (m == null) {
                            logger.warn("Nie znalazłem handlera dla eventu {}", args[0]);
                            return;
                        }

                        m.invoke(args[0], sa, args[1]);

                    } catch (Exception e) {
                        logger.error("Wystąpił błąd przy odbieraniu socketa", e);
                        Sentry.capture(e);
                    }
                }
            });

        } catch (Exception e) {
            Sentry.capture(e);
            logger.error("Nie udało się skonfigurować socketa!", e);
        }
    }

    @SocketEvent
    public void connect(Emitter.Listener e, Ack ack) {
        logger.info("Połączono do serwera socketów");
        socket.emit("registerFbot");
    }

    @SocketEvent
    public void disconnect(Emitter.Listener e, Ack ack) {
        logger.warn("Odłączono od serwera socketów");
    }

}
