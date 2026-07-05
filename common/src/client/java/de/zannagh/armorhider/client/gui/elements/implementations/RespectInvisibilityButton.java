package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class RespectInvisibilityButton extends LayeredImageButton {

    private final Identifier slotSprite = modSprite("invisibility_respect_enabled");
    private final Identifier disabledSlotSprite = modSprite("invisibility_respect_disabled");

    public RespectInvisibilityButton(boolean initial, OnPress onPress) {
        super(null, initial, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT,
                initial ? RespectInvisibilityButton.enabledMsg() : RespectInvisibilityButton.disabledMsg(), onPress);
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return enabled ? slotSprite : disabledSlotSprite;
    }
    
    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? modSprite("accept_highlighted") : null;
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
