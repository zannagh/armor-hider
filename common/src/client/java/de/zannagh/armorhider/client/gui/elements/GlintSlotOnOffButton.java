package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class GlintSlotOnOffButton extends LayeredButton {
    @Nullable private final Identifier slotSprite;

    @Override
    protected @Nullable Identifier midLayerSprite() {
        return slotSprite;
    }

    //? if < 1.21 {
    /*@Override
    protected int statusBorderColor() {
        return isEnabled ? 0xCCFFFF00 : 0;
    }
    *///?}

    //? if >= 1.21 {
    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return enabled ? modSprite("air_bursting") : null;
    }
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
            slotSprite = modSprite("iron_helmet");
        }
        else if (slot == EquipmentSlot.CHEST) {
            slotSprite = modSprite("iron_chestplate");
        }
        else if (slot == EquipmentSlot.LEGS) {
            slotSprite = modSprite("iron_leggings");
        }
        else if (slot == EquipmentSlot.FEET) {
            slotSprite = modSprite("iron_boots");
        }
        else {
            slotSprite = null;
        }
        super.setEnabled(initial);
    }
}
