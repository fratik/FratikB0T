/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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

package lavalink.client.io;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.LavalinkUtil;
import lavalink.client.io.jda.JdaLink;
import lavalink.client.player.LavalinkPlayer;
import lavalink.client.player.event.PlayerEvent;
import lavalink.client.player.event.TrackEndEvent;
import lavalink.client.player.event.TrackExceptionEvent;
import lavalink.client.player.event.TrackStuckEvent;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LavalinkSocket extends ReusableWebSocket {

    private static final Logger log = LoggerFactory.getLogger(LavalinkSocket.class);

    private static final int TIMEOUT_MS = 5000;
    @NotNull
    private final String name;
    @NotNull
    private final Lavalink lavalink;
    @Nullable
    private RemoteStats stats;
    long lastReconnectAttempt = 0;
    private int reconnectsAttempted = 0;
    @NotNull
    private final URI remoteUri;
    private boolean available = false;

    LavalinkSocket(@NotNull String name, @NotNull Lavalink lavalink, @NotNull URI serverUri, Draft protocolDraft, Map<String, String> headers) {
        super(serverUri, protocolDraft, headers, TIMEOUT_MS);
        this.name = name;
        this.lavalink = lavalink;
        this.remoteUri = serverUri;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        log.info("Received handshake from server");
        available = true;
        lavalink.loadBalancer.onNodeConnect(this);
        reconnectsAttempted = 0;
    }

    @Override
    public void onMessage(String message) {
        JSONObject json = new JSONObject(message);

        if (!Objects.equals(json.getString("op"), "playerUpdate")) {
            log.debug(message);
        }

        switch (json.getString("op")) {
            case "playerUpdate":
                lavalink.getLink(json.getString("guildId"))
                        .getPlayer()
                        .provideState(json.getJSONObject("state"));
                break;
            case "stats":
                stats = new RemoteStats(json);
                break;
            case "event":
                try {
                    handleEvent(json);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                log.warn("Unexpected operation: " + json.getString("op"));
                break;
        }
    }

    /**
     * Implementation details:
     * The only events extending {@link PlayerEvent} produced by the remote server are these:
     * 1. TrackEndEvent
     * 2. TrackExceptionEvent
     * 3. TrackStuckEvent
     * 4. WebSocketClosedEvent
     */
    private void handleEvent(JSONObject json) throws IOException {
        Link link = lavalink.getLink(json.getString("guildId"));
        LavalinkPlayer player = lavalink.getLink(json.getString("guildId")).getPlayer();
        PlayerEvent event = null;

        switch (json.getString("type")) {
            case "TrackEndEvent":
                event = new TrackEndEvent(player,
                        LavalinkUtil.toAudioTrackWithData(player, json.getString("track")),
                        AudioTrackEndReason.valueOf(json.getString("reason"))
                );
                break;
            case "TrackExceptionEvent":
                Exception ex;
                if (json.has("exception")) {
                    JSONObject jsonEx = json.getJSONObject("exception");
                    ex = new FriendlyException(
                            jsonEx.getString("message"),
                            FriendlyException.Severity.valueOf(jsonEx.getString("severity")),
                            new RuntimeException(jsonEx.getString("cause"))
                    );
                } else {
                    ex = new RemoteTrackException(json.getString("error"));
                }

                event = new TrackExceptionEvent(player,
                        LavalinkUtil.toAudioTrackWithData(player, json.getString("track")), ex
                );
                break;
            case "TrackStuckEvent":
                event = new TrackStuckEvent(player,
                        LavalinkUtil.toAudioTrackWithData(player, json.getString("track")),
                        json.getLong("thresholdMs")
                );
                break;
            case "WebSocketClosedEvent":
                // Unlike the other events, this is handled by the Link instead of the LavalinkPlayer,
                // as this event is more relevant to the implementation of Link.

                link.onVoiceWebSocketClosed(
                        json.getInt("code"),
                        json.getString("reason"),
                        json.getBoolean("byRemote")
                );
                break;
            default:
                log.warn("Unexpected event type: " + json.getString("type"));
                break;
        }

        if (event != null) player.emitEvent(event);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        available = false;
        reason = reason == null ? "<no reason given>" : reason;
        if (code == 1000) {
            log.info("Connection to " + getRemoteUri() + " closed gracefully with reason: " + reason + " :: Remote=" + remote);
        } else {
            log.warn("Connection to " + getRemoteUri() + " closed unexpectedly with reason " + code + ": " + reason + " :: Remote=" + remote);
        }

        List<JdaLink> links = new ArrayList<>();
        for (Object raw : lavalink.getLinks()) {
            links.add((JdaLink) raw);
        }
        List<JdaLink> filtered = links.stream().filter(n -> Objects.equals(n.getNode(), this)).collect(Collectors.toList());
        for (JdaLink f : filtered) {
            f.onVoiceWebSocketClosed(code, reason, remote);
        }
        lavalink.loadBalancer.onNodeDisconnect(this);
    }

    @Override
    public void onError(Exception ex) {
        if (ex instanceof ConnectException) {
            log.warn("Failed to connect to " + getRemoteUri() + ", retrying in " + getReconnectInterval()/1000 + " seconds.");
            return;
        }

        log.error("Caught exception in websocket", ex);
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        // Note: If we lose connection we will reconnect and initialize properly
        if (isOpen()) {
            super.send(text);
        } else if (isConnecting()) {
            log.warn("Attempting to send messages to " + getRemoteUri() + " WHILE connecting. Ignoring.");
        }
    }

    @NotNull
    @SuppressWarnings("unused")
    public URI getRemoteUri() {
        return remoteUri;
    }

    void attemptReconnect() {
        lastReconnectAttempt = System.currentTimeMillis();
        reconnectsAttempted++;
        connect();
    }

    long getReconnectInterval() {
        return reconnectsAttempted * 2000 - 2000;
    }

    @Nullable
    public RemoteStats getStats() {
        return stats;
    }

    public boolean isAvailable() {
        return available && isOpen() && !isClosing();
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "LavalinkSocket{" +
                "name=" + name +
                ",remoteUri=" + remoteUri +
                '}';
    }
}
