package de.zannagh.armorhider.client.gui.elements;

import com.sun.jna.platform.win32.Variant;
import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class GlintSlotOnOffButton extends LayeredButton {
    //? if >= 1.21 {
    @Nullable private final ItemStack slotSprite;
    @Override
    protected ItemStack midLayer() { return slotSprite; }

    @Override
    protected Function<Boolean, @Nullable Identifier> spriteForeground() {
        return this::spriteForeground;
    }

    protected Identifier spriteForeground(Boolean input) { return super.isEnabled ? Identifier.withDefaultNamespace("hud/air_bursting") : Identifier.withDefaultNamespace(""); }
    //?}

    public GlintSlotOnOffButton(boolean initial, EquipmentSlot slot, int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(slot, x, y, width, height, message, onPress, createNarration);
        //? if >= 1.21 {
        if (slot == EquipmentSlot.HEAD) {
            slotSprite = new ItemStack(Items.IRON_HELMET);
            this.isEnabled = initial;
        }
        else if (slot == EquipmentSlot.CHEST) {
            slotSprite = new ItemStack(Items.IRON_CHESTPLATE);
            this.isEnabled = initial;
        }
        else if (slot == EquipmentSlot.LEGS) {
            slotSprite = new ItemStack(Items.IRON_LEGGINGS);
            this.isEnabled = initial;
        }
        else if (slot == EquipmentSlot.FEET) {
            slotSprite = new ItemStack(Items.IRON_BOOTS);
            this.isEnabled = initial;
        }
        else {
            slotSprite = null;
        }
        //?}
        super.setEnabled(initial);
    }
}