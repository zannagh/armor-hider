//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderRule;
import de.zannagh.armorhider.client.api.ArmorHiderRenderApi;
import de.zannagh.armorhider.client.api.impl.AhRenderRuleRegistryImpl;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.net.packets.SharedRuleNotificationPacket;
import de.zannagh.armorhider.net.packets.SharedRuleTarget;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.concurrent.atomic.AtomicInteger;

import static de.zannagh.armorhider.smoke.RenderApiSmokeSupport.assertFadedPieceIsSubmittedTranslucent;
import static de.zannagh.armorhider.smoke.RenderApiSmokeSupport.expectOpacity;
import static de.zannagh.armorhider.smoke.RenderApiSmokeSupport.expectUnrenderedSlotsRejected;
import static de.zannagh.armorhider.smoke.RenderApiSmokeSupport.expectVanilla;
import static de.zannagh.armorhider.smoke.RenderApiSmokeSupport.resetToVanillaConfig;
import static de.zannagh.armorhider.smoke.RenderApiSmokeSupport.resolve;

/**
 * Public render-API smoke (fabric-client-gametest-api-v1).
 * <p>
 * {@link ArmorHiderRenderApi} lets a third-party mod hide, fade or de-glint equipment from a
 * predicate without writing a mixin. This test locks down that a rule registered through the public
 * surface reaches the decision every render path resolves through ({@link SlotModification}) and,
 * for the fade case, the render pipeline itself - a rule that lands in the registry but never
 * influences a draw would be indistinguishable from a no-op.
 * <p>
 * Assertions, in order:
 * <ol>
 *   <li>no rules registered - a fully-opaque config resolves to vanilla. The baseline every later
 *       "restores vanilla" claim is measured against.</li>
 *   <li>{@code hideArmorWhen(CHEST, always)} hides the chestplate, {@code unregister} restores it.
 *       Do not "simplify" to the {@code *Matching} form: only the plain {@code Predicate<Player>}
 *       overloads resolve the entity through {@code AhPlayerLookupCache}, and a defect that made
 *       every one of them never match was invisible to {@code *Matching} and to every unit test.</li>
 *   <li>{@code setOpacityWhen(HEAD, 0.5f, always)} fades - not hides - the helmet, and the piece is
 *       really submitted on the translucent armor render type while it renders.</li>
 *   <li>a predicate returning {@code false} is evaluated and still leaves vanilla rendering
 *       untouched - no consumer, no behaviour change.</li>
 *   <li>{@code hideElytraWhen} hides the wings while a chestplate in the same CHEST slot stays
 *       visible - the reason the elytra has its own registration method.</li>
 *   <li>{@code hideOffhandWhen} hides the off-hand item.</li>
 *   <li>slots Armor Hider does not render (MAINHAND, BODY) are rejected at registration.</li>
 *   <li>lifecycle behaviour, in {@link RenderApiSmokeLifecycle}: rule application is not a one-way
 *       ratchet either when a predicate stops matching or when the last rule is unregistered, a
 *       transiently throwing predicate is a non-match rather than an unregistration, and one
 *       builder can safely produce two rules.</li>
 *   <li>two rules on one slot resolve per the documented precedence: the strongest (lowest
 *       numeric) priority decides outright, ties broken by the most-hiding value.</li>
 *   <li>a {@code shared()} rule actually leaves the client: the client tick evaluates it against the
 *       local player, and the server decodes it, attributes it to the authenticated sender and stores
 *       it - and unregistering it retracts the announced state again, which no off-game test can
 *       prove because the failure mode there is a decision not to send.</li>
 * </ol>
 * Every rule is unregistered in a {@code finally} block: {@code runClientGametest} runs every
 * entrypoint in ONE client launch, so a leaked rule would silently hide armor in a sibling test.
 * <p>
 * Every assertion here has been mutation-tested: the latch was reintroduced in
 * {@code withRulesApplied} and each was confirmed to red. That matters because two of them were
 * measured to be vacuous when first written - see {@link RenderApiSmokeLifecycle}.
 */
