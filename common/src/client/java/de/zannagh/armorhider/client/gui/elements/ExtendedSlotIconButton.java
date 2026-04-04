package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class ExtendedSlotIconButton extends LayeredButton {
    //? if >= 1.21 {
    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) { return modSprite("item_dropped"); }
    //?}

    //? if < 1.21 {
    /*@Override
    protected @Nullable Component statusOverlay() {
        return Component.literal("\u2026");
    }
    *///?}

    public ExtendedSlotIconButton(EquipmentSlot slot, int width, int height, OnPress onPress) {
        super(slot, width, height, buttonMessage, onPress);
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
