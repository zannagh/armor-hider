package de.zannagh.armorhider.smoke.fallback;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A dependency-light, in-JVM stand-in for eunomia's C# relay ({@code Eunomia.Server.Web}), faithful to the exact
 * wire contract eunomia's {@code ExternalServerClient} speaks so the fallback E2E can run with no C# server
 * running. It reproduces the parts the client depends on:
 *
 * <ul>
 *   <li>{@code GET /health} - unversioned and unauthenticated (the C# side is a bare
 *       {@code app.MapGet("/health", () => Results.Ok("ok"))}). 200 when the relay's fallback is enabled
 *       server-side, 503 when disabled, so eunomia's {@code PingClient} 2xx probe is the server-side half of
 *       the two-sided opt-in gate.</li>
 *   <li>{@code PUT /api/v<major>.<minor>/packets/{keyed,plain}} taking a {@code PacketEnvelope}, plus the
 *       unversioned {@code /api/packets/...} the real controller still carries for pre-versioning clients.
 *       Both are anonymous: 0.3.2's auth work gates the dashboard, not the mod data path.</li>
 *   <li>The rejection ladder those endpoints apply, and the socket handshake's version/identifier checks -
 *       see {@link StubRelayContract}.</li>
 *   <li>A keyed, per-{@code (scope, channel)} store; {@code store_sync} snapshot pushed on WS connect; live
 *       {@code envelope} relay to same-scope peers except the sender; strict scope isolation.</li>
 * </ul>
 *
 * Serves HTTP and the WebSocket on one port (via {@link NanoWSD}) because the client derives the ws URL from the
 * same base host:port as its REST calls. Kept behaviourally identical to the C# {@code PacketsController} /
 * {@code WebSocketMiddleware} / {@code WebSocketHandler} / {@code ConnectionManager} / {@code KeyedPacketStore}
 * so a green run here predicts a green run against the real server.
 */
public final class StubEunomiaRelay extends NanoWSD {

    private static final Gson GSON = new Gson();

    /** scope -> channel -> key -> raw payload JSON (mirrors KeyedPacketStore's per-(scope,channel) map). */
    private final Map<String, Map<String, Map<String, JsonElement>>> store = new ConcurrentHashMap<>();

    /** scope -> live sessions (the session gate + relay fan-out set). */
    private final Map<String, CopyOnWriteArrayList<StubRelaySocket>> sessions = new ConcurrentHashMap<>();

    /** Scopes an operator has blocked - the stub's {@code IServerBlockService}. Empty unless a test fills it. */
    private final Set<String> blockedScopes = ConcurrentHashMap.newKeySet();

    private final boolean fallbackEnabled;

    public StubEunomiaRelay(int port, boolean fallbackEnabled) {
        super(port);
        this.fallbackEnabled = fallbackEnabled;
    }

    /** The scheme-qualified base the client should be pointed at. */
    public String base() {
        return "http://127.0.0.1:" + getListeningPort();
    }

    /** Blocks {@code scope} as an operator would: REST puts get 403, sockets are accepted then closed with 1008. */
    public void block(String scope) {
        blockedScopes.add(scope);
    }

    /** Null-safe: an envelope need not carry a scope at all, and a scope nobody blocked is not blocked. */
    private boolean isBlocked(String scope) {
        return scope != null && blockedScopes.contains(scope);
    }

    /**
     * Refuses a malformed socket handshake with a plain 400 before any upgrade, as
     * {@code WebSocketMiddleware.TryReadHandshake} does. It has to happen here rather than in
     * {@link #openWebSocket}, because by the time NanoWSD calls that it has already committed to answering with
     * a handshake response - there is no way back to an HTTP error from inside it.
     */
    @Override
    public Response serve(IHTTPSession session) {
        if (isWebsocketRequested(session) && !handshakeIsWellFormed(session.getParameters())) {
            return status(400, "malformed websocket handshake");
        }
        return super.serve(session);
    }

    /**
     * {@code id} must parse as a UUID and {@code scope} must be present; {@code scope} and the optional
     * {@code name} (new in 0.3.2, the human-readable server label) must both fit the persisted column, since
     * the real server writes them in {@code TouchPresenceAsync} right after accepting.
     */
    private static boolean handshakeIsWellFormed(Map<String, List<String>> params) {
        String scope = first(params, "scope");
        return StubRelayContract.canonicalUuid(first(params, "id")) != null
                && scope != null && !scope.isEmpty()
                && StubRelayContract.withinLimit(scope)
                && StubRelayContract.withinLimit(first(params, "name"));
    }

