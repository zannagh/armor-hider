package de.zannagh.armorhider.smoke.fallback;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The transport plumbing {@link LiveRelayContractTest} drives a <em>real, remote</em> relay with - kept out of the
 * test itself so the assertions there read as the contract they are checking. Nothing here is specific to
 * armor-hider's packets; it is HTTP/WebSocket mechanics plus the two shapes eunomia's client puts on the wire
 * (the packet envelope and the {@code /ws} handshake query).
 *
 * <p>Deliberately separate from {@link RelayUnderTest}, which owns relays the test <em>starts</em> (the in-JVM
 * stub, or a forked {@code dotnet run}) and therefore may reset, block scopes on, and shut down. A live relay is
 * someone's production instance: it can only be observed, and every request here is sized accordingly.</p>
 */
final class LiveRelayProbe {

    /** Overrides the relay under observation; the default is the public instance the mod ships pointing at. */
    static final String BASE_PROPERTY = "armorhider.relay.live";

    /** A REAL Minecraft account uuid, which the optional full-flow test needs to get past the Mojang gate. */
    static final String UUID_PROPERTY = "armorhider.relay.live.uuid";

    static final String DEFAULT_BASE = "https://eunomia.zannagh.me";

    /** Short on purpose: an offline or DNS-less machine must reach the {@code assumeTrue} quickly, not stall CI. */
    static final Duration TIMEOUT = Duration.ofSeconds(5);

    /** Sockets get their own budget - a handshake to a remote host plus a server-initiated close is not instant. */
    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(15);

    private static final Gson RAW = new Gson();

    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private LiveRelayProbe() {
    }

    static String base() {
        String value = System.getProperty(BASE_PROPERTY);
        return value == null || value.isBlank() ? DEFAULT_BASE : value.trim();
    }

    /** The configured real-account uuid for the full-flow test, or {@code null} when it was not supplied. */
    static String liveUuid() {
        String value = System.getProperty(UUID_PROPERTY);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * A scope that is obviously this test's traffic and will not collide with a real Minecraft server's
     * {@code host:port}. Port 0 is not bindable, so no genuine server can ever announce this scope.
     */
    static String testScope() {
        return "armorhider.e2e." + UUID.randomUUID().toString().substring(0, 8) + ":0";
    }

    /** {@code true} when {@code /health} answers at all - the gate the whole class self-skips on. */
    static boolean reachable() {
        try {
            return get("/health").statusCode() / 100 == 2;
        } catch (Exception e) {
            return false;
        }
    }

    static HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(TIMEOUT)
                .GET()
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * A well-formed {@code /packets/plain} envelope - the exact field set eunomia's client sends, so anything the
     * server rejects it for is a real gate rather than a malformed-request artifact. The payload is a trivial
     * marker object: these puts are all expected to be refused before storage, so nothing meaningful is carried.
     */
    static String plainEnvelope(String scope, String sender) {
        JsonObject payload = new JsonObject();
        payload.addProperty("probe", "armorhider-live-contract");
        return RAW.toJson(envelope(scope, sender, payload));
    }

    /** {@link #plainEnvelope} with the payload inflated past a given size, to trip the Content-Length gate. */
    static String oversizePlainEnvelope(String scope, String sender, int payloadBytes) {
        JsonObject payload = new JsonObject();
        payload.addProperty("probe", "x".repeat(payloadBytes));
        return RAW.toJson(envelope(scope, sender, payload));
    }

    private static JsonObject envelope(String scope, String sender, JsonObject payload) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("scope", scope);
        envelope.addProperty("channel", "armorhider:live_contract_probe");
        envelope.addProperty("replicated", false);
        envelope.addProperty("sender", sender);
        envelope.add("payload", payload);
        return envelope;
    }

    /**
     * Opens {@code /ws} with the given handshake query and reports how the relay disposed of it.
     *
     * <p>The two failure modes are structurally different and the test has to tell them apart: a rejected
     * handshake never becomes a socket (the JDK completes the build exceptionally with a
     * {@link WebSocketHandshakeException} carrying the HTTP response), whereas an <em>accepted-then-closed</em>
     * socket returns 101 first and only reveals its verdict in the close frame. Reporting both fields lets each
     * assertion name which one it expects.</p>
     *
     * @param version the {@code v=} segment; {@code null} omits the parameter entirely (a pre-versioning client).
     */
    static WsOutcome handshake(String id, String scope, String version) throws Exception {
        StringBuilder query = new StringBuilder("/ws?id=").append(encode(id))
                .append("&scope=").append(encode(scope))
                .append("&name=").append(encode("armorhider-live-contract"));
        if (version != null) {
            query.append("&v=").append(encode(version));
        }
        URI uri = URI.create(base().replaceFirst("^http", "ws") + query);

        CompletableFuture<Integer> closeCode = new CompletableFuture<>();
        WebSocket socket;
        try {
            socket = HTTP.newWebSocketBuilder()
                    .connectTimeout(SOCKET_TIMEOUT)
                    .buildAsync(uri, new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket ws) {
                            ws.request(1);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                            // Snapshots and relays are not this probe's business; keep draining so the close
                            // frame is actually delivered rather than stuck behind an unread message.
                            ws.request(1);
                            return null;
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                            closeCode.complete(statusCode);
                            return null;
                        }
                    })
                    .get(SOCKET_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WebSocketHandshakeException handshake) {
                return new WsOutcome(handshake.getResponse().statusCode(), null, handshake);
            }
            throw e;
        }

        try {
            return new WsOutcome(101, closeCode.get(SOCKET_TIMEOUT.toSeconds(), TimeUnit.SECONDS), null);
        } catch (TimeoutException e) {
            // The relay accepted us and kept the socket open - a legitimate session, not a rejection.
            return new WsOutcome(101, null, null);
        } finally {
            socket.abort();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * How a {@code /ws} handshake ended.
     *
     * @param handshakeStatus the HTTP status of the upgrade response - 101 when the socket was accepted.
     * @param closeCode       the code the server closed an accepted socket with, or {@code null} if it stayed open.
     * @param failure         the handshake exception when the upgrade was refused, otherwise {@code null}.
     */
    record WsOutcome(int handshakeStatus, Integer closeCode, WebSocketHandshakeException failure) {
    }
}
