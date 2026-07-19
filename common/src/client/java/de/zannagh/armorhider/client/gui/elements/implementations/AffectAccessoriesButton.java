package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Master toggle for the accessory-hiding feature (issue #246), shown in the general behaviour-toggle
 * row alongside Combat Detection etc.
 */
public class AffectAccessoriesButton extends LayeredImageButton {

    public AffectAccessoriesButton(boolean initial, OnPress onPress) {
        super(null, initial, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT,
                initial ? enabledMsg() : disabledMsg(), onPress);
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return modSprite("accessories_icon");
    }

    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? modSprite("accept_highlighted") : modSprite("reject_highlighted");
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
