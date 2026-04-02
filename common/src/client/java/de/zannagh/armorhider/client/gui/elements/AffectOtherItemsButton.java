package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class AffectOtherItemsButton extends LayeredButton {
    //? if >= 1.21 {
    @Nullable private final ItemStack slotSprite;
    @Override
    protected ItemStack midLayer() { return slotSprite; }
    @Override
    protected Function<Boolean, @Nullable Identifier> spriteForeground() {
        return this::spriteForeground;
    }

    protected Identifier spriteForeground(Boolean input) { return input ? Identifier.withDefaultNamespace("pending_invite/accept_highlighted") : Identifier.withDefaultNamespace("spectator/close"); }
    //? }

    public AffectOtherItemsButton(boolean initial, EquipmentSlot slot, int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(slot, x, y, width, height, message, onPress, createNarration);
        //? if >= 1.21 {
        if (slot == EquipmentSlot.HEAD) {
            slotSprite = new ItemStack(Items.SKELETON_SKULL);
            isEnabled = initial;
        }
        else if (slot == EquipmentSlot.CHEST) {
            slotSprite = new ItemStack(Items.ELYTRA);
            isEnabled = initial;
        }
        else {
            slotSprite = null;
        }
        //?}
        super.setEnabled(initial);
    }
}
