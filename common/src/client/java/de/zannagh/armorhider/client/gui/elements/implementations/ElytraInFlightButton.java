package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Toggles whether the elytra is rendered while the player is gliding. When enabled the wings show in
 * flight (Armor Hider leaves them alone); when disabled they follow the configured opacity in flight
 * too. Shows the elytra sprite with an accept/reject overlay, matching {@link AffectOtherItemsButton}.
 */
public class ElytraInFlightButton extends LayeredImageButton {

    private final Identifier elytraSprite = modSprite("elytra");

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return elytraSprite;
    }

    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? modSprite("accept_highlighted") : modSprite("reject_highlighted");
    }

    @Override
    protected Component enabledMessage() {
        return Component.translatable("armorhider.options.elytra_in_flight.tooltip.enabled");
    }

    @Override
    protected Component disabledMessage() {
        return Component.translatable("armorhider.options.elytra_in_flight.tooltip.disabled");
    }

    public ElytraInFlightButton(boolean initial, int width, int height, OnPress onPress) {
        super(null, initial, width, height,
                initial ? Component.translatable("armorhider.options.elytra_in_flight.tooltip.enabled")
                        : Component.translatable("armorhider.options.elytra_in_flight.tooltip.disabled"),
                onPress);
        super.setEnabled(initial);
    }
}
