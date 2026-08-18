package de.zannagh.armorhider.paper.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The channel-alias contract the "dumb relay" plugin depends on. These are pure constants, but the
 * relationships between them encode two subtle, regression-prone protocol rules
 * (see {@link Channels} and {@link ClientDialects}).
 */
@DisplayName("Channels alias and dialect contract")
class ChannelsTest {

    private static final List<List<String>> EVERY_PAYLOAD = List.of(
            Channels.PLAYER_CONFIG_C2S,
            Channels.SERVER_WIDE_SETTINGS_C2S,
            Channels.COMBAT_LOG_C2S,
            Channels.SERVER_CONFIGURATION_S2C,
            Channels.PERMISSIONS_S2C,
            Channels.COMBAT_LOG_S2C,
            Channels.HANDSHAKE_S2C);

    @Test
    @DisplayName("every payload carries exactly its two namespace aliases")
    void everyPayloadHasBothNamespaceAliases() {
        for (List<String> payload : EVERY_PAYLOAD) {
            assertEquals(2, payload.size(), () -> "expected legacy + current alias, got " + payload);
            String path = payload.get(0).substring(payload.get(0).indexOf(':') + 1);
            assertTrue(payload.contains(Channels.LEGACY_NAMESPACE + ":" + path),
                    () -> "missing legacy alias in " + payload);
            assertTrue(payload.contains(Channels.CURRENT_NAMESPACE + ":" + path),
                    () -> "missing current alias in " + payload);
        }
    }

    @Test
    @DisplayName("the two namespaces are distinct")
    void namespacesAreDistinct() {
        assertEquals("armorhider", Channels.LEGACY_NAMESPACE);
        assertEquals("de.zannagh.armorhider", Channels.CURRENT_NAMESPACE);
        assertFalse(Channels.LEGACY_NAMESPACE.equals(Channels.CURRENT_NAMESPACE));
    }

    @Test
    @DisplayName("only the namespace-switching C2S payloads bear the client's dialect")
    void dialectBearingIsPlayerConfigAndServerWideOnly() {
        // The documented bug: combat-log hardcodes the current namespace on every version, so it
        // reveals nothing and must never appear here (else a combat event mislabels a legacy client).
        assertTrue(Channels.DIALECT_BEARING_C2S.containsAll(Channels.PLAYER_CONFIG_C2S));
        assertTrue(Channels.DIALECT_BEARING_C2S.containsAll(Channels.SERVER_WIDE_SETTINGS_C2S));
        for (String combatChannel : Channels.COMBAT_LOG_C2S) {
            assertFalse(Channels.DIALECT_BEARING_C2S.contains(combatChannel),
                    () -> "combat-log channel " + combatChannel + " must not be treated as dialect evidence");
        }
        assertEquals(Channels.PLAYER_CONFIG_C2S.size() + Channels.SERVER_WIDE_SETTINGS_C2S.size(),
                Channels.DIALECT_BEARING_C2S.size());
    }

    @Test
    @DisplayName("ALL covers every payload's aliases with no duplicates")
    void allIsTheDistinctUnionOfEveryPayload() {
        for (List<String> payload : EVERY_PAYLOAD) {
            assertTrue(Channels.ALL.containsAll(payload), () -> "ALL is missing " + payload);
        }
        assertEquals(Channels.ALL.stream().distinct().count(), Channels.ALL.size(), "ALL must be distinct");
        // 7 payloads x 2 aliases each, all distinct.
        assertEquals(14, Channels.ALL.size());
    }
}