public final class RenderApiSmokeTest implements FabricClientGameTest {

    private static final String OWNER = "armorhider-render-api-smoke";

    @Override
    public void runTest(ClientGameTestContext context) {
        ArmorHider.LOGGER.info("[smoke/fcgt] Render API smoke starting");
        context.waitForScreen(TitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .adjustSettings(state -> {
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setGenerateStructures(false);
                })
                .create()) {
            resetToVanillaConfig(context);
            context.waitTicks(20);

            int leftBehind;
            try {
                assertVanillaBaseline(context);
                assertHideAndUnregister(context);
                assertPartialOpacity(context);
                assertFalsePredicateIsInert(context);
                assertElytraIsIndependentOfChestplate(context);
                assertOffhandHidden(context);
                assertPriorityPrecedence(context);
                assertSharedRuleReachesTheServer(context);
                expectUnrenderedSlotsRejected(context);
                RenderApiSmokeLifecycle.assertThrowingPredicateRecovers(context);
                RenderApiSmokeLifecycle.assertRuleResultIsNotRatcheted(context);
                RenderApiSmokeLifecycle.assertPredicateFollowsChangingCondition(context);
                RenderApiSmokeLifecycle.assertUnregisterRestoresBaseOpacity(context);
                RenderApiSmokeLifecycle.assertBuilderIsReusable(context);
            } finally {
                // Belt and braces: unregisterAll only catches owner-tagged rules, and a scenario that
                // threw mid-way never reached its own unregister. A leaked rule would contaminate
                // every sibling gametest in this client launch, so the registry is emptied
                // unconditionally - the count is taken first so the check below is not vacuous.
                leftBehind = context.computeOnClient(client -> {
                    ArmorHiderRenderApi.unregisterAll(OWNER);
                    int remaining = AhRenderRuleRegistryImpl.registeredRules().size();
                    AhRenderRuleRegistryImpl.clear();
                    return remaining;
                });
            }

            if (leftBehind != 0) {
                throw new IllegalStateException("[smoke/fcgt] " + leftBehind + " render rule(s) were still"
                        + " registered after the scenarios finished - every scenario must unregister what it"
                        + " registers or it contaminates the sibling gametests in this client launch");
            }
            ArmorHider.LOGGER.info("[smoke/fcgt] Render API smoke complete");
        }
    }

    // ── scenarios ────────────────────────────────────────────────────────────────────────────────

    /** With no rules registered and an all-opaque config, every slot must resolve to vanilla. */
    private static void assertVanillaBaseline(ClientGameTestContext context) {
        expectVanilla(
                resolve(context, EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE),
                "baseline chestplate", "no rule is registered yet");
        expectVanilla(
                resolve(context, EquipmentSlot.HEAD, Items.DIAMOND_HELMET),
                "baseline helmet", "no rule is registered yet");
        expectVanilla(
                resolve(context, EquipmentSlot.CHEST, Items.ELYTRA),
                "baseline elytra", "no rule is registered yet");
    }

