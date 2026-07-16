package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class VanillaArmorInCombatButton extends LayeredImageButton {

    public VanillaArmorInCombatButton(boolean initial, OnPress onPress) {
        super(null, initial, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT,
                initial ? VanillaArmorInCombatButton.enabledMsg() : VanillaArmorInCombatButton.disabledMsg(), onPress);
    }


    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return enabled ? modSprite("in_combat_vanilla_icon_enabled") : modSprite("in_combat_vanilla_icon_disabled");
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
        return Component.translatable("armorhider.options.in_combat_default_model.tooltip.enabled");
    }
    
    private static Component disabledMsg(){
        return Component.translatable("armorhider.options.in_combat_default_model.tooltip.disabled");
    }
}
