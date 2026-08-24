package de.zannagh.armorhider.server;

import de.zannagh.armorhider.net.packets.SharedRuleOverride;
import de.zannagh.armorhider.net.packets.SharedRuleTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The server's relay state for shared render rules. Everything here protects one of three things: the
 * server never relays a state it has already relayed (a per-tick-true predicate must not become a
 * per-tick broadcast), it never relays what a client sent rather than what it stored, and it never
 * hands a joining client somebody's stale session.
 */
@DisplayName("SharedRuleStore relay and dedup contract")
class SharedRuleStoreTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");

    private static List<SharedRuleOverride> hide(SharedRuleTarget target) {
        return List.of(new SharedRuleOverride(target, true, 0.0, false));
    }

    @Test
    @DisplayName("a first announcement is stored and returned for relaying")
    void firstAnnouncementIsRelayed() {
        SharedRuleStore store = new SharedRuleStore();

        var relayed = store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 1L);

        assertNotNull(relayed);
        assertEquals(1, relayed.size());
        assertEquals(SharedRuleTarget.HEAD, relayed.get(0).target);
        assertEquals(0.0, relayed.get(0).opacity);
    }

    @Test
    @DisplayName("an identical re-announcement is not relayed again")
    void identicalStateIsNotRelayed() {
        SharedRuleStore store = new SharedRuleStore();
        store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 1L);

        // The timestamp deliberately moves - only the state itself decides, or every client tick
        // would produce a broadcast.
        assertNull(store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 2L));
    }

    @Test
    @DisplayName("a changed opacity is relayed")
    void changedStateIsRelayed() {
        SharedRuleStore store = new SharedRuleStore();
        store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 1L);

        var relayed = store.put(ALICE, "Alice",
                List.of(new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.5, false)), 2L);

        assertNotNull(relayed);
        assertEquals(0.5, relayed.get(0).opacity);
    }

    @Test
    @DisplayName("a display-name change alone is relayed, so the clients re-key the entry")
    void renameIsRelayed() {
        SharedRuleStore store = new SharedRuleStore();
        store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 1L);

        assertNotNull(store.put(ALICE, "[VIP] Alice", hide(SharedRuleTarget.HEAD), 2L));
    }

    @Test
    @DisplayName("an empty state clears the entry once, and then says nothing")
    void emptyStateClearsExactlyOnce() {
        SharedRuleStore store = new SharedRuleStore();
        store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 1L);

        var cleared = store.put(ALICE, "Alice", List.of(), 2L);
        assertNotNull(cleared, "the clear itself has to reach the other clients");
        assertTrue(cleared.isEmpty());

        assertNull(store.put(ALICE, "Alice", List.of(), 3L),
                "a client that keeps announcing nothing must not produce a broadcast per tick");
    }

    @Test
    @DisplayName("no-op and malformed entries are dropped rather than stored")
    void meaninglessEntriesAreDropped() {
        SharedRuleStore store = new SharedRuleStore();

        var relayed = store.put(ALICE, "Alice", Arrays.asList(
                new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.0, false),
                new SharedRuleOverride(SharedRuleTarget.CHEST, false, 1.0, false), // says nothing
                new SharedRuleOverride(null, true, 0.0, false),                    // no target
                null), 1L);

        assertNotNull(relayed);
        assertEquals(1, relayed.size());
        assertEquals(SharedRuleTarget.HEAD, relayed.get(0).target);
    }

    @Test
    @DisplayName("a state that is only no-op entries is treated as nothing at all")
    void allNoOpEntriesAreNothing() {
        SharedRuleStore store = new SharedRuleStore();

        assertNull(store.put(ALICE, "Alice",
                List.of(new SharedRuleOverride(SharedRuleTarget.HEAD, false, 1.0, false)), 1L),
                "nothing was stored, so there is nothing to clear and nothing to relay");
    }

    @Test
    @DisplayName("opacities off the wire are clamped before they are stored")
    void opacityIsClamped() {
        SharedRuleStore store = new SharedRuleStore();

        var relayed = store.put(ALICE, "Alice", List.of(
                new SharedRuleOverride(SharedRuleTarget.HEAD, true, -4.0, false),
                new SharedRuleOverride(SharedRuleTarget.FEET, true, 17.0, false)), 1L);

        assertNotNull(relayed);
        assertEquals(0.0, relayed.get(0).opacity);
        assertEquals(1.0, relayed.get(1).opacity);
    }

    @Test
    @DisplayName("the relayed list is the stored one, not the caller's - mutating the input cannot reach a client")
    void storedStateIsIndependentOfTheCallersList() {
        SharedRuleStore store = new SharedRuleStore();
        List<SharedRuleOverride> mutable = new ArrayList<>(hide(SharedRuleTarget.HEAD));

        store.put(ALICE, "Alice", mutable, 1L);
        mutable.clear();

        var snapshot = store.snapshotExcept(BOB);
        assertEquals(1, snapshot.size());
        assertEquals(1, snapshot.get(0).overrides.size());
    }

    @Test
    @DisplayName("the stored entries are copies - mutating an override after the fact cannot reach a client")
    void storedEntriesAreIndependentOfTheCallersObjects() {
        SharedRuleStore store = new SharedRuleStore();
        SharedRuleOverride mutable = new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.0, false);

        store.put(ALICE, "Alice", List.of(mutable), 1L);
        // A SharedRuleOverride is a mutable public-field carrier deserialised straight out of the
        // inbound payload. Storing the caller's instance would let anything still holding that payload
        // rewrite the state every later joiner is told about.
        mutable.opacity = 1.0;
        mutable.target = SharedRuleTarget.FEET;

        var snapshot = store.snapshotExcept(BOB);
        assertEquals(1, snapshot.size());
        assertEquals(SharedRuleTarget.HEAD, snapshot.get(0).overrides.get(0).target);
        assertEquals(0.0, snapshot.get(0).overrides.get(0).opacity);
    }

    @Test
    @DisplayName("a snapshot skips the recipient's own state")
    void snapshotExcludesTheRecipient() {
        SharedRuleStore store = new SharedRuleStore();
        store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 1L);
        store.put(BOB, "Bob", hide(SharedRuleTarget.FEET), 1L);

        var forAlice = store.snapshotExcept(ALICE);

        assertEquals(1, forAlice.size());
        assertEquals("Bob", forAlice.get(0).playerName);
        assertEquals(BOB, forAlice.get(0).playerId);
    }

    @Test
    @DisplayName("players who have left are dropped, so a long-running server cannot accumulate entries")
    void retainOnlineDropsDepartedPlayers() {
        SharedRuleStore store = new SharedRuleStore();
        store.put(ALICE, "Alice", hide(SharedRuleTarget.HEAD), 1L);
        store.put(BOB, "Bob", hide(SharedRuleTarget.FEET), 1L);

        store.retainOnline(List.of(ALICE));

        var snapshot = store.snapshotExcept(null);
        assertEquals(1, snapshot.size());
        assertEquals("Alice", snapshot.get(0).playerName);
    }

    @Test
    @DisplayName("removing a player that never announced anything reports nothing to clear")
    void removingAnUnknownPlayerIsANoOp() {
        SharedRuleStore store = new SharedRuleStore();

        assertTrue(!store.remove(ALICE), "there was nothing to announce a clear for");
    }
}
