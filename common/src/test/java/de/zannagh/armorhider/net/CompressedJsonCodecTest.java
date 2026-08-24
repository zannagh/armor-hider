package de.zannagh.armorhider.net;

import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.net.packets.SharedRuleStatePacket;
import de.zannagh.armorhider.server.ServerConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which payloads are held to the tight <em>serverbound</em> size ceiling.
 *
 * <p>Getting this wrong is not a local bug: a C2S payload that slips past the check and exceeds
 * {@code ServerboundCustomPayloadPacket.MAX_PAYLOAD_SIZE} is decoded by a vanilla server as a
 * {@code DiscardedPayload}, which throws and disconnects the sender. The classification therefore has
 * to stay in step with what the mod actually sends, and it cannot be derived from
 * {@link PayloadRegistry} at runtime because that class is a stub below 1.20.5.</p>
 */
@DisplayName("CompressedJsonCodec serverbound classification")
class CompressedJsonCodecTest {

    @Test
    @DisplayName("every payload the mod sends to the server is classified serverbound")
    void everyC2SPayloadIsHeldToTheServerboundLimit() {
        assertTrue(CompressedJsonCodec.isServerboundPayload(PlayerConfig.empty()));
        assertTrue(CompressedJsonCodec.isServerboundPayload(new ServerWideSettings()));
        assertTrue(CompressedJsonCodec.isServerboundPayload(
                new CombatLogEventPacket(null, java.util.UUID.randomUUID())));
        assertTrue(CompressedJsonCodec.isServerboundPayload(
                new SharedRuleStatePacket("Zannagh", java.util.List.of())));
    }

    @Test
    @DisplayName("clientbound payloads keep the roomier ceiling")
    void clientboundPayloadsAreNotRestricted() {
        // ServerConfiguration legitimately runs into the hundreds of KiB on a large server; holding it
        // to the serverbound limit would make the mod unusable there.
        assertFalse(CompressedJsonCodec.isServerboundPayload(new ServerConfiguration()));
    }

    //? if >= 1.20.5 {
    /**
     * The list in {@code isServerboundPayload} is hand-written, so it can silently fall behind a new
     * C2S payload. The registry is the real declaration of which types travel which way, so on every
     * version that has one, the two must agree.
     */
    @Test
    @DisplayName("the hand-written list covers exactly the registry's C2S payloads")
    void classificationMatchesThePayloadRegistry() {
        PayloadRegistry.init();

        for (var entry : PayloadRegistry.getAllC2S().values()) {
            Object instance = instantiate(entry.type().id().toString());
            assertTrue(CompressedJsonCodec.isServerboundPayload(instance),
                    () -> "C2S payload " + entry.type().id() + " is registered as serverbound but"
                            + " CompressedJsonCodec.isServerboundPayload() does not recognise it, so it"
                            + " would be encoded against the clientbound ceiling and could disconnect the"
                            + " sender on a vanilla server. Add it to that method's list.");
        }
    }

    /**
     * Builds a sample of each registered C2S payload. Deliberately a hard failure on an unknown id
     * rather than a skip: a new C2S payload must show up here so its classification is checked.
     */
    private static Object instantiate(String payloadId) {
        return switch (payloadId.substring(payloadId.indexOf(':') + 1)) {
            case "settings_c2s_packet" -> PlayerConfig.empty();
            case "server_wide_settings" -> new ServerWideSettings();
            case "combatlog_c2s_packet" -> new CombatLogEventPacket(null, java.util.UUID.randomUUID());
            case "shared_rules_c2s_packet" -> new SharedRuleStatePacket("Zannagh", java.util.List.of());
            default -> throw new AssertionError("Unknown C2S payload '" + payloadId + "'. It was added to"
                    + " PayloadRegistry without being classified in CompressedJsonCodec"
                    + ".isServerboundPayload() or sampled here.");
        };
    }
    //?}
}
