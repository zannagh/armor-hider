//? if fcgt {
package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.ArmorHiderRenderApi;
import de.zannagh.armorhider.client.api.impl.AhRenderRuleRegistryImpl;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Scene setup, state resolution and assertion helpers for {@link RenderApiSmokeTest}. Split out of
 * the test class only to keep both files inside this repo's 300-line ceiling.
 */
final class RenderApiSmokeSupport {

    private RenderApiSmokeSupport() {}



    /**
     * Renders the equipped helmet for a few ticks and asserts the translucent-armor path was taken.
     * The record-level checks above prove the rule reached the decision; this proves the decision
     * reached a draw call.
     */
    static void assertFadedPieceIsSubmittedTranslucent(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var player = client.player;
            if (player != null) {
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            }
            // Third person, or the local player's own helmet is never drawn.
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        long before = context.computeOnClient(client -> ArmorHiderRenderTypes.armorNoDepthPathCount());
        // Re-assert the helmet between waits: setItemSlot on the client player is a local write the
        // server's inventory sync can undo, and an empty head slot would make the counter flat for
        // the wrong reason.
        for (int i = 0; i < 4; i++) {
            context.waitTicks(5);
            context.runOnClient(client -> {
                if (client.player != null) {
                    client.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                }
            });
        }
        long after = context.computeOnClient(client -> ArmorHiderRenderTypes.armorNoDepthPathCount());
        ArmorHider.LOGGER.info("[smoke/fcgt] translucent armor submissions while the API rule was active: {}",
                after - before);
        if (after <= before) {
            throw new IllegalStateException(
                    "[smoke/fcgt] a helmet faded to 50% by ArmorHiderRenderApi was never submitted on the"
                            + " translucent armor type (delta " + (after - before) + ") - the rule changed the"
                            + " resolved opacity but nothing was actually drawn differently");
        }
    }

    /** Equips {@code item} in {@code slot} and resolves the modification the render paths would see. */
    static SlotModification resolve(ClientGameTestContext context, EquipmentSlot slot, Item item) {
        return context.computeOnClient(client -> {
            ItemStack stack = new ItemStack(item);
            if (client.player != null) {
                client.player.setItemSlot(slot, stack);
            }
            return SlotModification.of(ArmorHiderClient.getCurrentPlayerName(), slot, stack);
        });
    }

    static void expectVanilla(SlotModification modification, String what, String why) {
        if (modification.needsModification()) {
            throw new IllegalStateException(
                    "[smoke/fcgt] " + what + " is not rendering vanilla (transparency "
                            + modification.transparency() + ", hidden=" + modification.shouldHide()
                            + ", glintOff=" + modification.shouldDisableGlint() + ") but should be: " + why);
        }
    }

    /**
     * Puts the local config into a known all-opaque state so any deviation observed below is the
     * API's doing. A prior smoke test (keybind) can leave the session disable override on, which
     * would short-circuit every rule in shouldUseVanilla and pass the "vanilla" assertions for the
     * wrong reason.
     */
    static void resetToVanillaConfig(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.player == null) {
                throw new IllegalStateException("[smoke/fcgt] Client player did not spawn");
            }
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
            var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER
                    .resolveConfig(ArmorHiderClient.getCurrentPlayerName());
            config.disableArmorHider.setValue(false);
            // Combat detection would raise opacity back to ~1.0 on any incidental damage and make the
            // fade assertions flaky; the feature has its own smoke test.
            config.enableCombatDetection.setValue(false);
            config.helmetOpacity.setValue(1.0);
            config.chestOpacity.setValue(1.0);
            config.legsOpacity.setValue(1.0);
            config.bootsOpacity.setValue(1.0);
            config.offHandOpacity.setValue(1.0);
            config.elytraOpacity.setValue(1.0);
            config.helmetGlint.setValue(true);
            config.chestGlint.setValue(true);
            config.legsGlint.setValue(true);
            config.bootsGlint.setValue(true);
            config.elytraGlint.setValue(true);
            AhRenderRuleRegistryImpl.clear();
        });
    }
    /** Asserts the slot resolves to {@code expected} opacity, probing with a diamond boot stack. */
    static void expectOpacity(ClientGameTestContext context, EquipmentSlot slot, double expected, String why) {
        var resolved = resolve(context, slot, Items.DIAMOND_BOOTS);
        if (Math.abs(resolved.transparency() - expected) > 0.01) {
            throw new IllegalStateException("[smoke/fcgt] " + slot + " resolved to transparency "
                    + resolved.transparency() + ", expected " + expected + " - " + why);
        }
    }

    /**
     * Slots Armor Hider does not render must be rejected loudly at registration. MAINHAND in
     * particular used to hand back a working-looking handle that could never fire - there is no
     * main-hand interceptor and no main-hand opacity in the config - which is worse than an error,
     * because the consumer has no way to notice. This also fails if anyone re-adds the
     * {@code AhRuleTarget} constant and quietly restores the silent no-op.
     */
    static void expectUnrenderedSlotsRejected(ClientGameTestContext context) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.BODY}) {
            String outcome = context.computeOnClient(client -> {
                try {
                    ArmorHiderRenderApi.rule(slot).hide().when(player -> true);
                    return "returned a handle";
                } catch (IllegalArgumentException expected) {
                    return null;
                } catch (Throwable other) {
                    return "threw " + other.getClass().getSimpleName();
                }
            });
            if (outcome != null) {
                throw new IllegalStateException(
                        "[smoke/fcgt] rule(" + slot + ") " + outcome + " instead of throwing"
                                + " IllegalArgumentException. Armor Hider does not render that slot, so the"
                                + " rule would be a silent no-op the consumer cannot detect");
            }
        }
    }

}
//?}
