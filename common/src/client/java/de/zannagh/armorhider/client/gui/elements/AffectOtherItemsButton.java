package de.zannagh.armorhider.client.gui.elements;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

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

    public AffectOtherItemsButton(boolean initial, EquipmentSlot slot, int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(slot, x, y, width, height, message, onPress, createNarration);
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
