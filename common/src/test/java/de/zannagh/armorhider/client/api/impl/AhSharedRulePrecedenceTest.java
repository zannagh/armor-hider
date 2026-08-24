package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.client.api.ArmorHiderRenderApi;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.SharedRuleOverride;
import de.zannagh.armorhider.net.packets.SharedRuleTarget;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a remote player's shared outcome and this viewer's own rules combine, and what a shared rule
 * actually announces.
 *
 * <p>The rules under test all use the {@code *Matching} form with a predicate that never touches
 * {@code ctx.player()}: there is no client and no level in a unit test, so resolving a player would
 * be a lookup against nothing. Everything else on the context is real.</p>
 */
@DisplayName("Shared render rule precedence")
class AhSharedRulePrecedenceTest {

    private static final String ALICE = "Alice";
    private static final UUID ALICE_ID = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final double CONFIG_BASE = 0.8;

    private PlayerConfig config;

    @BeforeEach
    @AfterEach
    void reset() {
        AhRenderRuleRegistryImpl.clear();
        AhSharedRuleStore.clear();
    }

    @BeforeEach
    void freshConfig() {
        config = PlayerConfig.empty();
    }

    private double evaluatedOpacity() {
        return AhRenderRuleRegistryImpl
                .evaluate(AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE)
                .opacity();
    }

    private void aliceShares(SharedRuleOverride override) {
        AhSharedRuleStore.put(ALICE, ALICE_ID, List.of(override));
    }

    @Test
    @DisplayName("a remote player's shared hide applies here with no local rule involved")
    void remoteOutcomeApplies() {
        aliceShares(new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.0, false));

        var outcome = AhRenderRuleRegistryImpl.evaluate(
                AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE);

