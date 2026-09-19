package de.zannagh.armorhider.client.gui;

import net.minecraft.resources.Identifier;

/**
 * armor-hider's own GUI sprite identifiers.
 *
 * <p>These used to be private constants inside armor-hider's copy of {@code PlayerHeadBarWidget}. eunomia's
 * copy takes its scroll arrows as constructor arguments instead of hard-coding them, so the identifiers have
 * to live somewhere a caller can reach - and the textures themselves stay armor-hider assets.
 */
public final class AhGuiSprites {
    /** Scroll-affordance arrows for the player head bar. Both textures are 23x13, drawn at native size. */
    public static final Identifier ARROW_BACK = sprite("textures/gui/sprites/arrow_back.png");

    public static final Identifier ARROW_FORWARD = sprite("textures/gui/sprites/arrow_forward.png");

    private AhGuiSprites() {
    }

    private static Identifier sprite(String path) {
        //? if >= 1.21 {
        return Identifier.fromNamespaceAndPath("armor-hider", path);
        //?}
        //? if < 1.21 {
        /*return new Identifier("armor-hider", path);
        *///?}
    }
}
