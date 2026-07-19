package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/**
 * Per-slot toggle deciding whether the accessory type mapped to this armor slot (hat / necklace / belt /
 * boots) is hidden together with the slot. Placeholder foreground uses the slot's armor icon — swap for
 * dedicated accessory art. Only takes effect while the master {@link AffectAccessoriesButton} is on.
 */
public class AccessoryAffectButton extends LayeredImageButton {

    public AccessoryAffectButton(boolean initial, EquipmentSlot slot, int width, int height, OnPress onPress) {
        super(slot, initial, width, height, initial ? enabledMsg(slot) : disabledMsg(slot), onPress);
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        if (slot == null) {
            return null;
        }
        return switch (slot) {
            case HEAD -> modSprite("iron_helmet");
            case CHEST -> modSprite("iron_chestplate");
            case LEGS -> modSprite("iron_leggings");
            case FEET -> modSprite("iron_boots");
            default -> null;
        };
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

    private static Component enabledMsg(@Nullable EquipmentSlot slot) {
        return Component.translatable("armorhider.options.affect_accessory." + slotKey(slot) + ".tooltip.enabled");
    }

    private static Component disabledMsg(@Nullable EquipmentSlot slot) {
        return Component.translatable("armorhider.options.affect_accessory." + slotKey(slot) + ".tooltip.disabled");
    }

    private static String slotKey(@Nullable EquipmentSlot slot) {
        if (slot == null) {
            return "head";
        }
        return switch (slot) {
            case CHEST -> "chest";
            case LEGS -> "legs";
            case FEET -> "feet";
            default -> "head";
        };
    }
}
