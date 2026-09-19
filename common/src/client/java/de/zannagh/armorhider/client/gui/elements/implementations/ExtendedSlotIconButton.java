package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.eunomia.client.gui.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class ExtendedSlotIconButton extends LayeredImageButton {
    
    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) { return sprite("armor-hider", "other_items_icon"); }
    

    public ExtendedSlotIconButton(int width, int height, OnPress onPress) {
        super(true, width, height, buttonMessage, onPress);
    }

    private static final Component buttonMessage = Component.translatable("armorhider.options.item_exclusion.button_tooltip");

    @Override
    protected Component enabledMessage() {
        return null;
    }

    @Override
    protected Component disabledMessage() {
        return null;
    }
}
