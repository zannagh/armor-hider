//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.server.ServerConfiguration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Two-client config-propagation end-to-end smoke: proves that when one player changes their Armor
 * Hider config, another player learns about it <em>through the server</em>, entirely over eunomia's
 * transport. This is the coverage the single-client {@link PaperHandshakeSmokeTest} cannot give - a
 * lone client only ever sees its own S2C join push, never another player's config.
 *
 * <p>The scenario is driven sequentially by {@code PaperE2ESmokeTest}, which forks the same variant
 * twice against one Paper server:</p>
 * <ol>
 *   <li><b>sender</b> ({@code role=sender}) joins, sets its helmet opacity to the marker value, sends
 *       its {@code PlayerConfig} C2S, and disconnects. The plugin stores that config keyed by the
 *       player's UUID and name.</li>
 *   <li><b>reader</b> ({@code role=reader}) joins afterwards. The plugin's join push
 *       ({@code ArmorHiderServerNet#pushOnJoin}) sends the aggregate {@code ServerConfiguration},
 *       which now carries the sender's stored config. The reader asserts the sender's entry is present
 *       with the marker opacity.</li>
 * </ol>
 *
 * <p>Sequential rather than concurrent on purpose: two clients of the same variant would share one
 * {@code run/} directory. The join-snapshot path exercises the same store-and-serialize-and-deliver
 * pipeline as the live {@code broadcastExcept}, so a persisted marker reaching the reader proves the
 * cross-client delivery works.</p>
 *
 * <p>Role, peer name and marker come from {@code -Darmorhider.smoke.twoclient.*} (forwarded from the
 * {@code -Psmoke.twoclient.*} gradle properties). Absent any of them, the test no-ops so a normal
 * {@code runClientGametest} is unaffected.</p>
 */
public final class TwoClientConfigPropagationSmokeTest implements FabricClientGameTest {

    /** Reused from the single-client row: the port of the externally-started Paper server. */
    private static final String PORT_PROPERTY = "armorhider.smoke.paper.port";
    private static final String ROLE_PROPERTY = "armorhider.smoke.twoclient.role";
    private static final String PEER_PROPERTY = "armorhider.smoke.twoclient.peer";
    private static final String MARKER_PROPERTY = "armorhider.smoke.twoclient.marker";

    private static final String HOST = "127.0.0.1";

    /** A non-default opacity (default is 1.0) used when the harness does not pin one explicitly. */
    private static final double DEFAULT_MARKER = 0.35;

    /** Doubles round-trip through JSON, so compare with a tolerance rather than for exact equality. */
    private static final double MARKER_EPSILON = 1.0e-6;

    /** ~30 s at 20 TPS for the join push / capability probe to settle. */
    private static final int EXCHANGE_TIMEOUT_TICKS = 600;

    /** Let the sender's C2S config reach the plugin and be persisted before disconnecting. */
    private static final int SENDER_DWELL_TICKS = 60;

    @Override
    public void runTest(ClientGameTestContext context) {
        Integer port = Integer.getInteger(PORT_PROPERTY);
        String role = System.getProperty(ROLE_PROPERTY);
        if (port == null || role == null || role.isBlank()) {
            ArmorHider.LOGGER.info("[smoke/fcgt] Two-client propagation smoke skipped: -D{} and -D{} must both be set",
                    PORT_PROPERTY, ROLE_PROPERTY);
            return;
        }
        double marker = parseMarker();

        ArmorHider.LOGGER.info("[smoke/fcgt] Two-client propagation smoke starting: role={} marker={} against {}:{}",
                role, marker, HOST, port);
        TestServerConnect.connect(context, HOST, port);
        try {
            switch (role) {
                case "sender" -> runSender(context, marker);
                case "reader" -> runReader(context, marker);
                default -> throw new IllegalArgumentException(
                        "[smoke/fcgt] Unknown two-client role '" + role + "' (want 'sender' or 'reader')");
            }
        } finally {
            TestServerConnect.disconnect(context);
        }
    }

    /** Sets the marker opacity on the local config and pushes it to the server, then dwells so it persists. */
    private void runSender(ClientGameTestContext context, double marker) {
        // Wait until the server config has arrived (proof this is an Armor Hider server, and the guard the
        // client's own save() checks before it will transmit) before mutating and sending.
        awaitServerConfig(context, "sender");

        context.runOnClient(client -> {
            PlayerConfig local = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();
            local.helmetOpacity.setValue(marker);
            // setLocalPlayerConfig persists and, because a server config is present and we are connected,
            // transmits the config C2S via eunomia's send path.
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setLocalPlayerConfig(local);
            ArmorHider.LOGGER.info("[smoke/fcgt] sender pushed helmetOpacity={} as {}",
                    marker, ArmorHiderClient.getCurrentPlayerName());
        });

        // Give the plugin time to receive, store by UUID+name and persist before we drop the connection.
        context.waitTicks(SENDER_DWELL_TICKS);
        ArmorHider.LOGGER.info("[smoke/fcgt] sender done - marker config sent and dwelled");
    }

    /** Reads the peer's config out of the join-snapshot ServerConfiguration and asserts the marker. */
    private void runReader(ClientGameTestContext context, double marker) {
        String peer = System.getProperty(PEER_PROPERTY);
        if (peer == null || peer.isBlank()) {
            throw new IllegalStateException("[smoke/fcgt] reader role requires -D" + PEER_PROPERTY);
        }
        awaitServerConfig(context, "reader");

        // The peer's config rides in on the join push, but the push and the capability probe are two
        // separate S2C messages, so poll the snapshot until the peer's entry with the marker shows up.
        try {
            context.waitFor(client -> peerMarkerMatches(peer, marker), EXCHANGE_TIMEOUT_TICKS);
        } catch (AssertionError | RuntimeException e) {
            PlayerConfig peerConfig = context.computeOnClient(
                    client -> peerConfig(peer));
            throw new IllegalStateException(String.format(
                    "[smoke/fcgt] reader never observed peer '%s' with helmetOpacity=%s in the server config."
                            + " Observed peer entry: %s. Either the sender's C2S config never reached the"
                            + " plugin, the plugin did not persist it under the sender's name, or the join"
                            + " push did not carry it back S2C.",
                    peer, marker,
                    peerConfig == null ? "<absent>" : ("helmetOpacity=" + peerConfig.helmetOpacity.getValue())), e);
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] reader observed peer '{}' with the marker opacity {} - "
                + "cross-client config propagation verified", peer, marker);
    }

    /** True once the server config contains {@code peer} with an opacity within epsilon of {@code marker}. */
    private static boolean peerMarkerMatches(String peer, double marker) {
        PlayerConfig peerConfig = peerConfig(peer);
        return peerConfig != null
                && Math.abs(peerConfig.helmetOpacity.getValue() - marker) < MARKER_EPSILON;
    }

    /** The peer's PlayerConfig out of the received ServerConfiguration, or null if not present yet. */
    private static PlayerConfig peerConfig(String peer) {
        ServerConfiguration serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        return serverConfig == null ? null : serverConfig.getPlayerConfigOrDefault(peer);
    }

    private void awaitServerConfig(ClientGameTestContext context, String role) {
        try {
            context.waitFor(client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig() != null,
                    EXCHANGE_TIMEOUT_TICKS);
        } catch (AssertionError | RuntimeException e) {
            throw new IllegalStateException("[smoke/fcgt] " + role
                    + " never received a ServerConfiguration from the Paper server within "
                    + EXCHANGE_TIMEOUT_TICKS + " ticks - the S2C join push did not arrive.", e);
        }
    }

    private static double parseMarker() {
        String raw = System.getProperty(MARKER_PROPERTY);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MARKER;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("[smoke/fcgt] -D" + MARKER_PROPERTY + " is not a number: " + raw, e);
        }
    }
}
//?}
