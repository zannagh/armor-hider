package de.zannagh.armorhider.paper.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Channel-narrowing behaviour for clients that speak more than one namespace.
 *
 * <p>The premise is not hypothetical - it was observed on the wire. A real Fabric 1.21.8 client
 * connecting to a real Paper 1.21.8 server registers this exact mixed set (from the Paper E2E run
 * on 2026-07-28):</p>
 *
 * <pre>
 * Injected S2C payload: de.zannagh.armorhider:combatlog_s2c_packet
 * Injected S2C payload: armorhider:settings_s2c_packet
 * Injected S2C payload: de.zannagh.armorhider:permissions_s2c_packet
 * </pre>
 *
 * <p>and correspondingly sends {@code armorhider:settings_c2s_packet} but
 * {@code de.zannagh.armorhider:combatlog_c2s_packet}. A single per-player namespace therefore
 * cannot describe such a client, which is what these tests pin down.</p>
 */
@DisplayName("ClientDialects namespace narrowing")
class ClientDialectsTest {

    /** Channels a 1.21.4 - 1.21.10 client actually registers. Mixed namespaces, deliberately. */
    private static final List<String> LEGACY_ERA_CLIENT_LISTENS = List.of(
            "armorhider:settings_s2c_packet",
            "de.zannagh.armorhider:permissions_s2c_packet",
            "de.zannagh.armorhider:combatlog_s2c_packet");

    private final ClientDialects dialects = new ClientDialects();
    private final UUID player = UUID.nameUUIDFromBytes("OfflinePlayer:ArmorHiderSmoke".getBytes());

    @Test
    @DisplayName("a client heard only on the legacy settings channel gets the legacy config alias")
    void narrowsConfigurationToTheLegacyAlias() {
        dialects.remember(player, "armorhider:settings_c2s_packet");

        List<String> selected = dialects.select(player, Channels.SERVER_CONFIGURATION_S2C);

        assertEquals(List.of("armorhider:settings_s2c_packet"), selected,
                "A client that speaks the legacy settings dialect must get the legacy config alias");
        assertTrue(LEGACY_ERA_CLIENT_LISTENS.containsAll(selected),
                "Every selected channel must be one the client actually registered");
    }

    /**
     * The defect. {@code combatlog_c2s_packet} hardcodes {@code de.zannagh.armorhider} on every
     * version >= 1.20.5, so it carries no information about the client's era - yet
     * {@link ClientDialects#remember} treats it as evidence like any other inbound channel and
     * overwrites the dialect learned from the settings packet.
     *
     * <p>Consequence on a 1.21.4 - 1.21.10 client: after it sends a single combat-log event, every
     * subsequent {@code ServerConfiguration} broadcast is narrowed to
     * {@code de.zannagh.armorhider:settings_s2c_packet} - a channel that client never registered.
     * {@code sendPluginMessage} skips unlistened channels silently, so server-wide settings changes
     * simply stop arriving, with no exception and no log line on either side.</p>
     */
    @Test
    @DisplayName("a combat-log packet must not flip the dialect learned from the settings channel")
    void combatLogDoesNotOverwriteTheSettingsDialect() {
        dialects.remember(player, "armorhider:settings_c2s_packet");
        // Same client, moments later - this channel is namespace-invariant across all >= 1.20.5.
        dialects.remember(player, "de.zannagh.armorhider:combatlog_c2s_packet");

        List<String> selected = dialects.select(player, Channels.SERVER_CONFIGURATION_S2C);

        assertTrue(LEGACY_ERA_CLIENT_LISTENS.containsAll(selected),
                () -> "Selected " + selected + ", but this client only listens on "
                        + LEGACY_ERA_CLIENT_LISTENS + ". A namespace-invariant combat-log packet was"
                        + " mistaken for evidence of the client's dialect, so the configuration"
                        + " broadcast is now aimed at a channel the client never registered and"
                        + " will be dropped silently.");
    }

    @Test
    @DisplayName("a 1.21.11+ client keeps the current dialect after a combat-log packet")
    void currentEraClientIsUnaffected() {
        dialects.remember(player, "de.zannagh.armorhider:settings_c2s_packet");
        dialects.remember(player, "de.zannagh.armorhider:combatlog_c2s_packet");

        assertEquals(List.of("de.zannagh.armorhider:settings_s2c_packet"),
                dialects.select(player, Channels.SERVER_CONFIGURATION_S2C));
    }

    /**
     * The permission packet must never be narrowed. A 1.21.4 - 1.21.10 client is legitimately
     * recorded as speaking {@code armorhider} yet listens for permissions on
     * {@code de.zannagh.armorhider} only, so narrowing selects a channel it does not listen on and
     * {@code PacketSender} drops it. This is invisible at join (the dialect is still unknown then,
     * so both aliases go out) but silently breaks the re-send that follows every inbound
     * {@code PlayerConfig} - which is what the admin UI depends on.
     */
    @Test
    @DisplayName("narrowing the permission packet would aim it at an unlistened channel")
    void permissionsMustNotBeNarrowed() {
        dialects.remember(player, "armorhider:settings_c2s_packet");

        List<String> narrowed = dialects.select(player, Channels.PERMISSIONS_S2C);

        assertTrue(narrowed.stream().noneMatch(LEGACY_ERA_CLIENT_LISTENS::contains),
                "Precondition for this test: narrowing permissions yields only channels this client"
                        + " does not listen on, which is why PacketSender must not narrow them");
        assertTrue(Channels.PERMISSIONS_S2C.stream().anyMatch(LEGACY_ERA_CLIENT_LISTENS::contains),
                "Sending permissions on every alias does reach the client");
    }

    @Test
    @DisplayName("an unheard client gets every alias rather than none")
    void unknownDialectFallsBackToBothAliases() {
        assertEquals(Channels.SERVER_CONFIGURATION_S2C,
                dialects.select(UUID.randomUUID(), Channels.SERVER_CONFIGURATION_S2C));
    }
}
