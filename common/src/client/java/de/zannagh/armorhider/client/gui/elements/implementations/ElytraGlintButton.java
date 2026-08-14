package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Toggles the enchantment glint on the elytra. Mirrors {@link GlintSlotOnOffButton} but is not tied to
 * a vanilla {@link net.minecraft.world.entity.EquipmentSlot}: it shows the elytra sprite with the glint
 * icon overlaid while glint is enabled.
 */
public class ElytraGlintButton extends LayeredImageButton {

    private final Identifier elytraSprite = modSprite("elytra");

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return elytraSprite;
    }

    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? modSprite("glint_button_icon") : null;
    }

    @Override
    protected Component enabledMessage() {
        return Component.translatable("armorhider.options.elytra_glint.tooltip.enabled");
    }

    @Override
    protected Component disabledMessage() {
        return Component.translatable("armorhider.options.elytra_glint.tooltip.disabled");
    }

    public ElytraGlintButton(boolean initial, int width, int height, OnPress onPress) {
        super(null, initial, width, height,
                initial ? Component.translatable("armorhider.options.elytra_glint.tooltip.enabled")
                        : Component.translatable("armorhider.options.elytra_glint.tooltip.disabled"),
                onPress);
        super.setEnabled(initial);
    }
}
