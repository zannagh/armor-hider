package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

//? if < 1.21
//import net.minecraft.ChatFormatting;

public class AffectOtherItemsButton extends LayeredButton {
    @Nullable private final Identifier slotSprite;

    @Override
    protected @Nullable Identifier midLayerSprite() {
        return slotSprite;
    }

    //? if < 1.21 {
    /*@Override
    protected @Nullable Component statusOverlay() {
        return isEnabled ? Component.literal("\u2713").withStyle(ChatFormatting.GREEN) : Component.literal("\u2717").withStyle(ChatFormatting.RED);
    }
    *///?}

    //? if >= 1.21 {
    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return enabled ? modSprite("accept_highlighted") : modSprite("reject_highlighted");
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
            slotSprite = modSprite("skull_banner_pattern");
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
