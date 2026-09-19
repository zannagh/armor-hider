package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.eunomia.client.gui.LayeredImageButton;
import de.zannagh.eunomia.ui.UiSizes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class CombatDetectionButton extends LayeredImageButton {

    private final Identifier slotSprite = sprite("armor-hider", "in_combat_button_icon_enabled");
    private final Identifier disabledSlotSprite = sprite("armor-hider", "in_combat_button_icon_disabled");

    public CombatDetectionButton(boolean initial, OnPress onPress) {
        super(initial, UiSizes.SQUARE_BUTTON_WIDTH, UiSizes.DEFAULT_BUTTON_HEIGHT,
                initial ? CombatDetectionButton.enabledMsg() : CombatDetectionButton.disabledMsg(), onPress);
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
        return Component.translatable("armorhider.options.combat_detection.tooltip.enabled");
    }
    
    private static Component disabledMsg(){
        return Component.translatable("armorhider.options.combat_detection.tooltip.disabled");
    }
}
