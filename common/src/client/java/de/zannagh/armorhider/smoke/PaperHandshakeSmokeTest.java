//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.net.ClientCommunicationManager;
import de.zannagh.armorhider.client.net.ClientPacketSender;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.server.ServerConfiguration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Client half of the Paper end-to-end smoke: connects to an already-running external PaperMC
 * server and asserts the Armor Hider payload exchange actually happened over the Bukkit plugin
 * messaging channels.
 * <p>
 * This is the only test in the suite that exercises a non-Minecraft server implementation. The
 * Paper plugin cannot use the mod's packet mixins, so it force-subscribes each player's connection
 * to our channels reflectively via {@code CraftPlayer#addChannel}; if that reflection ever breaks,
 * {@code CraftPlayer#sendPluginMessage} drops every S2C payload <b>silently</b> - no exception, no
 * log line, and the mod simply appears to connect and do nothing. Only an end-to-end assertion on
 * received state can catch that, which is what this test is for.
 * <p>
 * The test <b>no-ops</b> unless the system property {@code armorhider.smoke.paper.port} is set to
 * the port of a running Paper server (wired from {@code -Psmoke.paper.port=N}); a normal
 * {@code runClientGametest} run has no Paper server and must not fail because of it.
 */
public final class PaperHandshakeSmokeTest implements FabricClientGameTest {

    /** System property carrying the port of the externally-started Paper server. */
    private static final String PORT_PROPERTY = "armorhider.smoke.paper.port";

    private static final String HOST = "127.0.0.1";

    /** ~30 s at 20 TPS - the server pushes on join, but Paper startup/chunk load can stall the tick. */
    private static final int EXCHANGE_TIMEOUT_TICKS = 600;

    /**
     * 200 ticks = 10 s at 20 TPS - the client's outgoing-traffic suppression window
     * ({@code ClientPacketSender.HANDSHAKE_TIMEOUT_MILLIS}). The handshake must arrive inside it,
     * otherwise the client would give up and drop all C2S traffic. {@code connect(...)} already blocks
     * until the player has joined, so the server-side join push (which sends the handshake) has fired
     * by the time this budget starts - this asserts the round-trip beats the suppression deadline.
     */
    private static final int HANDSHAKE_WINDOW_TICKS = 200;

    /** ~5 s at 20 TPS for the disconnect handler to clear SERVER_SUPPORTS_MOD after the connection drops. */
    private static final int DISCONNECT_RESET_TICKS = 100;

    /**
     * Permission level the Paper side seeds for the smoke player (OP). The client default is 0 and
     * nothing client-side can raise it, so observing 4 proves a PermissionPacket really arrived.
     */
    private static final int EXPECTED_PERMISSION_LEVEL = 4;

    /**
     * Ticks to stay connected after the S2C assertions pass, so the client's outbound
     * {@code PlayerConfig} can reach the plugin and be persisted before we disconnect.
     */
    private static final int C2S_DWELL_TICKS = 60;

    /** Let the join and the gamemode change settle before the first damage attempt. */
    private static final int SETTLE_TICKS = 40;

    /** Damage has to round-trip to Paper and back as a damage event before this is re-checked. */
    private static final int DAMAGE_RETRY_TICKS = 20;

    private static final int DAMAGE_ATTEMPTS = 5;

    /** Must match the identity pinned in {@code multiloader-loom.gradle.kts} for a Paper smoke run. */
    private static final String SMOKE_PLAYER = "ArmorHiderSmoke";

    /**
     * Vanilla {@code /damage}, available since 1.19.4 and therefore on every FCGT variant.
     *
     * <p>{@code generic_kill} rather than the default {@code generic}: it carries the
     * {@code bypasses_invulnerability} tag, so the damage lands whatever the player's gamemode or
     * ability flags are. Plain {@code /damage <player> <n>} was rejected with "Target is
     * invulnerable to the given damage type" on a freshly joined smoke player, which would make this
     * assertion depend on server defaults that have nothing to do with what it is testing. The
     * gamemode is forced to survival first anyway; this is belt and braces.</p>
     */
    private static final String DAMAGE_COMMAND =
            "damage " + SMOKE_PLAYER + " 4 minecraft:generic_kill";

    /** A creative/spectator player ignores the damage this test depends on. */
    private static final String SURVIVAL_COMMAND = "gamemode survival " + SMOKE_PLAYER;

    @Override
    public void runTest(ClientGameTestContext context) {
        Integer port = Integer.getInteger(PORT_PROPERTY);
        if (port == null) {
            ArmorHider.LOGGER.info("[smoke/fcgt] Paper handshake smoke skipped: -D{} not set"
                    + " (no external Paper server for this run)", PORT_PROPERTY);
            return;
        }

        ArmorHider.LOGGER.info("[smoke/fcgt] Paper handshake smoke starting against {}:{}", HOST, port);
        TestServerConnect.connect(context, HOST, port);
        try {
            assertServerHandshake(context);
            awaitExchange(context);
            assertServerWideSettings(context);
            assertPermissionPacket(context);
            assertCombatLogReachesServer(context);
            assertPermissionResend(context);
            assertHandshakeResetsOnReconnect(context, port);
            // The S2C assertions above can be satisfied within a tick or two of joining, but the
            // client's own PlayerConfig (the C2S direction) is sent from its join handler and then
            // has to reach the plugin and be stored. Disconnecting immediately raced that: the
            // server's armor-hider.json came back with empty playerConfigs/playerNameConfigs even
            // though S2C had clearly worked. Dwell so the server-side C2S assertion is meaningful.
            context.waitTicks(C2S_DWELL_TICKS);
            ArmorHider.LOGGER.info("[smoke/fcgt] Paper handshake smoke passed");
        } finally {
            // Always hand the client back in the state FCGT's runner asserts on, even on failure,
            // so a genuine assertion message isn't buried under a bogus final-state complaint.
            TestServerConnect.disconnect(context);
        }
    }

    /**
     * Polls until both S2C payloads have landed. A timeout here is not fatal on its own - the
     * per-assertion methods below produce far more precise diagnostics, so we only log and fall
     * through to them.
     */
    private void awaitExchange(ClientGameTestContext context) {
        try {
            context.waitFor(client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig() != null
                    && ArmorHiderClient.permissionLevel != 0, EXCHANGE_TIMEOUT_TICKS);
            // FCGT reports a wait timeout as a bare AssertionError, not a RuntimeException.
        } catch (AssertionError | RuntimeException e) {
            ArmorHider.LOGGER.warn("[smoke/fcgt] Payload exchange did not settle within {} ticks;"
                    + " falling through to the detailed assertions", EXCHANGE_TIMEOUT_TICKS);
        }
    }

    /**
     * Asserts the ServerConfiguration snapshot arrived and carries the deliberately non-default
     * values the Paper side seeds. Defaults are {@code true/false/false/true}, so an all-default
     * snapshot means we are reading a client-side fallback rather than server-sent state.
     */
    private void assertServerWideSettings(ClientGameTestContext context) {
        ServerConfiguration serverConfig = context.computeOnClient(
                client -> ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig());
        if (serverConfig == null) {
            throw new IllegalStateException(
                    "[smoke/fcgt] Expected a ServerConfiguration from the Paper server, observed null."
                            + " Paper may be silently dropping S2C payloads because the channel"
                            + " force-subscribe (CraftPlayer#addChannel reflection) failed - sendPluginMessage"
                            + " skips any channel the connection has not registered, without throwing.");
        }

        ServerWideSettings settings = serverConfig.serverWideSettings;
        if (settings == null) {
            throw new IllegalStateException(
                    "[smoke/fcgt] ServerConfiguration arrived but serverWideSettings was null -"
                            + " the Paper plugin is emitting a JSON shape the client cannot deserialize.");
        }

        check("enableCombatDetection", false, settings.enableCombatDetection.getValue());
        check("forceArmorHiderOff", true, settings.forceArmorHiderOff.getValue());
        check("disableArmorHiderOnInvisibilityGlobally", true,
                settings.disableArmorHiderOnInvisibilityGlobally.getValue());
        check("allowIndividualPlayerConfigurations", false,
                settings.allowIndividualPlayerConfigurations.getValue());
        ArmorHider.LOGGER.info("[smoke/fcgt] serverWideSettings matched the seeded non-default values");
    }

    /**
     * <b>The single most important assertion in the whole suite.</b>
     * <p>
     * The smoke player is seeded as an OP server-side, so a genuine PermissionPacket carries level
     * 4. The client-side default is 0 and no client code path can raise it, so level 4 cannot occur
     * unless a PermissionPacket was really received. It is therefore the only assertion that proves
     * Paper's {@code CraftPlayer#addChannel} reflection works and that S2C delivery is not being
     * silently dropped - the ServerConfiguration check above can, in principle, be satisfied by a
     * stale or locally-constructed snapshot, but this cannot.
     */
    private void assertPermissionPacket(ClientGameTestContext context) {
        int observed = context.computeOnClient(client -> ArmorHiderClient.permissionLevel);
        if (observed != EXPECTED_PERMISSION_LEVEL) {
            throw new IllegalStateException(
                    "[smoke/fcgt] Expected permissionLevel " + EXPECTED_PERMISSION_LEVEL
                            + " (the smoke player is OP'd server-side), observed " + observed + "."
                            + (observed == 0
                                    ? " 0 is the untouched client default: no PermissionPacket was received at all."
                                        + " Most likely the Paper plugin's CraftPlayer#addChannel force-subscribe"
                                        + " failed, so sendPluginMessage silently skipped every S2C payload;"
                                        + " next most likely, the channel names drifted apart between plugin and mod."
                                    : " A packet did arrive but with the wrong level: the plugin's op/LuckPerms"
                                        + " permission resolution disagrees with the seeded OP entry, or the test"
                                        + " player connected under a different name than the one that was OP'd."));
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] PermissionPacket received with level {} - Paper S2C delivery works",
                observed);
    }

    /**
     * Drives real damage on the Paper server and asserts the client's combat hook fired, which is
     * what emits the {@code CombatLogEventPacket} C2S.
     *
     * <p>Unlike {@code CombatDetectionSmokeTest} there is no integrated server to call
     * {@code hurtServer} on, so the damage is requested with the vanilla {@code /damage} command -
     * the smoke player is OP level 4 on the Paper side, which is exactly what makes that legal.
     * The client-side hook is {@code LivingEntityMixin#triggerCombat} on {@code handleDamageEvent},
     * i.e. it only fires because a real {@code ClientboundDamageEventPacket} arrived from Paper.</p>
     *
     * <p>Server-side receipt is asserted by {@code PaperE2ESmokeTest} against the plugin's log line;
     * with one connected player the relay reaches nobody, so there is nothing to observe here.</p>
     */
    private void assertCombatLogReachesServer(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.player == null) {
                throw new IllegalStateException("[smoke/fcgt] No client player to damage");
            }
            // Combat detection must be on for the packet to be emitted at all. The Paper side
            // deliberately seeds enableCombatDetection=false server-wide, so this relies on the
            // per-player config (whose default is true) - restated here so the test does not
            // silently depend on that default.
            ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName())
                    .enableCombatDetection.setValue(true);
            client.player.connection.sendCommand(SURVIVAL_COMMAND);
        });
        // A player damaged in the same tick it joins is still reported invulnerable, so let the join
        // and the gamemode change settle before the first attempt.
        context.waitTicks(SETTLE_TICKS);

        // Retried rather than one-shot: the exact length of the post-join invulnerability is a server
        // detail we do not want this assertion coupled to.
        for (int attempt = 1; attempt <= DAMAGE_ATTEMPTS; attempt++) {
            if (context.computeOnClient(client -> isInCombat())) {
                break;
            }
            int current = attempt;
            context.runOnClient(client -> {
                if (client.player == null) {
                    return;
                }
                ArmorHider.LOGGER.info("[smoke/fcgt] damage attempt {}/{} (gameMode={}, health={})",
                        current, DAMAGE_ATTEMPTS,
                        client.gameMode == null ? "?" : client.gameMode.getPlayerMode(),
                        client.player.getHealth());
                client.player.connection.sendCommand(DAMAGE_COMMAND);
            });
            context.waitTicks(DAMAGE_RETRY_TICKS);
        }

        if (!context.computeOnClient(client -> isInCombat())) {
            throw new IllegalStateException(
                    "[smoke/fcgt] Player never entered combat after " + DAMAGE_ATTEMPTS
                            + " attempts at `/" + DAMAGE_COMMAND + "`. Either the command was rejected"
                            + " (is the smoke player still OP server-side? check the Paper log for"
                            + " \"issued server command\" and any chat response) or the client damage"
                            + " hook never fired, in which case no CombatLogEventPacket was sent.");
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] combat triggered by real server damage -"
                + " CombatLogEventPacket sent C2S");
    }

    /**
     * <b>Regression test for silently-dropped S2C after the client's first C2S message.</b>
     *
     * <p>Everything asserted above happens on the join push, where the plugin has not yet recorded a
     * dialect for this connection and therefore sends on <em>both</em> namespace aliases. That masks
     * any channel-narrowing bug. This asserts the other path: reset the client's permission level,
     * send a {@code PlayerConfig}, and require the plugin's answering {@code PermissionPacket} to
     * bring it back to 4.</p>
     *
     * <p>Before the 2026-07-28 fix this failed on 1.21.4-1.21.10: those clients are recorded as
     * speaking {@code armorhider} (from the settings channel) but listen for permissions on
     * {@code de.zannagh.armorhider} only, so the narrowed send matched no listening channel and was
     * dropped without an exception or a log line on either side.</p>
     */
    private void assertPermissionResend(ClientGameTestContext context) {
        context.runOnClient(client -> {
            // 0 is unreachable by any client code path, so a later 4 can only come off the wire.
            ArmorHiderClient.permissionLevel = 0;
            ClientPacketSender.sendToServer(ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName())
                    .forNetwork());
        });

        try {
            context.waitFor(client -> ArmorHiderClient.permissionLevel == EXPECTED_PERMISSION_LEVEL,
                    EXCHANGE_TIMEOUT_TICKS);
        } catch (AssertionError | RuntimeException e) {
            int observed = context.computeOnClient(client -> ArmorHiderClient.permissionLevel);
            throw new IllegalStateException(
                    "[smoke/fcgt] After sending a PlayerConfig the server's answering PermissionPacket"
                            + " never arrived - permissionLevel is still " + observed + ", expected "
                            + EXPECTED_PERMISSION_LEVEL + ". The join-time push worked, so S2C delivery"
                            + " itself is fine; this is the post-first-C2S path, where the plugin has"
                            + " recorded a dialect for this connection and may be narrowing the send to"
                            + " an alias this client does not listen on.");
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] PermissionPacket re-send after C2S arrived - dialect"
                + " narrowing does not drop small payloads");
    }

    /**
     * Asserts the server's {@code HandshakePacket} arrives within the client's 10 s suppression
     * window. Until {@link ClientCommunicationManager#SERVER_SUPPORTS_MOD} flips, the client queues
     * and (after the window) drops all outgoing traffic, so a Paper server that never sent the
     * handshake would look "connected but inert". This proves Paper sends it and that 10 s is enough.
     */
    private void assertServerHandshake(ClientGameTestContext context) {
        try {
            context.waitFor(client -> ClientCommunicationManager.SERVER_SUPPORTS_MOD, HANDSHAKE_WINDOW_TICKS);
        } catch (AssertionError | RuntimeException e) {
            throw new IllegalStateException(
                    "[smoke/fcgt] SERVER_SUPPORTS_MOD never became true within " + HANDSHAKE_WINDOW_TICKS
                            + " ticks (10 s) of joining. The Paper plugin did not deliver a HandshakePacket in"
                            + " time, so the client would suppress all C2S traffic and the mod would appear inert."
                            + " Check that PlayerConnectionListener.onJoin calls sendHandshake and that"
                            + " Channels.HANDSHAKE_S2C is force-subscribed.", e);
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] handshake received within the 10 s window - C2S traffic unlocked");
    }

    /**
     * Asserts the handshake state is per-connection: it must reset to {@code false} on disconnect (so a
     * later vanilla server can't inherit a stale "supported"), and be re-established within the 10 s
     * window on reconnect. Exercises {@code ClientCommunicationManager}'s disconnect handler and
     * {@code ClientPacketSender.reset()} together with a full re-handshake.
     */
    private void assertHandshakeResetsOnReconnect(ClientGameTestContext context, int port) {
        TestServerConnect.disconnect(context);
        try {
            context.waitFor(client -> !ClientCommunicationManager.SERVER_SUPPORTS_MOD, DISCONNECT_RESET_TICKS);
        } catch (AssertionError | RuntimeException e) {
            throw new IllegalStateException(
                    "[smoke/fcgt] SERVER_SUPPORTS_MOD stayed true after disconnecting. The disconnect handler"
                            + " must clear it (and call ClientPacketSender.reset()), otherwise a client that next"
                            + " joins a vanilla server would keep sending custom payloads and risk being kicked.", e);
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] SERVER_SUPPORTS_MOD reset to false on disconnect");

        TestServerConnect.connect(context, HOST, port);
        try {
            context.waitFor(client -> ClientCommunicationManager.SERVER_SUPPORTS_MOD, HANDSHAKE_WINDOW_TICKS);
        } catch (AssertionError | RuntimeException e) {
            throw new IllegalStateException(
                    "[smoke/fcgt] After reconnecting, the handshake did not re-arrive within "
                            + HANDSHAKE_WINDOW_TICKS + " ticks. The reset left the gate stuck, or the server did"
                            + " not re-push the handshake on the second join.", e);
        }
        ArmorHider.LOGGER.info("[smoke/fcgt] re-handshake succeeded within the 10 s window on reconnect");
    }

    private static boolean isInCombat() {
        return ArmorHiderApi.getInstance().getCombatManagement()
                .isInCombat(ArmorHiderClient.getCurrentPlayerName());
    }

    private static void check(String name, boolean expected, Boolean observed) {
        if (observed == null || observed != expected) {
            throw new IllegalStateException(
                    "[smoke/fcgt] serverWideSettings." + name + ": expected " + expected
                            + " (deliberately non-default, seeded by the Paper side), observed " + observed
                            + ". Either the plugin never wrote the seeded config, or the client fell back to"
                            + " its own defaults because the ServerConfiguration payload failed to deserialize.");
        }
    }
}
//?}
