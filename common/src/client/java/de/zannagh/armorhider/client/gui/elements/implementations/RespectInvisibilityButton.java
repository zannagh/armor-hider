package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.eunomia.client.gui.LayeredImageButton;
import de.zannagh.eunomia.ui.UiSizes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class RespectInvisibilityButton extends LayeredImageButton {

    private final Identifier slotSprite = sprite("armor-hider", "invisibility_respect_enabled");
    private final Identifier disabledSlotSprite = sprite("armor-hider", "invisibility_respect_disabled");

    public RespectInvisibilityButton(boolean initial, OnPress onPress) {
        super(initial, UiSizes.SQUARE_BUTTON_WIDTH, UiSizes.DEFAULT_BUTTON_HEIGHT,
                initial ? RespectInvisibilityButton.enabledMsg() : RespectInvisibilityButton.disabledMsg(), onPress);
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return enabled ? slotSprite : disabledSlotSprite;
    }
    
    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? sprite("armor-hider", "accept_highlighted") : null;
    }

    @Override
    protected Component enabledMessage() {
        return enabledMsg();
    }

    @Override
    protected Component disabledMessage() {
        return disabledMsg();
    }
    
    private static Component enabledMsg(){
        return Component.translatable("armorhider.options.invisibilityRespect.tooltip.enabled");
    }
    
    private static Component disabledMsg(){
        return Component.translatable("armorhider.options.invisibilityRespect.tooltip.disabled");
    }
}