        assertTrue(outcome.changed());
        assertEquals(0.0, outcome.opacity());
    }

    @Test
    @DisplayName("a remote outcome for one player does not touch another")
    void remoteOutcomeIsPerPlayer() {
        aliceShares(new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.0, false));

        var outcome = AhRenderRuleRegistryImpl.evaluate(
                AhRuleTarget.HEAD, "Bob", EquipmentSlot.HEAD, null, false, config, CONFIG_BASE);

        assertFalse(outcome.changed(), "Bob shares nothing, so his config value stands");
    }

    @Test
    @DisplayName("a viewer's stronger local rule overrules what the other player shares")
    void strongerLocalRuleWins() {
        aliceShares(new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.0, false));
        ArmorHiderRenderApi.rule(EquipmentSlot.HEAD)
                .priority(ArmorHiderRenderApi.defaultPriority() - 1)
                .opacity(1.0f)
                .whenMatching(ctx -> true);

        assertEquals(1.0, evaluatedOpacity(),
                "the viewer's own stronger rule decides what the viewer sees");
    }

    @Test
    @DisplayName("a weaker local rule loses to what the other player shares")
    void weakerLocalRuleLoses() {
        aliceShares(new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.0, false));
        ArmorHiderRenderApi.rule(EquipmentSlot.HEAD)
                .priority(ArmorHiderRenderApi.defaultPriority() + 1)
                .opacity(1.0f)
                .whenMatching(ctx -> true);

        assertEquals(0.0, evaluatedOpacity());
    }

    @Test
    @DisplayName("at equal priority the lower opacity wins, remote or local")
    void equalPriorityHidesMost() {
        aliceShares(new SharedRuleOverride(SharedRuleTarget.HEAD, true, 0.25, false));
        ArmorHiderRenderApi.setOpacityWhenMatching(EquipmentSlot.HEAD, 0.75f, ctx -> true);

        assertEquals(0.25, evaluatedOpacity());
    }

    @Test
    @DisplayName("a shared glint suppression applies whatever the local rules do to opacity")
    void remoteGlintIsNotPriorityBanded() {
        aliceShares(new SharedRuleOverride(SharedRuleTarget.HEAD, false, 1.0, true));
        ArmorHiderRenderApi.rule(EquipmentSlot.HEAD)
                .priority(ArmorHiderRenderApi.defaultPriority() - 100)
                .opacity(1.0f)
                .whenMatching(ctx -> true);

        var outcome = AhRenderRuleRegistryImpl.evaluate(
                AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE);

        assertTrue(outcome.disableGlint());
        assertEquals(1.0, outcome.opacity());
    }

    @Test
    @DisplayName("a glint-only share leaves the opacity the viewer derived alone")
    void glintOnlyShareDoesNotMoveOpacity() {
        aliceShares(new SharedRuleOverride(SharedRuleTarget.HEAD, false, 0.1, true));

        var outcome = AhRenderRuleRegistryImpl.evaluate(
                AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE);

        assertEquals(CONFIG_BASE, outcome.opacity(),
                "affectsOpacity was false, so the sender's own 0.1 must never reach the render path");
        assertTrue(outcome.disableGlint());
    }

    @Test
    @DisplayName("only shared rules are announced, and viewer-side rules stay put")
    void onlySharedRulesAreAnnounced() {
        ArmorHiderRenderApi.setOpacityWhenMatching(EquipmentSlot.HEAD, 0.2f, ctx -> true);

        assertNull(AhRenderRuleRegistryImpl.evaluateShared(
                        AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE),
                "an unshared rule is a statement about what I see, not about me");

        ArmorHiderRenderApi.rule(EquipmentSlot.HEAD).shared().hide().whenMatching(ctx -> true);

        var announced = AhRenderRuleRegistryImpl.evaluateShared(
                AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE);
        assertNotNull(announced);
        assertEquals(SharedRuleTarget.HEAD, announced.target);
        assertTrue(announced.affectsOpacity);
        assertEquals(0.0, announced.opacity);
    }

    @Test
    @DisplayName("a shared glint rule announces the glint alone, never the sender's own opacity")
    void sharedGlintRuleAnnouncesGlintOnly() {
        ArmorHiderRenderApi.rule(EquipmentSlot.HEAD).shared().disableGlint().whenMatching(ctx -> true);

        var announced = AhRenderRuleRegistryImpl.evaluateShared(
                AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE);

        assertNotNull(announced);
        assertFalse(announced.affectsOpacity,
                "otherwise every receiver would substitute the sender's configured opacity for its own");
        assertTrue(announced.disableGlint);
    }

    @Test
    @DisplayName("a shared rule whose predicate stops matching announces nothing, so the state reverts")
    void nonMatchingSharedRuleAnnouncesNothing() {
        ArmorHiderRenderApi.rule(EquipmentSlot.HEAD).shared().hide().whenMatching(ctx -> false);

        assertNull(AhRenderRuleRegistryImpl.evaluateShared(
                AhRuleTarget.HEAD, ALICE, EquipmentSlot.HEAD, null, false, config, CONFIG_BASE));
    }

    @Test
    @DisplayName("a shared rule still applies locally - sharing is additive, never a replacement")
    void sharedRulesStillEvaluateLocally() {
        ArmorHiderRenderApi.rule(EquipmentSlot.HEAD).shared().hide().whenMatching(ctx -> true);

        assertEquals(0.0, evaluatedOpacity());
    }

    @Test
    @DisplayName("the broadcaster's fast path tracks registrations and survives an unregisterAll")
    void sharedPresenceTracksRegistrations() {
        assertFalse(AhRenderRuleRegistryImpl.hasSharedRules());

        var unshared = ArmorHiderRenderApi.setOpacityWhenMatching(EquipmentSlot.HEAD, 0.5f, ctx -> true);
        assertFalse(AhRenderRuleRegistryImpl.hasSharedRules(), "an unshared rule is not announced");

        Object owner = new Object();
        var shared = ArmorHiderRenderApi.rule(EquipmentSlot.FEET)
                .owner(owner).shared().hide().whenMatching(ctx -> true);
        assertTrue(AhRenderRuleRegistryImpl.hasSharedRules());

        ArmorHiderRenderApi.unregisterAll(owner);
        assertFalse(AhRenderRuleRegistryImpl.hasSharedRules(),
                "the last shared rule is gone, so the client tick must go quiet again");
        assertFalse(shared.isRegistered());
        assertTrue(unshared.isRegistered(), "a different owner's rule is untouched");
    }

    @Test
    @DisplayName("unregistering the only shared rule quiets the fast path")
    void unregisteringTheLastSharedRuleQuietsTheFastPath() {
        var first = ArmorHiderRenderApi.rule(EquipmentSlot.HEAD).shared().hide().whenMatching(ctx -> true);
        var second = ArmorHiderRenderApi.rule(EquipmentSlot.FEET).shared().hide().whenMatching(ctx -> true);

        ArmorHiderRenderApi.unregister(first);
        assertTrue(AhRenderRuleRegistryImpl.hasSharedRules(), "the second one is still shared");

        ArmorHiderRenderApi.unregister(second);
        assertFalse(AhRenderRuleRegistryImpl.hasSharedRules());
    }
}
