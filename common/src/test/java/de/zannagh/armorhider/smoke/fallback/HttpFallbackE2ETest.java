package de.zannagh.armorhider.smoke.fallback;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.AhPackets;
import de.zannagh.armorhider.net.packets.AhReplicatedPlayerConfig;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.eunomia.clients.ExternalClientTransport;
import de.zannagh.eunomia.clients.ExternalServerClient;
import de.zannagh.eunomia.clients.PingClient;
import de.zannagh.eunomia.keyed.KeyPath;
import de.zannagh.eunomia.keyed.ReplicatedClientStore;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import de.zannagh.eunomia.networking.packets.KeyedPacket;
import de.zannagh.eunomia.networking.serialization.NetworkSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof that armor-hider's per-player config propagates over eunomia's HTTP/WebSocket fallback exactly
 * as it would with the server running eunomia. It drives armor-hider's REAL {@link PlayerConfig} (wrapped as the
 * production {@link AhReplicatedPlayerConfig}) through eunomia's REAL client stack - {@link ExternalServerClient},
 * {@link ExternalClientTransport} and {@link ReplicatedClientStore} - against a relay, asserting: a sender's config
 * is stored + relayed live to a same-scope peer, replayed as a snapshot to a later joiner, isolated from other
 * scopes, and rejected (409) for an identity with no live socket; plus the two-sided opt-in gate.
 *
 * <p>Runs against the in-JVM {@link StubEunomiaRelay} by default (so it works with no C# server running) and,
 * when {@code -Darmorhider.relay.dotnet=<path-to-eunomia/csharp>} is set, against the real C# relay - the same
 * assertions for both. Gated on {@code -Darmorhider.fallback.e2e} so a normal {@code ./gradlew test} skips it.</p>
 */
class HttpFallbackE2ETest {

    private static final Logger LOG = LoggerFactory.getLogger("ArmorHiderFallbackE2E");
    private static final Gson RAW = new Gson();

    /** The real production channel per-player config rides over the relay. */
    private static final KeyedPacket<AhReplicatedPlayerConfig> CHANNEL = AhPackets.PLAYER_CONFIG_REPLICATED;

    private static final long RECEIVE_MILLIS = 15_000;

    private final HttpClient http = HttpClient.newHttpClient();
    private RelayUnderTest relay;

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeTrue(System.getProperty("armorhider.fallback.e2e") != null,
                "set -Darmorhider.fallback.e2e to run the HTTP/WebSocket fallback E2E");
        // Resolve payloads with armor-hider's own Gson (its config type adapters), just like the mod installs
        // at startup - otherwise the wrapped PlayerConfig would not (de)serialize to its real on-wire shape.
        NetworkSerializer.setGson(ArmorHider.GSON);
        CommunicationManager.resetForTesting();
        // resetForTesting() does not reach StoreSyncClient's one-shot registration flag; without this the
        // store_sync handler stays bound to a dead manager and snapshots vanish. See EunomiaTestState.
        EunomiaTestState.rebindStoreSync();
        CommunicationManager.register(CHANNEL);
        relay = RelayUnderTest.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (relay != null) {
            relay.close();
        }
        CommunicationManager.resetForTesting();
    }

    @Test
    void configIsStoredRelayedLiveAndSnapshotted() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID carol = UUID.randomUUID();
        String scope = "mc.one:25565";
        PlayerConfig aliceConfig = sampleConfig(alice, "ArmorHiderSmokeA", 0.35);
        JsonObject expected = ArmorHider.GSON.toJsonTree(aliceConfig.forNetwork()).getAsJsonObject();

        // Alice sends her config through the real client stack (ExternalServerClient + ExternalClientTransport).
        ExternalServerClient aliceClient = new ExternalServerClient(relay.base(), scope, "alice", alice, LOG);
        aliceClient.start();
        CommunicationManager.setClientTransport(new ExternalClientTransport(aliceClient));
        // Drive the REAL send gate exactly as the client transport selector does when it installs the relay:
        // this opens the gate to the external transport, so a plain sendToServer(...) reaches the relay
        // instead of being dropped (the gate suppresses serverbound sends to a non-Eunomia MC server).
        CommunicationManager.setExternalTransportActive(true);
        AhReplicatedPlayerConfig payload = AhReplicatedPlayerConfig.forNetwork(alice, aliceConfig);

        // A same-scope peer receives the LIVE relay (not the sender). Retrying the send covers async WS-connect
        // timing; a keyed put is idempotent. Proves store + live relay + wire fidelity of the real config.
        try (MockWs peer = openSocket(UUID.randomUUID(), scope)) {
            String frame = awaitFrame(peer, "envelope", () ->
                    CommunicationManager.sendToServer(CHANNEL, payload));
            assertNotNull(frame, "a same-scope peer should receive the live relay");
            assertEquals(expected, configFromEnvelopeFrame(frame),
                    "the live-relayed config matches what Alice sent, field for field");
        }
        aliceClient.stop();

        // A later joiner receives the STORED config as a snapshot on connect - through the real
        // ReplicatedClientStore mirror, exactly as the mod applies it in-game.
        ReplicatedClientStore<AhReplicatedPlayerConfig> carolMirror =
                new ReplicatedClientStore<>(1, AhReplicatedPlayerConfig.class, CHANNEL).enableClient();
        ExternalServerClient carolClient = new ExternalServerClient(relay.base(), scope, "carol", carol, LOG);
        carolClient.start();
        assertTrue(await(() -> carolMirror.store().contains(KeyPath.of(alice))),
                "a newcomer's mirror should receive Alice's stored config as a snapshot");
        assertEquals(expected, ArmorHider.GSON.toJsonTree(carolMirror.store().get(KeyPath.of(alice)).orElseThrow().config),
                "the snapshotted config matches what Alice sent");
        carolClient.stop();
    }

    @Test
    void scopeIsolationAndSessionGate() throws Exception {
        UUID alice = UUID.randomUUID();
        String scope1 = "mc.one:25565";
        String scope2 = "mc.two:25565";
        PlayerConfig aliceConfig = sampleConfig(alice, "ArmorHiderSmokeA", 0.2);

        // Alice stores her config on scope1 via the real client.
        ExternalServerClient aliceClient = new ExternalServerClient(relay.base(), scope1, "alice", alice, LOG);
        aliceClient.start();
        CommunicationManager.setClientTransport(new ExternalClientTransport(aliceClient));
        CommunicationManager.setExternalTransportActive(true);
        // Confirm the store landed via a raw same-scope peer that receives the live relay.
        try (MockWs peer = openSocket(UUID.randomUUID(), scope1)) {
            AhReplicatedPlayerConfig payload = AhReplicatedPlayerConfig.forNetwork(alice, aliceConfig);
            assertTrue(await(() -> {
                CommunicationManager.sendToServer(CHANNEL, payload);
                try {
                    return peer.poll(200) != null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }), "a same-scope peer should see the relay");
        }

        // A client on a DIFFERENT scope gets no snapshot carrying Alice's data.
        try (MockWs otherScope = openSocket(UUID.randomUUID(), scope2)) {
            assertNull(otherScope.poll(1000), "a different scope must not receive Alice's config");
        }

        // A REST put from an identity with no live socket is refused (session gate).
        assertEquals(409, rawKeyedPut(scope1, UUID.randomUUID(), aliceConfig));
        aliceClient.stop();
    }

    @Test
    void fallbackNeedsServerSideEnable() {
        // Server-side enable (the selector's server-side half): eunomia's ClientTransportSelector only routes to
        // the relay when PingClient.isReachable() is true, so a reachable, fallback-enabled relay answers /health
        // 2xx while a disabled or absent one does not. The client-opt-in half is eunomia-common's EunomiaConfig
        // (enableExternalFallback + address), exercised inside eunomia's own ClientTransportSelector - it is not
        // on armor-hider's core-only classpath, so it is asserted there, not here. Both are required (ANDed).
        assertTrue(PingClient.isReachable(relay.base()), "the running relay is server-side enabled");
        try (RelayUnderTest disabled = RelayUnderTest.startStub(false)) {
            assertFalse(PingClient.isReachable(disabled.base()), "a fallback-disabled relay must not be used");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertFalse(PingClient.isReachable("http://127.0.0.1:1"), "an unreachable relay must not be used");
    }

    // --- helpers -------------------------------------------------------------------------------------

    private static PlayerConfig sampleConfig(UUID id, String name, double helmetOpacity) {
        PlayerConfig config = new PlayerConfig(id, name);
        config.helmetOpacity.setValue(helmetOpacity);
        return config;
    }

    /** Fires {@code trigger} and polls {@code socket} until a WsFrame of {@code type} arrives (or the deadline). */
    private String awaitFrame(MockWs socket, String type, Runnable trigger) throws InterruptedException {
        long deadline = System.nanoTime() + RECEIVE_MILLIS * 1_000_000L;
        while (System.nanoTime() < deadline) {
            trigger.run();
            String raw = socket.poll(300);
            if (raw != null && type.equals(JsonParser.parseString(raw).getAsJsonObject().get("type").getAsString())) {
                return raw;
            }
        }
        return null;
    }

    /** Extracts the carried PlayerConfig (as a JSON tree) from a relayed {@code envelope} WsFrame. */
    private JsonObject configFromEnvelopeFrame(String rawFrame) {
        JsonObject envelope = JsonParser.parseString(rawFrame).getAsJsonObject().getAsJsonObject("data");
        AhReplicatedPlayerConfig wrapper = ArmorHider.GSON.fromJson(envelope.get("payload"), AhReplicatedPlayerConfig.class);
        return ArmorHider.GSON.toJsonTree(wrapper.config).getAsJsonObject();
    }

    private boolean await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + RECEIVE_MILLIS * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    private int rawKeyedPut(String scope, UUID sender, PlayerConfig config) throws Exception {
        JsonObject env = new JsonObject();
        env.addProperty("scope", scope);
        env.addProperty("channel", CHANNEL.channelKey());
        env.addProperty("key", sender.toString());
        env.addProperty("replicated", true);
        env.addProperty("sender", sender.toString());
        env.add("payload", ArmorHider.GSON.toJsonTree(AhReplicatedPlayerConfig.forNetwork(sender, config)));
        HttpRequest request = HttpRequest.newBuilder(URI.create(relay.base() + "/api/packets/keyed"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(RAW.toJson(env)))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private MockWs openSocket(UUID id, String scope) throws Exception {
        String query = "/ws?id=" + id + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
        URI uri = URI.create(relay.base().replaceFirst("^http", "ws") + query);
        return new MockWs(http, uri);
    }

    /** A raw WebSocket client that queues each complete text frame - to observe the relay directly. */
    private static final class MockWs implements AutoCloseable {
        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final StringBuilder buffer = new StringBuilder();
        private final WebSocket webSocket;

        MockWs(HttpClient http, URI uri) throws Exception {
            this.webSocket = http.newWebSocketBuilder()
                    .buildAsync(uri, new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket socket) {
                            socket.request(1);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
                            buffer.append(data);
                            if (last) {
                                frames.add(buffer.toString());
                                buffer.setLength(0);
                            }
                            socket.request(1);
                            return null;
                        }
                    })
                    .get(10, TimeUnit.SECONDS);
        }

        String poll(long millis) throws InterruptedException {
            return frames.poll(millis, TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {
                // best effort
            }
        }
    }
}
