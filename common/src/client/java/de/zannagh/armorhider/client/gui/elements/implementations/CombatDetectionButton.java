package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class CombatDetectionButton extends LayeredImageButton {

    private final ResourceLocation slotSprite = modSprite("in_combat_button_icon_enabled");
    private final ResourceLocation disabledSlotSprite = modSprite("in_combat_button_icon_disabled");

    public CombatDetectionButton(boolean initial, OnPress onPress) {
        super(null, initial, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT,
                initial ? CombatDetectionButton.enabledMsg() : CombatDetectionButton.disabledMsg(), onPress);
    }

    @Override
    protected @Nullable ResourceLocation spriteForeground(boolean enabled) {
        return enabled ? slotSprite : disabledSlotSprite;
    }
    
    @Override
    protected @Nullable ResourceLocation midLayerSprite(boolean enabled) {
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
        return Component.translatable("armorhider.options.combat_detection.tooltip.enabled");
    }
    
    private static Component disabledMsg(){
        return Component.translatable("armorhider.options.combat_detection.tooltip.disabled");
    }
}
