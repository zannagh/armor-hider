package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.net.packets.SharedRuleOverride;
import de.zannagh.armorhider.net.packets.SharedRuleTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The receiving half of shared render rules. The store is static (it is read from the render path,
 * where an instance lookup would be pure overhead), so every case clears it first.
 */
@DisplayName("AhSharedRuleStore receive contract")
class AhSharedRuleStoreTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");

    @BeforeEach
    @AfterEach
    void clearStore() {
        AhSharedRuleStore.clear();
    }

    private static List<SharedRuleOverride> hide(SharedRuleTarget target) {
        return List.of(new SharedRuleOverride(target, true, 0.0, false));
    }

    @Test
    @DisplayName("nothing is shared until something arrives, and the render path can see that on one read")
    void startsEmpty() {
        assertTrue(AhSharedRuleStore.isEmpty());
        assertNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD));
    }

    @Test
    @DisplayName("a received state is readable under the name it was relayed with")
    void storesByName() {
        assertTrue(AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD)));

        assertFalse(AhSharedRuleStore.isEmpty());
        SharedRuleOverride stored = AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD);
        assertNotNull(stored);
        assertEquals(0.0, stored.opacity);
        assertNull(AhSharedRuleStore.get("Alice", AhRuleTarget.FEET), "only the announced target");
        assertNull(AhSharedRuleStore.get("Bob", AhRuleTarget.HEAD), "only the announced player");
    }

    @Test
    @DisplayName("a repeated identical state reports no change, so no render cache is invalidated for nothing")
    void identicalStateReportsNoChange() {
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD));

        assertFalse(AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD)));
    }

    @Test
    @DisplayName("an empty state clears the player and empties the fast path again")
    void emptyStateClearsThePlayer() {
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD));

        assertTrue(AhSharedRuleStore.put("Alice", ALICE, List.of()));
        assertNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD));
        assertTrue(AhSharedRuleStore.isEmpty());
    }

    @Test
    @DisplayName("a state is replaced wholesale, never merged into the previous one")
    void stateIsReplacedNotMerged() {
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD));
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.FEET));

        assertNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD),
                "a rule that stopped matching must revert, not linger from the previous packet");
        assertNotNull(AhSharedRuleStore.get("Alice", AhRuleTarget.FEET));
    }

    @Test
    @DisplayName("a renamed player does not leave their old name hidden forever")
    void renameDropsTheOldEntry() {
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD));

        AhSharedRuleStore.put("[VIP] Alice", ALICE, hide(SharedRuleTarget.HEAD));

        assertNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD));
        assertNotNull(AhSharedRuleStore.get("[VIP] Alice", AhRuleTarget.HEAD));
    }

    @Test
    @DisplayName("two players sharing at once do not disturb each other")
    void playersAreIndependent() {
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD));
        AhSharedRuleStore.put("Bob", BOB, hide(SharedRuleTarget.FEET));

        assertNotNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD));
        assertNotNull(AhSharedRuleStore.get("Bob", AhRuleTarget.FEET));
        assertEquals(2, AhSharedRuleStore.knownPlayers().size());
    }

    @Test
    @DisplayName("malformed entries are skipped without losing the rest of that player's state")
    void malformedEntriesAreSkipped() {
        AhSharedRuleStore.put("Alice", ALICE, Arrays.asList(
                new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.0, false),
                new SharedRuleOverride(null, true, 0.0, false),
                new SharedRuleOverride(SharedRuleTarget.CHEST, false, 1.0, false),
                null));

        assertNotNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD));
        assertNull(AhSharedRuleStore.get("Alice", AhRuleTarget.CHEST));
    }

    @Test
    @DisplayName("an opacity off the wire is clamped before the render path can multiply a colour with it")
    void opacityIsClamped() {
        AhSharedRuleStore.put("Alice", ALICE, List.of(
                new SharedRuleOverride(SharedRuleTarget.HEAD, true, 9.5, false),
                new SharedRuleOverride(SharedRuleTarget.FEET, true, -2.0, false)));

        assertEquals(1.0, AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD).opacity);
        assertEquals(0.0, AhSharedRuleStore.get("Alice", AhRuleTarget.FEET).opacity);
    }

    @Test
    @DisplayName("a blank name is refused rather than stored under a key nothing can look up")
    void blankNamesAreRefused() {
        assertFalse(AhSharedRuleStore.put("", ALICE, hide(SharedRuleTarget.HEAD)));
        assertFalse(AhSharedRuleStore.put(null, ALICE, hide(SharedRuleTarget.HEAD)));
        assertTrue(AhSharedRuleStore.isEmpty());
    }

    @Test
    @DisplayName("departed players are pruned by id, and an entry with no id is kept - it cannot be proven stale")
    void pruningMatchesOnIdOnly() {
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD));
        AhSharedRuleStore.put("Bob", BOB, hide(SharedRuleTarget.FEET));
        AhSharedRuleStore.put("Nameless", null, hide(SharedRuleTarget.LEGS));

        assertTrue(AhSharedRuleStore.retainOnline(List.of(ALICE)));

        assertNotNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD));
        assertNull(AhSharedRuleStore.get("Bob", AhRuleTarget.FEET));
        assertNotNull(AhSharedRuleStore.get("Nameless", AhRuleTarget.LEGS));
    }

    @Test
    @DisplayName("disconnecting drops everything - shared state belongs to one connection")
    void clearDropsEverything() {
        AhSharedRuleStore.put("Alice", ALICE, hide(SharedRuleTarget.HEAD));

        AhSharedRuleStore.clear();

        assertTrue(AhSharedRuleStore.isEmpty());
        assertNull(AhSharedRuleStore.get("Alice", AhRuleTarget.HEAD));
    }

    @Test
    @DisplayName("the wire and client target enums stay in step, so no target can be silently undeliverable")
    void wireAndClientTargetsMap() {
        for (AhRuleTarget target : AhRuleTarget.values()) {
            assertEquals(target, AhRuleTarget.fromWire(target.toWire()),
                    () -> "AhRuleTarget." + target + " does not round-trip through the wire enum");
        }
        for (SharedRuleTarget wire : SharedRuleTarget.values()) {
            assertNotNull(AhRuleTarget.fromWire(wire),
                    () -> "SharedRuleTarget." + wire + " has no client-side twin, so anything shared"
                            + " for it would be dropped on arrival");
        }
        assertNull(AhRuleTarget.fromWire(null));
    }
}
