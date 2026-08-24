//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderRule;
import de.zannagh.armorhider.client.api.ArmorHiderRenderApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.SlotModification;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rule-lifecycle scenarios for {@link RenderApiSmokeTest}: what happens to a rule over time rather
 * than at a single instant. Split out of the test class to keep both files inside this repo's
 * 300-line ceiling.
 */
final class RenderApiSmokeLifecycle {

    private static final String OWNER = "armorhider-render-api-smoke-lifecycle";

    private RenderApiSmokeLifecycle() {}

    /**
     * The latch regression, driven through the cached {@code PlayerModificationInfo} via
     * {@link IdentityCarrier#getModification} - the call the render paths actually make.
     * <p>
     * The setup is the whole trick. Registering a rule does <em>not</em> dirty the cache: {@code
     * PlayerMixin} only sets {@code modsDirty} from the config-change listener and {@code
     * onEquipItem}. So a naive version of this assertion probes a cache that still holds whatever a
     * previous scenario baked, there is no rule-derived value for the flip to release, and it passes
     * whether or not the ratchet exists - measured, not assumed: an earlier version of this test
     * stayed green with the latch reintroduced.
     * <p>
     * Equipping is NOT enough to force the rebuild. {@code client.player.setItemSlot(...)} writes the
     * slot directly; it does not run {@code Player#onEquipItem}, which is driven by the equipment
     * change detection rather than by a direct client-side write. So the equip leaves {@code
     * modsDirty} false and the stale record stands - that is exactly how this assertion failed in CI,
     * reading a previous scenario's 0.7856 where it expected the rule's 0. The config-change listener
     * is the other invalidation path and is callable outright, so drive the rebuild with that.
     * <p>
     * No ticks between the rebuild and either read: {@code armorHider$getPlayerModifications()}
     * rebuilds on demand when dirty, so the bake happens inside the same client call that reads it,
     * and a server equipment re-sync cannot slip in and rebuild the cache for us. The explicit
     * baked-value precondition below fails loudly if that setup ever stops working, rather than
     * letting the assertion go quietly vacuous again.
     */
    static void assertPredicateFollowsChangingCondition(ClientGameTestContext context) {
        AtomicBoolean hiding = new AtomicBoolean(true);
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideArmorWhen(EquipmentSlot.CHEST, player -> hiding.get()));
        try {
            // Equip so the render-path lookup below has a stack to fold in, then dirty the cache
            // explicitly and read it back in the same client call - the getter rebuilds on demand, so
            // this is the rebuild that runs with the rule matching and bakes its opacity in. A null
            // player name notifies every listener regardless of which player it is keyed to.
            double baked = context.computeOnClient(client -> {
                if (client.player != null) {
                    client.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                }
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.notifyConfigListeners(null);
                return cachedChestRecord(client).transparency();
            });
            if (baked > 0.01) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the cached chest record reads " + baked + ", expected ~0 - the rebuild"
                                + " did not run while the rule was matching, so there is no baked value here"
                                + " and the un-latch assertion below would pass vacuously");
            }
            // Flip and re-read with no tick in between, so a server equipment re-sync cannot rebuild
            // the cache and clear the latch for us.
            double released = context.computeOnClient(client -> {
                hiding.set(false);
                return resolveCachedChest(client).transparency();
            });
            if (released < 0.99) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the chestplate is STILL hidden after the predicate started returning"
                                + " false (transparency " + released + "). The rule's match latched into the"
                                + " cached PlayerModificationInfo, so a time-varying condition hides armor"
                                + " permanently until a re-equip or config change");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
    }

    /** The raw cached record, pre-{@code addItemInformation} - used to prove the setup actually baked. */
    private static SlotModification cachedChestRecord(Minecraft client) {
        var player = client.player;
        if (player == null) {
            throw new IllegalStateException("[smoke/fcgt] Client player vanished mid-test");
        }
        return ((IdentityCarrier) player).armorHider$getPlayerModifications().chest();
    }

    /** The render path's own lookup: cached record folded together with the equipped stack. */
    private static SlotModification resolveCachedChest(Minecraft client) {
        var player = client.player;
        if (player == null) {
            throw new IllegalStateException("[smoke/fcgt] Client player vanished mid-test");
        }
        ItemStack equipped = player.getItemBySlot(EquipmentSlot.CHEST);
        if (equipped.isEmpty()) {
            throw new IllegalStateException("[smoke/fcgt] the chest slot emptied mid-test");
        }
        return ((IdentityCarrier) player).getModification(EquipmentSlot.CHEST, equipped);
    }

    /**
     * A throwing predicate must be treated as non-matching, not as a death sentence: the classic
     * cause is an unguarded {@code ctx.player()} on a frame where the entity is briefly unresolvable,
     * which recovers by itself. This one throws on its first few evaluations and then behaves, so it
     * must survive - only {@code MAX_CONSECUTIVE_FAILURES} throws IN A ROW may drop a rule, and any
     * success resets the streak.
     */
    static void assertThrowingPredicateRecovers(ClientGameTestContext context) {
        AtomicInteger calls = new AtomicInteger();
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideArmorWhen(EquipmentSlot.CHEST, player -> {
                    if (calls.incrementAndGet() <= 3) {
                        throw new IllegalStateException("[smoke/fcgt] deliberate render-rule failure");
                    }
                    return false;
                }));
        try {
            context.waitTicks(20);
            int evaluations = calls.get();
            boolean stillRegistered = context.computeOnClient(client -> rule.isRegistered());
            ArmorHider.LOGGER.info("[smoke/fcgt] throwing rule: {} evaluations, registered={}",
                    evaluations, stillRegistered);
            if (evaluations <= 3) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the throwing predicate was only evaluated " + evaluations + " time(s),"
                                + " so it never got past its failure streak and this proves nothing");
            }
            if (!stillRegistered) {
                throw new IllegalStateException(
                        "[smoke/fcgt] a rule that threw transiently and then recovered was unregistered."
                                + " A predicate tripping on a temporarily unresolvable player must not lose"
                                + " the consumer their rule for the session");
            }
            var chest = context.computeOnClient(client -> {
                ItemStack stack = new ItemStack(Items.DIAMOND_CHESTPLATE);
                return SlotModification.of(ArmorHiderClient.getCurrentPlayerName(), EquipmentSlot.CHEST, stack);
            });
            if (chest.needsModification()) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the recovered rule now returns false but the chestplate is still"
                                + " modified - a throw was folded in as a match instead of a non-match");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
    }

    /**
     * One builder, two rules. The effect setters must snapshot rather than mutate shared state: a
     * builder reused after {@code .hide()} previously leaked opacity 0 into the next rule built from
     * it, so a glint-only rule silently hid the piece. The hide rule here never matches, so if the
     * boots come out hidden it can only be leakage from the shared builder.
     */
    static void assertBuilderIsReusable(ClientGameTestContext context) {
        // Both handles are released through unregisterAll(OWNER) below, so neither is kept here.
        context.runOnClient(client -> {
            var builder = ArmorHiderRenderApi.rule(EquipmentSlot.FEET).owner(OWNER);
            builder.hide().when(player -> false);
            builder.disableGlint().when(player -> true);
        });
        try {
            var boots = context.computeOnClient(client -> {
                ItemStack stack = new ItemStack(Items.DIAMOND_BOOTS);
                if (client.player != null) {
                    client.player.setItemSlot(EquipmentSlot.FEET, stack);
                }
                return SlotModification.of(ArmorHiderClient.getCurrentPlayerName(), EquipmentSlot.FEET, stack);
            });
            if (boots.shouldHide() || boots.transparency() < 0.99) {
                throw new IllegalStateException(
                        "[smoke/fcgt] a glint-only rule built from a reused builder hid the boots"
                                + " (transparency " + boots.transparency() + "). The earlier .hide() on the same"
                                + " builder leaked its opacity into the second rule");
            }
            if (!boots.shouldDisableGlint()) {
                throw new IllegalStateException(
                        "[smoke/fcgt] the glint-only rule from the reused builder had no effect at all -"
                                + " the assertion above would pass vacuously");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregisterAll(OWNER));
        }
    }

    /**
     * The unregister-shaped latch, and a defect distinct from
     * {@link #assertRuleResultIsNotRatcheted}: there the predicate stops matching while the rule is
     * still registered, here the last rule for the slot is removed entirely. With nothing left to
     * evaluate, any "skip re-derivation when no rules exist" fast path hands back the record's baked
     * opacity and the piece stays hidden forever.
     * <p>
     * Driven the same deterministic way the ratchet assertion is - two {@code addItemInformation}
     * calls on one record - rather than through the cached {@code PlayerModificationInfo}. An earlier
     * cache-driven version of this assertion was measured to stay green with the latch present, so
     * the cache route cannot be trusted to observe it.
     */
    static void assertUnregisterRestoresBaseOpacity(ClientGameTestContext context) {
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideArmorWhen(EquipmentSlot.CHEST, player -> true));
        boolean unregistered = false;
        try {
            double restored = context.computeOnClient(client -> {
                ItemStack stack = new ItemStack(Items.DIAMOND_CHESTPLATE);
                String name = ArmorHiderClient.getCurrentPlayerName();
                SlotModification hidden = SlotModification.of(name, EquipmentSlot.CHEST, stack);
                if (!hidden.shouldHide()) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] the chestplate was not hidden while its rule was registered -"
                                    + " precondition failed for the unregister assertion");
                }
                ArmorHiderRenderApi.unregister(rule);
                return hidden.addItemInformation(stack).transparency();
            });
            unregistered = true;
            if (restored < 0.99) {
                throw new IllegalStateException(
                        "[smoke/fcgt] re-applying rules after the slot's only rule was unregistered left"
                                + " transparency at " + restored + ", expected ~1.0. With no rules left nothing"
                                + " re-derives the config base, so the removed rule's opacity is latched in -"
                                + " armor stays hidden after the consumer turns its feature off");
            }
        } finally {
            if (!unregistered) {
                context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
            }
        }
    }

    /**
     * The deterministic core of the latching defect, independent of when the cached
     * {@code PlayerModificationInfo} happens to be rebuilt. Re-applying rules to a record that a
     * rule has already modified is a one-way ratchet: a predicate that has stopped matching is
     * indistinguishable from "no rules at all", so {@code withRulesApplied} keeps the previous
     * rule's opacity instead of reverting to the config-derived base.
     */
    static void assertRuleResultIsNotRatcheted(ClientGameTestContext context) {
        AtomicBoolean hiding = new AtomicBoolean(true);
        AhRenderRule rule = context.computeOnClient(client ->
                ArmorHiderRenderApi.hideArmorWhenMatching(EquipmentSlot.LEGS, ctx -> hiding.get()));
        try {
            double reverted = context.computeOnClient(client -> {
                ItemStack stack = new ItemStack(Items.DIAMOND_LEGGINGS);
                String name = ArmorHiderClient.getCurrentPlayerName();
                SlotModification hidden = SlotModification.of(name, EquipmentSlot.LEGS, stack);
                if (!hidden.shouldHide()) {
                    throw new IllegalStateException(
                            "[smoke/fcgt] leggings were not hidden while the predicate matched -"
                                    + " precondition failed");
                }
                hiding.set(false);
                // Exactly what IdentityCarrier#getModification does to a cached record.
                return hidden.addItemInformation(stack).transparency();
            });
            if (reverted < 0.99) {
                throw new IllegalStateException(
                        "[smoke/fcgt] re-applying rules to an already-modified record left transparency at "
                                + reverted + " after the predicate stopped matching, expected ~1.0. Rule"
                                + " application is a one-way ratchet: nothing restores the config-derived"
                                + " base opacity, which is what lets a rule latch permanently");
            }
        } finally {
            context.runOnClient(client -> ArmorHiderRenderApi.unregister(rule));
        }
    }
    /** Within one priority band the most-hiding (lowest) opacity wins. */
    static void assertEqualPriorityTieIsMostHiding(ClientGameTestContext context) {
        AhRenderRule lighter = context.computeOnClient(client ->
                ArmorHiderRenderApi.setOpacityWhen(EquipmentSlot.FEET, 0.8F, player -> true));
        AhRenderRule darker = context.computeOnClient(client ->
                ArmorHiderRenderApi.setOpacityWhen(EquipmentSlot.FEET, 0.3F, player -> true));
        try {
            RenderApiSmokeSupport.expectOpacity(context, EquipmentSlot.FEET, 0.3,
                    "among equal-priority rules the most-hiding value must win");
        } finally {
            context.runOnClient(client -> {
                ArmorHiderRenderApi.unregister(lighter);
                ArmorHiderRenderApi.unregister(darker);
            });
        }
    }

}
//?}
