package de.zannagh.armorhider.client.compat;

/**
 * Render-thread marker for "a Female Gender Mod breast-armor draw is in progress".
 * <p>
 * From FGM 5.0.0-Beta.5 (the 26.1.2+ pins) the breast armor is drawn through vanilla
 * {@code EquipmentLayerRenderer.renderLayers} with the raw worn chest stack, so
 * {@code EquipmentRenderMixin} cannot tell that draw apart from a body chestplate or a foreign elytra by
 * its arguments alone. {@code GenderArmorLayerV5Mixin} raises this flag around the draw so that
 * {@code EquipmentRenderMixin} (a) does not route an armored-elytra breast piece through the elytra
 * renderer and (b) can attribute a translucent render-type swap to the breast armor for the smoke-test
 * counters. Not gated on the gender constant: the readers compile on every variant and the flag is simply
 * never raised where the compat is absent.
 * <p>
 * Render thread only. Kept as a depth counter so a nested draw cannot clear an outer one early.
 */
public final class GenderBreastRenderGuard {

    private static int depth;

    private GenderBreastRenderGuard() {
    }

    public static void enter() {
        depth++;
    }

    public static void exit() {
        if (depth > 0) {
            depth--;
        }
    }

    public static boolean isActive() {
        return depth > 0;
    }
}
