package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class AffectOtherItemsButton extends LayeredButton {
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
    protected @Nullable Component statusOverlay() {
        return isEnabled ? Component.literal("✓").withStyle(ChatFormatting.GREEN) : Component.literal("✗").withStyle(ChatFormatting.RED);
    }

    //? if >= 1.21 {
    @Override
    protected Function<Boolean, @Nullable Identifier> spriteForeground() {
        return this::spriteForeground;
    }

    protected Identifier spriteForeground(Boolean input) { return input ? Identifier.withDefaultNamespace("pending_invite/accept_highlighted") : Identifier.withDefaultNamespace("spectator/close"); }
    //? }

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
            slotItem = Items.SKELETON_SKULL;
        }
        else if (slot == EquipmentSlot.CHEST) {
            slotItem = Items.ELYTRA;
        }
        else {
            slotItem = null;
        }
        super.setEnabled(initial);
    }
}
