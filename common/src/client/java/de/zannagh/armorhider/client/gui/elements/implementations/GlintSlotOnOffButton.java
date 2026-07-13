package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class GlintSlotOnOffButton extends LayeredImageButton {
    @Nullable private final ResourceLocation slotSprite;

    @Override
    protected @Nullable ResourceLocation spriteForeground(boolean enabled) {
        return slotSprite;
    }

    @Override
    protected @Nullable ResourceLocation midLayerSprite(boolean enabled) {
        return enabled ? modSprite("glint_button_icon") : null;
    }

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
        super(slot, initial, width, height, initial ? GlintSlotOnOffButton.enabledMsg(slot) : GlintSlotOnOffButton.disabledMsg(slot), onPress);
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
