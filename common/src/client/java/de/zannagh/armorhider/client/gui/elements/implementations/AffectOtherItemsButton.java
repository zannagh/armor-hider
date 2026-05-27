package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.LayeredButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

//? if < 1.21
//import net.minecraft.ChatFormatting;

public class AffectOtherItemsButton extends LayeredButton {
    @Nullable private final Identifier slotSprite;

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return slotSprite;
    }

    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? modSprite("accept_highlighted") : modSprite("reject_highlighted");
    }

    @Override
    protected Component enabledMessage() {
        return enabledMsg(slot);
    }

    @Override
    protected Component disabledMessage() {
        return disabledMsg(slot);
    }

    private static Component enabledMsg(EquipmentSlot slot){
        if (slot == EquipmentSlot.HEAD) {
            return Component.translatable("armorhider.options.helmet_affection.tooltip.enabled");
        }
        if (slot == EquipmentSlot.CHEST) {
            return Component.translatable("armorhider.options.elytra_affection.tooltip.enabled");
        }
        return Component.empty();
    }

    private static Component disabledMsg(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            return Component.translatable("armorhider.options.helmet_affection.tooltip.disabled");
        }
        if (slot == EquipmentSlot.CHEST) {
            return Component.translatable("armorhider.options.elytra_affection.tooltip.disabled");
        }
        return Component.empty();
    }

    public AffectOtherItemsButton(boolean initial, EquipmentSlot slot, int width, int height, OnPress onPress) {
        super(slot, width, height, initial ? enabledMsg(slot) : disabledMsg(slot), onPress);
        if (slot == EquipmentSlot.HEAD) {
            slotSprite = modSprite("affect_head_slot_button");
        }
        else if (slot == EquipmentSlot.CHEST) {
            slotSprite = modSprite("elytra");
        }
        else {
            slotSprite = null;
        }
        super.setEnabled(initial);
    }
}
