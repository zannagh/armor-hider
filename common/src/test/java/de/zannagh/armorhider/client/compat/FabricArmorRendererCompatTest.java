package de.zannagh.armorhider.client.compat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The "inert without Fabric API" contract of the {@code ArmorRenderer} compat.
 *
 * <p>Two of its consumers sit on shared entry points - {@code HumanoidArmorLayer.renderArmorPiece} and
 * {@code armorCutoutNoCull} - and both are reached on every armor draw, Fabric API or not. If the compat
 * could report "drawing custom armor" without fabric-rendering-v1 actually present, vanilla armor would
 * pick up a render-type swap that belongs to the mod path. This test JVM has no Fabric API on the
 * classpath, so it exercises exactly that absent case.
 */
@DisplayName("Fabric ArmorRenderer compat gating")
class FabricArmorRendererCompatTest {

    @Test
    @DisplayName("the compat reports itself unavailable and finds no custom renderer without Fabric API")
    void inertWithoutFabricRenderingV1() {
        assertFalse(FabricArmorRendererCompat.isAvailable(),
                "the registry lookup must not resolve without fabric-rendering-v1");
        assertFalse(FabricArmorRendererCompat.hasCustomRenderer(null),
                "a null stack can never carry a custom renderer");
        assertFalse(FabricArmorRendererCompat.isDrawingCustomArmor(),
                "no draw window may be open before one is entered");
    }

    @Test
    @DisplayName("the draw window cannot be opened while the compat is unavailable")
    void drawWindowStaysShutWhenUnavailable() {
        FabricArmorRendererCompat.beginCustomArmorRender();
        try {
            assertFalse(FabricArmorRendererCompat.isDrawingCustomArmor(),
                    "without Fabric API the window must read shut even after an (impossible) begin, so the "
                            + "shared render-type hook stays off the vanilla armor path");
        } finally {
            FabricArmorRendererCompat.endCustomArmorRender();
        }
        assertFalse(FabricArmorRendererCompat.isDrawingCustomArmor());
    }
}
