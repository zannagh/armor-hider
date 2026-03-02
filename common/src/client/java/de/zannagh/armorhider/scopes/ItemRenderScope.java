package de.zannagh.armorhider.scopes;

import de.zannagh.armorhider.resources.ArmorModificationInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the scope of rendering a specific equipment item in a specific slot.
 * Active during renderArmorPiece / renderLayers / render for a specific layer renderer.
 *
 * Holds the slot, item, and pre-resolved modification info.
 * Immutable value object — created on entry, read-only during scope, discarded on exit.
 */
public final class ItemRenderScope {

    private final @NotNull EquipmentSlot slot;
    private final @NotNull ItemStack itemStack;
    private final @NotNull ArmorModificationInfo modification;

    public ItemRenderScope(@NotNull EquipmentSlot slot,
                           @NotNull ItemStack itemStack,
                           @NotNull ArmorModificationInfo modification) {
        this.slot = slot;
        this.itemStack = itemStack;
        this.modification = modification;
    }

    public @NotNull EquipmentSlot slot() { return slot; }
    public @NotNull ItemStack itemStack() { return itemStack; }
    public @NotNull ArmorModificationInfo modification() { return modification; }

    public boolean shouldHide() {
        return modification.shouldHide();
    }

    public boolean shouldModify() {
        return modification.shouldModify();
    }

    public double transparency() {
        return modification.getTransparency();
    }
}
