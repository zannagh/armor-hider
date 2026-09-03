package de.zannagh.armorhider.smoke.fallback;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.AhPackets;
import de.zannagh.armorhider.net.packets.AhReplicatedPlayerConfig;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.smoke.fallback.LiveRelayProbe.WsOutcome;
import de.zannagh.eunomia.clients.ExternalClientTransport;
import de.zannagh.eunomia.clients.ExternalServerClient;
import de.zannagh.eunomia.clients.RelayConnectionState;
import de.zannagh.eunomia.common.ApiVersion;
import de.zannagh.eunomia.keyed.KeyPath;
import de.zannagh.eunomia.keyed.ReplicatedClientStore;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import de.zannagh.eunomia.networking.packets.KeyedPacket;
import de.zannagh.eunomia.networking.serialization.NetworkSerializer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the wire contract {@link StubEunomiaRelay} reproduces against the REAL relay, so the hermetic suite is
 * measured against the server it claims to imitate instead of only against the C# source it was written from.
 * Everything asserted here is a gate reachable <em>without</em> a Minecraft account, because the live relay
 * enforces the Mojang session check on {@code /ws}: a synthetic uuid cannot open a socket, so the store/relay/
 * snapshot flow is out of reach unless a real account uuid is supplied (see {@link FullFlow}).
 *
 * <p>Self-skipping, not opt-in: the class probes {@code /health} once in {@link #probeRelay()} and assumes past
 * itself when the relay is unreachable, so an offline {@code ./gradlew test} stays green while a machine with
 * network gets the coverage for free. Point it elsewhere with {@code -Darmorhider.relay.live=<base-url>}.</p>
 *
 * <p>The relay under observation is production, not a fixture: every scope used is a non-bindable
 * {@code armorhider.e2e.*:0} (see {@link LiveRelayProbe#testScope()}), no request is repeated for volume, and
 * nothing outside the documented client surface is touched.</p>
 */
class LiveRelayContractTest {

    private static final Logger LOG = LoggerFactory.getLogger("ArmorHiderLiveRelayContract");

    /** Derived, never spelled out - a 0.4 bump must move this test's routes with the client. See StubRelayContract. */
    private static final String VERSIONED_PACKETS = "/api/v" + ApiVersion.CURRENT + "/packets/";

    /** The pre-versioning prefix the C# controller still carries as a second {@code [Route]}. */
    private static final String LEGACY_PACKETS = "/api/packets/";

    /** {@code PacketsController.MaxBodyBytes}; the 413 is decided on Content-Length, before the body is read. */
    private static final int MAX_BODY_BYTES = 1024 * 1024;

    @BeforeAll
    static void probeRelay() {
        Assumptions.assumeTrue(LiveRelayProbe.reachable(),
                "live relay " + LiveRelayProbe.base() + " is not reachable; skipping the live wire-contract checks");
    }

    @Test
    void healthAnswersOk() throws Exception {
        HttpResponse<String> response = LiveRelayProbe.get("/health");
        assertEquals(2, response.statusCode() / 100, "the relay's /health is the client's reachability probe");
        // The body is the server-side fallback-enabled marker. Note the shape: the C# endpoint is
        // `Results.Ok("ok")`, which is a JSON *string* - `"ok"` with quotes, content-type application/json - not
        // the bare text/plain `ok` StubEunomiaRelay answers with. That divergence is live and unfixed here (the
        // stub is deliberately untouched by this test), and it is harmless only because PingClient.isReachable()
        // reads nothing but the status; any future code that parses this body would break against one of the two.
        assertEquals("ok", unquote(response.body().trim()), "/health carries an ok body");
    }

    /** Strips the quotes an ASP.NET {@code Results.Ok(string)} wraps its payload in, so both shapes compare equal. */
    private static String unquote(String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1)
                : value;
    }

    /**
     * The specific regression a path drift would cause: one of the two prefixes starts 404ing. Both must land on
     * the same controller action, so both must produce the same non-404 verdict for an identical envelope - here
     * the 409 session gate, since neither request has a socket behind it.
     */
    @Test
    void versionedAndLegacyPacketRoutesReachTheSameHandler() throws Exception {
        String scope = LiveRelayProbe.testScope();
        String body = LiveRelayProbe.plainEnvelope(scope, UUID.randomUUID().toString());

        int versioned = LiveRelayProbe.put(VERSIONED_PACKETS + "plain", body).statusCode();
        int legacy = LiveRelayProbe.put(LEGACY_PACKETS + "plain", body).statusCode();

        assertNotEquals(404, versioned, VERSIONED_PACKETS + "plain must be routed");
        assertNotEquals(404, legacy, LEGACY_PACKETS + "plain must still be routed for pre-versioning clients");
        assertEquals(versioned, legacy, "both prefixes are [Route]s on one action, so they must agree");
    }

    /** The gate that stops REST spoofing: a put is only honoured for an identity already on a socket. */
    @Test
    void putFromAnIdentityWithNoLiveSocketIsRefused() throws Exception {
        String body = LiveRelayProbe.plainEnvelope(LiveRelayProbe.testScope(), UUID.randomUUID().toString());
        assertEquals(409, LiveRelayProbe.put(VERSIONED_PACKETS + "plain", body).statusCode(),
                "a well-formed envelope with no live session is a 409");
    }

    /** {@code Guid.TryParse}, not a null check - a non-uuid sender fails before the session gate is consulted. */
    @Test
    void putWithANonUuidSenderIsRejected() throws Exception {
        String body = LiveRelayProbe.plainEnvelope(LiveRelayProbe.testScope(), "not-a-uuid");
        assertEquals(400, LiveRelayProbe.put(VERSIONED_PACKETS + "plain", body).statusCode(),
                "a sender that is not a uuid is a 400, ahead of the 409");
    }

    @Test
    void oversizeBodyIsRejected() throws Exception {
        String body = LiveRelayProbe.oversizePlainEnvelope(
                LiveRelayProbe.testScope(), UUID.randomUUID().toString(), MAX_BODY_BYTES + 1024);
        assertTrue(body.length() > MAX_BODY_BYTES, "the probe body must actually exceed the cap");
        assertEquals(413, LiveRelayProbe.put(VERSIONED_PACKETS + "plain", body).statusCode(),
                "a declared Content-Length above MaxBodyBytes is a 413");
    }

    /** The handshake's own {@code Guid.TryParse}: the upgrade is refused outright, so no socket ever exists. */
    @Test
    void handshakeWithANonUuidIdIsRejected() throws Exception {
        WsOutcome outcome = LiveRelayProbe.handshake("not-a-uuid", LiveRelayProbe.testScope(), ApiVersion.CURRENT);
        assertNotNull(outcome.failure(), "a non-uuid id must fail the upgrade, not open a socket");
        assertEquals(400, outcome.handshakeStatus(), "a non-uuid id is a 400 on the upgrade response");
    }

    /**
     * Accept-then-close, which is why this cannot be asserted on the handshake status: an unsupported {@code v=}
     * still gets its 101, and the verdict arrives only in the close frame. 4001 rather than 1008 on purpose -
     * the client treats "this relay does not speak your version" as a different terminal outcome from "an
     * operator blocked this scope" (see StubRelayContract.UNSUPPORTED_VERSION_CLOSE).
     */
    @Test
    void handshakeWithAnUnsupportedVersionIsAcceptedThenClosed() throws Exception {
        WsOutcome outcome = LiveRelayProbe.handshake(UUID.randomUUID().toString(), LiveRelayProbe.testScope(), "9.9");
        assertEquals(101, outcome.handshakeStatus(), "an unsupported version is still upgraded first");
        assertEquals(4001, outcome.closeCode(), "then closed with the unsupported-version application code");
    }

    /**
     * The constraint that makes every live socket-backed assertion impossible, asserted rather than annotated:
     * the deployed relay verifies the id against Mojang's session service, so an id that owns no Minecraft
     * account is refused. This is the gate the hermetic backends switch off (the forked C# relay is started with
     * {@code EUNOMIA_DISABLE_MOJANG_GATE=1}; the stub never had one), and it is exactly why the fallback E2E
     * cannot simply be repointed at production.
     */
    @Test
    void handshakeWithAnUnauthenticatedIdentityIsForbidden() throws Exception {
        WsOutcome outcome = LiveRelayProbe.handshake(
                UUID.randomUUID().toString(), LiveRelayProbe.testScope(), ApiVersion.CURRENT);
        assertNotNull(outcome.failure(), "a synthetic uuid must not get a socket on a Mojang-gated relay");
        assertEquals(403, outcome.handshakeStatus(), "an id with no Minecraft account is a 403 on the upgrade");
    }

    /**
     * The half of the contract that needs an identity Mojang recognises: armor-hider's real
     * {@link PlayerConfig} stored through eunomia's real client stack and replayed to a later connect as a
     * snapshot. Opt-in via {@code -Darmorhider.relay.live.uuid=<uuid>} because there is no way to synthesise a
     * passing identity - see {@link #handshakeWithAnUnauthenticatedIdentityIsForbidden()}.
     *
     * <p>Live relay to a <em>peer</em> is deliberately not covered: it would need a second real account, and
     * that is more than the guarantee is worth. Store and snapshot are the two the mod actually depends on.</p>
     */
    @Nested
    class FullFlow {

        private static final KeyedPacket<AhReplicatedPlayerConfig> CHANNEL = AhPackets.PLAYER_CONFIG_REPLICATED;

        @Test
        void configIsStoredAndSnapshottedBackToALaterConnect() throws Exception {
            String raw = LiveRelayProbe.liveUuid();
            Assumptions.assumeTrue(raw != null,
                    "set -D" + LiveRelayProbe.UUID_PROPERTY + "=<real-account-uuid> to run the live full flow");
            UUID player = UUID.fromString(raw);
            String scope = LiveRelayProbe.testScope();
            String base = LiveRelayProbe.base();

            // Same startup the mod performs: armor-hider's Gson carries the config type adapters, so the payload
            // takes its real on-wire shape rather than a default-reflection one.
            NetworkSerializer.setGson(ArmorHider.GSON);
            CommunicationManager.resetForTesting();
            EunomiaTestState.rebindStoreSync();
            CommunicationManager.register(CHANNEL);
            try {
                PlayerConfig config = new PlayerConfig(player, "ArmorHiderLiveContract");
                config.helmetOpacity.setValue(0.42);
                var expected = ArmorHider.GSON.toJsonTree(config.forNetwork());

                // The relay broadcasts an envelope to same-scope sockets EXCLUDING the sender, so a sender can
                // never observe its own put on its own session - only a later connect sees it, as a store_sync
                // snapshot. That is why there is no "it came back to me" assertion here: it would contradict the
                // contract. Storage is proven by the rejoin below, which is the guarantee the mod depends on.
                AtomicReference<RelayConnectionState> state = new AtomicReference<>();
                ExternalServerClient sender = new ExternalServerClient(
                        base, scope, "live-contract", player, LOG, () -> { }, state::set);
                sender.start();
                CommunicationManager.setClientTransport(new ExternalClientTransport(sender));
                CommunicationManager.setExternalTransportActive(true);
                AhReplicatedPlayerConfig payload = AhReplicatedPlayerConfig.forNetwork(player, config);

                // The put is session-gated (409 without a live socket) and the socket connects asynchronously,
                // so wait for the relay to report OPEN before sending rather than spraying retries at a
                // production server.
                assertTrue(await(30_000, () -> state.get() == RelayConnectionState.OPEN),
                        "the relay should accept the socket for a Mojang-valid identity, reaching OPEN");

                // OPEN is the CLIENT's view of its own socket; the server registers the session in its
                // ConnectionManager a moment later, and until it does a put is still refused with 409. Observed
                // against the live relay: sending immediately on OPEN logs
                // `Relay rejected ... (409): "No live websocket session for this identity/scope."`.
                // So poll until the server itself admits the session. A PLAIN put is the right probe: it is
                // session-gated exactly like a keyed one but is never stored, so this settles the race without
                // writing anything.
                String probe = LiveRelayProbe.plainEnvelope(scope, player.toString());
                assertTrue(await(30_000, () -> {
                    try {
                        return LiveRelayProbe.put(VERSIONED_PACKETS + "plain", probe).statusCode() != 409;
                    } catch (Exception e) {
                        return false;
                    }
                }), "the relay should register the websocket session, stopping the 409 on a put");

                CommunicationManager.sendToServer(CHANNEL, payload);
                // Give the keyed write time to land before dropping the socket - stopping mid-flight would
                // race the store rather than test it.
                Thread.sleep(1_000);
                sender.stop();

                // A fresh mirror on a later connect must be populated from storage, not from a live relay.
                CommunicationManager.resetForTesting();
                EunomiaTestState.rebindStoreSync();
                CommunicationManager.register(CHANNEL);
                ReplicatedClientStore<AhReplicatedPlayerConfig> rejoin =
                        new ReplicatedClientStore<>(1, AhReplicatedPlayerConfig.class, CHANNEL).enableClient();
                ExternalServerClient rejoined = new ExternalServerClient(base, scope, "live-contract", player, LOG);
                rejoined.start();
                assertTrue(await(30_000, () -> rejoin.store().contains(KeyPath.of(player))),
                        "a later connect should receive the stored config as a snapshot");
                assertEquals(expected, ArmorHider.GSON.toJsonTree(
                                rejoin.store().get(KeyPath.of(player)).orElseThrow().config),
                        "the snapshotted config matches what was sent");
                rejoined.stop();
            } finally {
                CommunicationManager.resetForTesting();
            }
        }

        private boolean await(long millis, java.util.function.BooleanSupplier condition) throws InterruptedException {
            long deadline = System.nanoTime() + millis * 1_000_000L;
            while (System.nanoTime() < deadline) {
                if (condition.getAsBoolean()) {
                    return true;
                }
                Thread.sleep(250);
            }
            return false;
        }
    }
}
