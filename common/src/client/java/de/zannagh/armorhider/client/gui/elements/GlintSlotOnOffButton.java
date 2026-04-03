package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class GlintSlotOnOffButton extends LayeredButton {
    @Nullable private final Item slotItem;
    @Nullable private ItemStack cachedSlotStack;

    @Override
    protected @Nullable ItemStack midLayer() {
        if (slotItem == null) return null;
        if (cachedSlotStack == null) {
            try {
                cachedSlotStack = new ItemStack(slotItem);
            } catch (Exception ignored) {}
        }
        return cachedSlotStack;
    }

    @Override
    protected int statusBorderColor() {
        return isEnabled ? 0xCCFFFF00 : 0; // semi-transparent yellow border when glint on
    }

    //? if >= 1.21 {
    @Override
    protected Function<Boolean, @Nullable Identifier> spriteForeground() {
        return this::spriteForeground;
    }

    protected Identifier spriteForeground(Boolean input) { return input ? Identifier.withDefaultNamespace("hud/air_bursting") : Identifier.withDefaultNamespace(""); }
    //?}

    @Override
    protected Component enabledMessage() {
        return enabledMsg(slot);
    }

    @Override
    protected Component disabledMessage() {
        return disabledMsg(slot);
    }

    private static Component enabledMsg(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            return Component.translatable("armorhider.options.helmet_glint.tooltip.enabled");
        }
        else if (slot == EquipmentSlot.CHEST) {
            return Component.translatable("armorhider.options.chest_glint.tooltip.enabled");
        }
        else if (slot == EquipmentSlot.LEGS) {
            return Component.translatable("armorhider.options.legs_glint.tooltip.enabled");
        }
        else if (slot == EquipmentSlot.FEET) {
            return Component.translatable("armorhider.options.boots_glint.tooltip.enabled");
        }
        return Component.empty();
    }

    private static Component disabledMsg(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            return Component.translatable("armorhider.options.helmet_glint.tooltip.disabled");
        }
        else if (slot == EquipmentSlot.CHEST) {
            return Component.translatable("armorhider.options.chest_glint.tooltip.disabled");
        }
        else if (slot == EquipmentSlot.LEGS) {
            return Component.translatable("armorhider.options.legs_glint.tooltip.disabled");
        }
        else if (slot == EquipmentSlot.FEET) {
            return Component.translatable("armorhider.options.boots_glint.tooltip.disabled");
        }
        else {
            return Component.empty();
        }
    }

    public GlintSlotOnOffButton(boolean initial, EquipmentSlot slot, int width, int height, OnPress onPress) {
        super(slot, width, height, initial ? GlintSlotOnOffButton.enabledMsg(slot) : GlintSlotOnOffButton.disabledMsg(slot), onPress);
        if (slot == EquipmentSlot.HEAD) {
            slotItem = Items.IRON_HELMET;
        }
        else if (slot == EquipmentSlot.CHEST) {
            slotItem = Items.IRON_CHESTPLATE;
        }
        else if (slot == EquipmentSlot.LEGS) {
            slotItem = Items.IRON_LEGGINGS;
        }
        else if (slot == EquipmentSlot.FEET) {
            slotItem = Items.IRON_BOOTS;
        }
        else {
            slotItem = null;
        }
        super.setEnabled(initial);
    }
}
