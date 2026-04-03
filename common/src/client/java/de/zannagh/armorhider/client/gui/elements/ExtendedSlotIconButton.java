package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ExtendedSlotIconButton extends LayeredButton {
    //? if >= 1.21 {
    @Override
    protected Function<Boolean, @Nullable Identifier> spriteForeground() { return (bln) -> Identifier.withDefaultNamespace("statistics/item_dropped"); }
    //?}

    @Override
    protected @Nullable Component statusOverlay() {
        return Component.literal("…");
    }

    public ExtendedSlotIconButton(EquipmentSlot slot, int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(slot, x, y, width, height, message, onPress, createNarration);
    }
}