    /**
     * Both remaining handshake refusals are "accept, then close with a code" on the real server rather than an
     * HTTP status, because the client only learns them through the socket: an unserved {@code v=} closes with
     * 4001, a blocked scope with 1008. The decision is taken here and carried into {@link StubRelaySocket#onOpen}
     * since that is the first point at which a frame can be written.
     */
    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        Map<String, List<String>> params = handshake.getParameters();
        String id = first(params, "id");
        String scope = first(params, "scope");
        int closeWith = 0;
        if (!StubRelayContract.isSupportedVersion(first(params, "v"))) {
            closeWith = StubRelayContract.UNSUPPORTED_VERSION_CLOSE;
        } else if (isBlocked(scope)) {
            closeWith = StubRelayContract.POLICY_VIOLATION_CLOSE;
        }
        return new StubRelaySocket(this, handshake, id, scope, closeWith);
    }

    @Override
    protected Response serveHttp(IHTTPSession session) {
        String uri = session.getUri();
        if ("/health".equals(uri)) {
            // The C# side is `Results.Ok("ok")`, which serialises the string as JSON - so the body is the four
            // bytes `"ok"`, quotes included, under application/json. NOT bare text/plain: this stub used to
            // answer that and LiveRelayContractTest caught the divergence against the real relay. Nothing reads
            // the body today (PingClient.isReachable only looks at the status code), but a stub that disagrees
            // with the server on the wire is a trap for whoever reads it next.
            return fallbackEnabled
                    ? newFixedLengthResponse(Response.Status.OK, "application/json", "\"ok\"")
                    : status(503, "fallback disabled");
        }
        String route = StubRelayContract.packetsRoute(uri);
        if (session.getMethod() == Method.PUT && route != null) {
            return handlePut(session, "keyed".equals(route));
        }
        // Drain before answering 404. NanoHTTPD does not consume an unread body, so on a keep-alive connection
        // its bytes are handed to the NEXT request on that socket, which then fails to parse - a 404 would
        // silently corrupt the request after it. Real ASP.NET drains, so this is fidelity, not a workaround.
        readBody(session);
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found");
    }

    private Response handlePut(IHTTPSession session, boolean keyed) {
        // The body is drained first regardless (see readBody), but the 413 gate is decided on the DECLARED
        // length and BEFORE any parse, as the C# controller does - so an over-large body that is also
        // malformed is a 413, not a 400.
        long contentLength = declaredContentLength(session);
        String raw = readBody(session);
        if (contentLength > StubRelayContract.MAX_BODY_BYTES) {
            return status(413, "payload too large");
        }
        JsonObject env;
        try {
            env = JsonParser.parseString(raw).getAsJsonObject();
        } catch (RuntimeException e) {
            // Unbindable body - the ASP.NET model binder answers 400 here too.
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "bad body");
        }

        int rejection = StubRelayContract.rejection(env, keyed, this::isBlocked, this::isConnected);
        if (rejection != StubRelayContract.ACCEPTED) {
            return status(rejection, "rejected");
        }

        String scope = StubRelayContract.asString(env, "scope");
        String sender = StubRelayContract.canonicalUuid(StubRelayContract.asString(env, "sender"));
        if (keyed) {
            JsonElement payload = env.get("payload");
            store.computeIfAbsent(scope, s -> new ConcurrentHashMap<>())
                    .computeIfAbsent(StubRelayContract.asString(env, "channel"), c -> new ConcurrentHashMap<>())
                    .put(StubRelayContract.asString(env, "key"), payload == null ? new JsonObject() : payload);
        }
        broadcastEnvelope(scope, env, sender);
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{}");
    }

    /**
     * Consumes the request body and returns it, or {@code ""} when it could not be read - which
     * {@link JsonParser} then rejects, landing on the same 400 an unbindable body earns on the real server.
     * Always call this, even on a path that will not look at the body: see the drain note in {@link #serveHttp}.
     */
    private static String readBody(IHTTPSession session) {
        try {
            Map<String, String> body = new LinkedHashMap<>();
            session.parseBody(body);
            // NanoHTTPD stores a PUT body as a temp file under "content" (only POST uses "postData"),
            // so read whichever is present.
            if (body.containsKey("postData")) {
                return body.get("postData");
            }
            return body.containsKey("content") ? Files.readString(Path.of(body.get("content"))) : "{}";
        } catch (IOException | ResponseException | RuntimeException e) {
            return "";
        }
    }

    /** The declared body length, or {@code -1} when the client sent none (chunked, or a bodyless request). */
    private static long declaredContentLength(IHTTPSession session) {
        String header = session.getHeaders().get("content-length");
        try {
            return header == null ? -1 : Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Relays the raw envelope to every same-scope peer except the sender (the C# BroadcastToScopeAsync). */
    private void broadcastEnvelope(String scope, JsonObject envelope, String sender) {
        JsonObject frame = new JsonObject();
        frame.addProperty("type", "envelope");
        frame.add("data", envelope);
        String json = GSON.toJson(frame);
        for (StubRelaySocket socket : socketsIn(scope)) {
            if (!socket.id().equals(sender)) {
                socket.trySend(json);
            }
        }
    }

    private boolean isConnected(String scope, String id) {
        for (StubRelaySocket socket : socketsIn(scope)) {
            if (socket.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Null-safe: an envelope may name a scope nobody has ever connected on, or no scope at all. */
    private List<StubRelaySocket> socketsIn(String scope) {
        CopyOnWriteArrayList<StubRelaySocket> list = scope == null ? null : sessions.get(scope);
        return list == null ? List.of() : list;
    }

    private static String first(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static Response status(int code, String description) {
        return newFixedLengthResponse(new NanoHTTPD.Response.IStatus() {
            @Override
            public String getDescription() {
                return code + " " + description;
            }

            @Override
            public int getRequestStatus() {
                return code;
            }
        }, "text/plain", description);
    }

    // --- session registry, driven by StubRelaySocket ---------------------------------------------

    void register(StubRelaySocket socket) {
        sessions.computeIfAbsent(socket.scope(), s -> new CopyOnWriteArrayList<>()).add(socket);
    }

    void unregister(StubRelaySocket socket) {
        CopyOnWriteArrayList<StubRelaySocket> list = sessions.get(socket.scope());
        if (list != null) {
            list.remove(socket);
        }
    }

    /** The stored channels of a scope, for a joiner's snapshot. Empty when nothing has been stored there yet. */
    Map<String, Map<String, JsonElement>> channelsIn(String scope) {
        Map<String, Map<String, JsonElement>> channels = scope == null ? null : store.get(scope);
        return channels == null ? Map.of() : channels;
    }
}
