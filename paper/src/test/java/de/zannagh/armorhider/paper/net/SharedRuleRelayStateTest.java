package de.zannagh.armorhider.paper.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plugin's half of the shared-rule relay, which has to behave exactly like the mod's
 * {@code SharedRuleStore} without understanding the payload: same dedup, same "empty clears", same
 * authenticated envelope. The array of outcomes itself is moved opaquely, which is what keeps the
 * plugin working against a client shipping a newer shape.
 */
@DisplayName("SharedRuleRelayState relay and dedup contract")
class SharedRuleRelayStateTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");

    private static JsonArray overrides(String target, double opacity) {
        JsonObject entry = new JsonObject();
        entry.addProperty("target", target);
        entry.addProperty("affectsOpacity", true);
        entry.addProperty("opacity", opacity);
        entry.addProperty("disableGlint", false);
        JsonArray array = new JsonArray();
        array.add(entry);
        return array;
    }

    @Test
    @DisplayName("a first announcement produces a notification carrying the authenticated envelope")
    void firstAnnouncementIsRelayed() {
        SharedRuleRelayState state = new SharedRuleRelayState();

        JsonObject relayed = state.put(ALICE, "Alice", overrides("HEAD", 0.0), 7L);

        assertNotNull(relayed);
        assertEquals("Alice", relayed.get(SharedRuleRelayState.PLAYER_NAME).getAsString());
        assertEquals(ALICE.toString(), relayed.get(SharedRuleRelayState.PLAYER_ID).getAsString());
        assertEquals(7L, relayed.get(SharedRuleRelayState.TIMESTAMP).getAsLong());
        assertEquals(1, relayed.getAsJsonArray(SharedRuleRelayState.OVERRIDES).size());
    }

    @Test
    @DisplayName("an unknown field inside an outcome survives the relay untouched")
    void unknownFieldsAreRelayedOpaquely() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        JsonArray fromNewerClient = overrides("HEAD", 0.0);
        fromNewerClient.get(0).getAsJsonObject().addProperty("somethingTheServerHasNeverHeardOf", 42);

        JsonObject relayed = state.put(ALICE, "Alice", fromNewerClient, 1L);

        assertNotNull(relayed);
        assertEquals(42, relayed.getAsJsonArray(SharedRuleRelayState.OVERRIDES).get(0)
                .getAsJsonObject().get("somethingTheServerHasNeverHeardOf").getAsInt());
    }

    @Test
    @DisplayName("the stored state is independent of the caller's array - mutating the input cannot reach it")
    void storedStateIsIndependentOfTheCallersArray() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        JsonArray inbound = overrides("HEAD", 0.0);

        state.put(ALICE, "Alice", inbound, 1L);
        // A JsonArray is mutable and this one belongs to the decoded inbound payload. If the store kept
        // the reference, this would rewrite what every later joiner is told - and silently defeat the
        // dedup below, since the "previous" state would mutate along with the new one.
        inbound.remove(0);
        inbound.add("garbage");

        List<JsonObject> snapshot = state.snapshotExcept(BOB);
        assertEquals(1, snapshot.size());
        JsonArray stored = snapshot.get(0).getAsJsonArray(SharedRuleRelayState.OVERRIDES);
        assertEquals(1, stored.size());
        assertEquals("HEAD", stored.get(0).getAsJsonObject().get("target").getAsString());
    }

    @Test
    @DisplayName("a relayed notification cannot be used to edit the stored state")
    void relayedNotificationDoesNotAliasTheStore() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        JsonObject relayed = state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);

        relayed.getAsJsonArray(SharedRuleRelayState.OVERRIDES).remove(0);

        JsonArray stillStored = state.snapshotExcept(BOB).get(0)
                .getAsJsonArray(SharedRuleRelayState.OVERRIDES);
        assertEquals(1, stillStored.size());
    }

    @Test
    @DisplayName("an identical re-announcement is not relayed again")
    void identicalStateIsNotRelayed() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);

        assertNull(state.put(ALICE, "Alice", overrides("HEAD", 0.0), 2L),
                "only the state decides - otherwise a per-tick-true predicate is a per-tick broadcast");
    }

    @Test
    @DisplayName("a changed outcome is relayed")
    void changedStateIsRelayed() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);

        assertNotNull(state.put(ALICE, "Alice", overrides("HEAD", 0.5), 2L));
    }

    @Test
    @DisplayName("a rename alone is relayed, so the clients re-key the entry")
    void renameIsRelayed() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);

        assertNotNull(state.put(ALICE, "[VIP] Alice", overrides("HEAD", 0.0), 2L));
    }

    @Test
    @DisplayName("an empty state clears the entry once, and then says nothing")
    void emptyStateClearsExactlyOnce() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);

        JsonObject cleared = state.put(ALICE, "Alice", new JsonArray(), 2L);
        assertNotNull(cleared);
        assertTrue(cleared.getAsJsonArray(SharedRuleRelayState.OVERRIDES).isEmpty());

        assertNull(state.put(ALICE, "Alice", new JsonArray(), 3L));
        assertNull(state.put(ALICE, "Alice", null, 4L));
    }

    @Test
    @DisplayName("removing a player announces the clear once and only if they had announced anything")
    void removeAnnouncesOnce() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        assertNull(state.remove(ALICE, "Alice", 1L), "nothing was stored, so nothing to clear");

        state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);

        JsonObject cleared = state.remove(ALICE, "Alice", 2L);
        assertNotNull(cleared);
        assertTrue(cleared.getAsJsonArray(SharedRuleRelayState.OVERRIDES).isEmpty());
        assertNull(state.remove(ALICE, "Alice", 3L));
    }

    @Test
    @DisplayName("a snapshot skips the recipient's own state")
    void snapshotExcludesTheRecipient() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);
        state.put(BOB, "Bob", overrides("FEET", 0.0), 1L);

        List<JsonObject> forAlice = state.snapshotExcept(ALICE);

        assertEquals(1, forAlice.size());
        assertEquals("Bob", forAlice.get(0).get(SharedRuleRelayState.PLAYER_NAME).getAsString());
    }

    @Test
    @DisplayName("players who have left are dropped")
    void retainOnlineDropsDepartedPlayers() {
        SharedRuleRelayState state = new SharedRuleRelayState();
        state.put(ALICE, "Alice", overrides("HEAD", 0.0), 1L);
        state.put(BOB, "Bob", overrides("FEET", 0.0), 1L);

        state.retainOnline(List.of(BOB));

        List<JsonObject> remaining = state.snapshotExcept(null);
        assertEquals(1, remaining.size());
        assertEquals("Bob", remaining.get(0).get(SharedRuleRelayState.PLAYER_NAME).getAsString());
    }
}
