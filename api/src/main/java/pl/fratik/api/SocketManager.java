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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.iwebpp.crypto.TweetNaclFast;
import io.sentry.Sentry;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import lombok.Getter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.api.entity.Exceptions;
import pl.fratik.core.util.GsonUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SocketManager implements WebSocketConnectionCallback, SocketAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SocketManager.class);
    private final ShardManager shardManager;
    private final Map<SocketAdapter, Map<String, Method>> events = new HashMap<>();
    private final Map<String, SocketAdapter> channels = new HashMap<>();
    private static final TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair();
    private static final AtomicLong nonce = new AtomicLong();
    private final Set<Connection> connections = new HashSet<>();

    public SocketManager(ShardManager shardManager) {
        this.shardManager = shardManager;
        registerAdapter(this);
    }

    public void registerAdapter(SocketAdapter adapter) {
        int count = 0;
        String channelName = adapter.getChannelName();
        if (channels.containsKey(channelName))
            throw new IllegalArgumentException("Kanał " + channelName + " już istnieje!");
        for (Method method : adapter.getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(SocketEvent.class)) {
                    SocketEvent socketEvent = method.getAnnotation(SocketEvent.class);
                    String name = socketEvent.eventName().isEmpty() ? method.getName() : socketEvent.eventName();
                    events.compute(adapter, (h, map) -> {
                        if (map == null) map = new HashMap<>();
                        map.put(name, method);
                        return map;
                    });
                    count++;
                }
                channels.put(channelName, adapter);
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

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        connections.add(new Connection(channel));
    }

    public static String generateUserAuthString(long id) {
        TweetNaclFast.Signature sig = new TweetNaclFast.Signature(keyPair.getPublicKey(), keyPair.getSecretKey());
        return Base64.getEncoder().encodeToString(sig.sign((id + "." + nonce.getAndUpdate(h -> {
            if (h >= 4294967295L) h = 0;
            return ++h;
        })).getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String getChannelName() {
        return null;
    }

    public int invalidate(String userId) {
        return invalidate(MiscUtil.parseSnowflake(userId));
    }

    public int invalidate(long userId) {
        int closed = 0;
        for (Connection con : new HashSet<>(connections)) {
            if (con.getAuthenticatedUser().getIdLong() != userId) continue;
            try {
                con.close(Exceptions.Codes.WS_LOGGED_OUT);
            } catch (IOException e) {
                // ignoruj
            }
            closed++;
        }
        return closed;
    }

    @Getter
    public class Connection extends AbstractReceiveListener {
        private final WebSocketChannel channel;
        private final Set<String> subscribedChannels = new HashSet<>();
        private User authenticatedUser;

        private Connection(WebSocketChannel channel) {
            this.channel = channel;
            logger.info("Nowe połączenie do WebSocket'a! {} {}", channel.getSourceAddress().getHostString(), this);
            channel.getReceiveSetter().set(this);
            channel.resumeReceives();
        }

        @Override
        protected void onError(WebSocketChannel channel, Throwable error) {
            if (authenticatedUser != null)
                Sentry.getContext().setUser(new io.sentry.event.User(authenticatedUser.getId(),
                        authenticatedUser.getName(), channel.getSourceAddress().getHostString(), null));
            Sentry.capture(error);
            Sentry.clearContext();
            logger.error(this + " - wystąpił błąd w połączeniu WebSocket'a!", error);
            try {
                if (channel.isOpen()) close(Exceptions.Codes.WS_INTERNAL_ERROR);
            } catch (IOException e) {
                logger.error(this + " - nieudane zamknięcie", e);
            }
        }

        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
            JsonObject parent;
            int type;
            JsonElement content;
            try {
                parent = GsonUtil.fromJSON(message.getData(), JsonObject.class);
                type = parent.get("t").getAsInt();
                content = Objects.requireNonNull(parent.get("d"));
            } catch (Exception e) {
                close(Exceptions.Codes.WS_INVALID_MESSAGE_FORMAT);
                return;
            }
            switch (type) {
                case 0: { // logowanie
                    handleLogin(content);
                    break;
                }
                case 1: // subskrypcja kanału
                case 2: { // unsub kanału
                    handleSubOrUnsub(content, type == 1);
                    break;
                }
                case 3: {
                    handleMessage(parent, content);
                    break;
                }
                default: {
                    close(Exceptions.Codes.WS_INVALID_MESSAGE_TYPE);
                }
            }
        }

        @Override
        protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
            logger.info("Kanał {} zamknięty!", this);
            subscribedChannels.stream().map(SocketManager.this.channels::get).forEach(a -> a.unsubscribe(this));
            connections.remove(this);
        }

        private void subscribe(String channel) throws RegisterException {
            subscribe(Collections.singleton(channel));
        }

        private synchronized void subscribe(Set<String> channels) throws RegisterException {
            for (SocketAdapter adapter : channels.stream().filter(c -> !subscribedChannels.contains(c))
                    .map(SocketManager.this.channels::get).collect(Collectors.toSet())) {
                adapter.subscribe(this);
            }
            subscribedChannels.addAll(channels);
        }

        private void unsubscribe(String channel) {
            unsubscribe(Collections.singleton(channel));
        }

        private synchronized void unsubscribe(Set<String> channels) {
            channels.stream().filter(subscribedChannels::contains)
                    .map(SocketManager.this.channels::get).forEach(a -> a.unsubscribe(this));
            subscribedChannels.removeAll(channels);
        }

        private void handleLogin(JsonElement content) throws IOException {
            byte[] signed;
            try {
                JsonObject obj = content.getAsJsonObject();
                JsonElement txt = obj.get("txt");
                if (txt.isJsonNull()) signed = null;
                else signed = Base64.getDecoder().decode(txt.getAsString());
                Set<String> channels = StreamSupport.stream(obj.get("ch").getAsJsonArray().spliterator(), false)
                        .map(JsonElement::getAsString).collect(Collectors.toSet());
                if (channels.stream().anyMatch(s -> !SocketManager.this.channels.containsKey(s))) {
                    close(Exceptions.Codes.WS_INVALID_CHANNEL);
                    return;
                }
                subscribedChannels.stream().map(SocketManager.this.channels::get).forEach(a -> a.unsubscribe(this));
                subscribedChannels.clear();
                channels.add(null);
                subscribe(channels);
            } catch (RegisterException e) {
                close(e.getExceptionCode());
                return;
            } catch (Exception e) {
                close(Exceptions.Codes.WS_INVALID_MESSAGE_FORMAT);
                return;
            }
            User user;
            if (signed == null) {
                user = null;
                logger.info("{} - anonimowe połączenie", this);
            } else try {
                TweetNaclFast.Signature sig = new TweetNaclFast.Signature(keyPair.getPublicKey(), null);
                byte[] opened = sig.open(signed);
                if (opened == null) {
                    close(Exceptions.Codes.WS_INVALID_SIGNATURE);
                    return;
                }
                String[] split = new String(opened, StandardCharsets.UTF_8).split("\\.");
                user = shardManager.retrieveUserById(MiscUtil.parseSnowflake(split[0])).complete();
                int nonce = Integer.parseUnsignedInt(split[1]);
                logger.debug("{} - zalogowano do WebSocketa {} (nonce: {})", this, user, nonce);
                authenticatedUser = user;
            } catch (Exception e) {
                close(Exceptions.Codes.WS_INVALID_SIGNATURE);
                return;
            }
            sendRawMessage(0, false, false, null, null, user == null ? null : new JsonPrimitive(user.getId()));
        }

        private void handleSubOrUnsub(JsonElement content, boolean sub) throws IOException {
            try {
                Set<String> channels;
                if (content.isJsonArray()) channels = StreamSupport.stream(content.getAsJsonArray().spliterator(), false)
                        .map(JsonElement::getAsString).collect(Collectors.toSet());
                else channels = Collections.singleton(content.getAsString());
                if (channels.stream().anyMatch(s -> !SocketManager.this.channels.containsKey(s))) {
                    close(Exceptions.Codes.WS_INVALID_CHANNEL);
                    return;
                }
                if (sub) subscribe(channels);
                else unsubscribe(channels);
            } catch (RegisterException e) {
                close(e.getExceptionCode());
                return;
            } catch (Exception e) {
                close(Exceptions.Codes.WS_INVALID_MESSAGE_FORMAT);
                return;
            }
        }

        private void handleMessage(JsonObject parent, JsonElement content) throws IOException {
            String ch;
            String topic;
            try {
                ch = parent.get("ch").isJsonNull() ? null : parent.get("ch").getAsString();
                topic = parent.get("topic").isJsonNull() ? null : parent.get("topic").getAsString();
            } catch (Exception e) {
                close(Exceptions.Codes.WS_INVALID_MESSAGE_FORMAT);
                return;
            }
            SocketAdapter adapter = channels.get(ch);
            if (adapter == null || !subscribedChannels.contains(ch)) {
                close(Exceptions.Codes.WS_INVALID_CHANNEL);
                return;
            }
            Method method = events.get(adapter).get(topic);
            if (method != null) {
                try {
                    method.invoke(adapter, this, content);
                } catch (Exception e) {
                    close(Exceptions.Codes.WS_INTERNAL_ERROR);
                    return;
                }
            }
        }

        public void sendMessage(String channel, String topic, Object content) {
            sendRawMessage(3, true, true, channel, topic,
                    content instanceof JsonElement ? (JsonElement) content : GsonUtil.GSON.toJsonTree(content));
        }

        protected void sendRawMessage(int t, boolean sendChannel, boolean sendTopic, String channel, String topic, JsonElement content) {
            JsonObject obj = new JsonObject();
            obj.addProperty("t", t);
            if (sendChannel) obj.addProperty("ch", channel);
            if (sendTopic) obj.addProperty("topic", topic);
            obj.add("d", content != null ? content : JsonNull.INSTANCE);
            WebSockets.sendText(obj.toString(), this.channel, null);
        }

        public void close(Exceptions.Codes err) throws IOException {
            WebSockets.sendText(Exceptions.Codes.getJson(err), channel, null);
            close(err.getHttpCode());
        }

        public void close(int code) throws IOException {
            channel.setCloseCode(code);
            channel.close();
        }
    }
}
