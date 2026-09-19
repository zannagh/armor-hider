package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.eunomia.client.gui.LayeredImageButton;
import de.zannagh.eunomia.ui.UiSizes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Master toggle for the accessory-hiding feature (issue #246), shown in the general behaviour-toggle
 * row alongside Combat Detection etc.
 */
public class AffectAccessoriesButton extends LayeredImageButton {

    public AffectAccessoriesButton(boolean initial, OnPress onPress) {
        super(initial, UiSizes.SQUARE_BUTTON_WIDTH, UiSizes.DEFAULT_BUTTON_HEIGHT,
                initial ? enabledMsg() : disabledMsg(), onPress);
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return sprite("armor-hider", "accessories_icon");
    }

    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? sprite("armor-hider", "accept_highlighted") : sprite("armor-hider", "reject_highlighted");
    }

    @Override
    protected Component enabledMessage() {
        return enabledMsg();
    }

    @Override
    protected Component disabledMessage() {
        return disabledMsg();
    }

    private static Component enabledMsg() {
        return Component.translatable("armorhider.options.affect_accessories.tooltip.enabled");
    }

    private static Component disabledMsg() {
        return Component.translatable("armorhider.options.affect_accessories.tooltip.disabled");
    }
}
