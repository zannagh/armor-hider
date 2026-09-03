package de.zannagh.armorhider.smoke.fallback;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.NanoWSD.WebSocketFrame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * One client socket accepted by {@link StubEunomiaRelay}: it registers into its scope, dumps the store snapshot
 * on open, and is thereafter a fan-out target for live relays - the C# {@code WebSocketHandler} plus the
 * {@code ConnectionManager} bookkeeping around it.
 *
 * <p>A socket may also be born already refused. The real server's two post-handshake refusals (an API version
 * it does not serve, and a blocked scope) are not HTTP statuses but close codes, because the client can only
 * learn them through the socket - so those connections are accepted, closed with the code, and never
 * registered. {@link StubEunomiaRelay#openWebSocket} takes that decision; this class carries it out.</p>
 */
final class StubRelaySocket extends NanoWSD.WebSocket {

    private static final Gson GSON = new Gson();

    private final StubEunomiaRelay relay;

    /** The sender identity, canonicalized so it compares equal to a REST envelope's, as parsed Guids do. */
    private final String id;

    private final String scope;

    /** A close code to hand back instead of a session (4001 / 1008), or 0 to serve this socket normally. */
    private final int closeWith;

    StubRelaySocket(StubEunomiaRelay relay, NanoHTTPD.IHTTPSession handshake, String id, String scope, int closeWith) {
        super(handshake);
        this.relay = relay;
        this.id = StubRelayContract.canonicalUuid(id);
        this.scope = scope;
        this.closeWith = closeWith;
    }

    /** The canonical sender uuid this socket authenticates - what the REST session gate matches against. */
    String id() {
        return id;
    }

    String scope() {
        return scope;
    }

    @Override
    protected void onOpen() {
        if (closeWith != 0) {
            // Refused, so it never enters the session map: it must not satisfy the REST session gate, and it
            // must not receive relays. The real server likewise closes before OnConnectionAdded.
            closeWithCode(closeWith);
            return;
        }
        relay.register(this);
        pushSnapshot();
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        relay.unregister(this);
    }

    @Override
    protected void onMessage(WebSocketFrame message) {
        // Inbound frames are heartbeats only; server -> client is the only data channel.
    }

    @Override
    protected void onPong(WebSocketFrame pong) {
    }

    @Override
    protected void onException(IOException exception) {
    }

    void trySend(String json) {
        try {
            send(json);
        } catch (IOException ignored) {
            // dead socket - dropped like the C# ConnectionManager does
        }
    }

    /**
     * Sends a close frame carrying an arbitrary status code. NanoWSD's own {@code close(CloseCode, ...)} only
     * takes the RFC 6455 enum, which has no room for an application code, and 4001 is precisely the value the
     * client keys "unsupported version" off - so the frame is assembled by hand: the two-byte big-endian code,
     * then the UTF-8 reason, exactly as the wire format specifies.
     */
    private void closeWithCode(int code) {
        byte[] reason = (code == StubRelayContract.UNSUPPORTED_VERSION_CLOSE
                ? "unsupported api version" : "server blocked").getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[2 + reason.length];
        payload[0] = (byte) (code >> 8);
        payload[1] = (byte) code;
        System.arraycopy(reason, 0, payload, 2, reason.length);
        try {
            sendFrame(new WebSocketFrame(WebSocketFrame.OpCode.Close, true, payload));
        } catch (IOException ignored) {
            // The socket is being refused either way; a peer that will not read it must not stall us.
        }
    }

    /** One store_sync frame per stored channel in this scope (the C# WebSocketHandler.PushSnapshotAsync). */
    private void pushSnapshot() {
        Map<String, Map<String, JsonElement>> channels = relay.channelsIn(scope);
        channels.forEach((channel, entries) -> {
            JsonObject sync = new JsonObject();
            sync.addProperty("channel", channel);
            JsonObject entryObj = new JsonObject();
            entries.forEach((key, payload) -> entryObj.addProperty(key, payload.toString()));
            sync.add("entries", entryObj);

            JsonObject frame = new JsonObject();
            frame.addProperty("type", "store_sync");
            frame.add("data", sync);
            trySend(GSON.toJson(frame));
        });
    }
}