    /** hideArmorWhen hides the piece; unregister gives vanilla back. */
    private static void assertHideAndUnregister(ClientGameTestContext context) {

        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideArmorWhen(EquipmentSlot.CHEST, player -> true));
        try {
            var hidden = resolve(context, EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE);
            if (!hidden.shouldHide()) {
                throw new IllegalStateException(
                        "[smoke/fcgt] hideArmorWhen(CHEST, always) did not hide the chestplate"
                                + " (transparency " + hidden.transparency() + ") - the rule never reached"
                                + " SlotModification, so it changes nothing that renders");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
        expectVanilla(
                resolve(context, EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE),
                "chestplate after unregister", "the only rule was unregistered");
    }

    /**
     * setOpacityWhen must fade rather than hide, and the fade must reach the pipeline: the
     * translucent (no-depth) armor type is what a faded piece is submitted on, so its counter
     * climbing while the helmet renders is the end-to-end evidence that the rule drew something.
     */
    private static void assertPartialOpacity(ClientGameTestContext context) {
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.setOpacityWhen(EquipmentSlot.HEAD, 0.5F, player -> true));
        try {
            var faded = resolve(context, EquipmentSlot.HEAD, Items.DIAMOND_HELMET);
            if (faded.shouldHide() || Math.abs(faded.transparency() - 0.5) > 0.01) {
                throw new IllegalStateException(
                        "[smoke/fcgt] setOpacityWhen(HEAD, 0.5) resolved to transparency "
                                + faded.transparency() + " (hidden=" + faded.shouldHide() + "), expected ~0.5"
                                + " and not hidden");
            }
            if (!faded.needsTranslucency()) {
                throw new IllegalStateException(
                        "[smoke/fcgt] a 50% helmet does not report needsTranslucency, so it would never be"
                                + " routed onto the translucent armor type and would render opaque");
            }
            assertFadedPieceIsSubmittedTranslucent(context);
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
        expectVanilla(
                resolve(context, EquipmentSlot.HEAD, Items.DIAMOND_HELMET),
                "helmet after unregister", "the opacity rule was unregistered");
    }

    /** A rule whose predicate says no must be evaluated and still leave rendering vanilla. */
    private static void assertFalsePredicateIsInert(ClientGameTestContext context) {
        AtomicInteger evaluations = new AtomicInteger();
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideArmorWhen(EquipmentSlot.LEGS, player -> {
                    evaluations.incrementAndGet();
                    return false;
                }));
        try {
            expectVanilla(
                resolve(context, EquipmentSlot.LEGS, Items.DIAMOND_LEGGINGS),
                "leggings under a false predicate",
                    "a rule that does not match must not change anything");
            if (evaluations.get() == 0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the LEGS predicate was never evaluated - the assertion above passed"
                                + " vacuously and would keep passing with the whole rule pipeline unwired");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
    }

    /**
     * The elytra shares the CHEST slot but is targeted separately, so hideElytraWhen must hide the
     * wings and leave a chestplate in that same slot alone.
     */
    private static void assertElytraIsIndependentOfChestplate(ClientGameTestContext context) {
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideElytraWhen(player -> true));
        try {
            var wings = resolve(context, EquipmentSlot.CHEST, Items.ELYTRA);
            if (!wings.shouldHide()) {
                throw new IllegalStateException(
                        "[smoke/fcgt] hideElytraWhen(always) did not hide the elytra (transparency "
                                + wings.transparency() + ")");
            }
            expectVanilla(
                resolve(context, EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE),
                "chestplate under an elytra-only rule",
                    "an ELYTRA-targeted rule must not bleed onto CHEST armor - that separation is the"
                            + " entire reason hideElytraWhen exists");
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
    }

    /**
     * A {@code shared()} rule has to leave the client. Everything about sharing that a unit test can
     * reach - precedence, the store, the dedup - is covered off-game; what only a running game proves
     * is that the client tick evaluates the rule against the local player, encodes it, and that the
     * server decodes it, attributes it to the authenticated sender and stores it.
     * <p>
     * Singleplayer is enough for that: the integrated server runs the same {@code CommsManager}
     * handler a dedicated one does, over the same loopback connection. The relay to the other clients
     * is the one hop this cannot see - it broadcasts to everyone but the sender, and here the sender
     * is everyone.
     * <p>
     * The retraction half is the reason this is not just an "arrives" assertion. Unregistering the
     * last shared rule leaves the state standing on every <em>other</em> client, so a broadcaster that
     * goes quiet the moment it has nothing to share leaves the player hidden for the rest of the
     * session - and every off-game test still passes, because the bug is in the decision not to send.
     */
    private static void assertSharedRuleReachesTheServer(ClientGameTestContext context) {
        AhRenderRule rule = context.computeOnClient(client -> ArmorHiderRenderApi.rule(EquipmentSlot.HEAD)
                .owner(OWNER)
                .shared()
                .hide()
                .whenMatching(ctx -> true));
        try {
            var announced = awaitSharedRuleState(context, true);
            if (announced == null) {
                throw new IllegalStateException(
                        "[smoke/fcgt] a shared() rule never reached the server's shared-rule store -"
                                + " the other players would keep seeing the piece the rule hides");
            }
            var forHead = announced.overrides.stream()
                    .filter(o -> o.target == SharedRuleTarget.HEAD)
                    .findFirst()
                    .orElse(null);
            if (forHead == null || !forHead.affectsOpacity || forHead.opacity != 0.0) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the shared HEAD hide arrived as " + announced.overrides
                                + ", expected an opacity-0 entry for HEAD");
            }
            if (announced.playerName == null || announced.playerName.isBlank()) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the server stored the shared state under a blank name, so no client"
                                + " could ever look it up while rendering that player");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }

        if (awaitSharedRuleState(context, false) != null) {
            throw new IllegalStateException(
                    "[smoke/fcgt] unregistering the last shared rule did not retract the announced state -"
                            + " every other client would keep the piece hidden for the rest of the session");
        }
    }

    /**
     * Polls the integrated server's shared-rule store until it reports {@code expectPresent}, or gives
     * up. Polling rather than a fixed wait because the packet crosses a thread boundary: the client
     * tick sends, the server thread handles. The store is concurrent, so reading it from here is safe.
     */
    private static SharedRuleNotificationPacket awaitSharedRuleState(ClientGameTestContext context,
                                                                     boolean expectPresent) {
        SharedRuleNotificationPacket seen = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            context.waitTicks(5);
            var runtime = ArmorHider.getRuntime();
            if (runtime == null) {
                continue;
            }
            var snapshot = runtime.getSharedRules().snapshotExcept(null);
            seen = snapshot.isEmpty() ? null : snapshot.get(0);
            if ((seen != null) == expectPresent) {
                return seen;
            }
        }
        return seen;
    }

    /** hideOffhandWhen targets the off-hand slot. */
    private static void assertOffhandHidden(ClientGameTestContext context) {
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideOffhandWhen(player -> true));
        try {
            var offhand = resolve(context, EquipmentSlot.OFFHAND, Items.SHIELD);
            if (!offhand.shouldHide()) {
                throw new IllegalStateException(
                        "[smoke/fcgt] hideOffhandWhen(always) did not hide the off-hand item (transparency "
                                + offhand.transparency() + ")");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
        expectVanilla(
                resolve(context, EquipmentSlot.OFFHAND, Items.SHIELD),
                "off-hand after unregister", "the off-hand rule was unregistered");
    }

    /**
     * Two opacity rules on one slot at different priorities. The stronger (lower numeric) priority
     * decides outright, so the deliberately <em>less</em>-hiding 75% rule at priority 100 must beat
     * the 25% rule at the default priority. Written this way on purpose: with a naive "lowest
     * opacity always wins" implementation this resolves to 0.25 and the test reds, which is exactly
     * the regression worth catching.
     */
    private static void assertPriorityPrecedence(ClientGameTestContext context) {
        AhRenderRule weakPriority = context.computeOnClient(client -> ArmorHiderRenderApi
                .rule(EquipmentSlot.FEET)
                .owner(OWNER)
                .priority(ArmorHiderRenderApi.defaultPriority())
                .opacity(0.25F)
                .when(player -> true));
        AhRenderRule strongPriority = context.computeOnClient(client -> ArmorHiderRenderApi
                .rule(EquipmentSlot.FEET)
                .owner(OWNER)
                .priority(100)
                .opacity(0.75F)
                .when(player -> true));
        try {
            expectOpacity(context, EquipmentSlot.FEET, 0.75,
                    "the priority-100 rule must decide outright over the default-priority one");
        } finally {
            context.runOnClient(client -> {
                ArmorHiderRenderApi.unregister(weakPriority);
                ArmorHiderRenderApi.unregister(strongPriority);
            });
        }
        RenderApiSmokeLifecycle.assertEqualPriorityTieIsMostHiding(context);
        expectVanilla(
                resolve(context, EquipmentSlot.FEET, Items.DIAMOND_BOOTS),
                "boots after unregister", "every priority rule was unregistered");
    }

}
//?}
