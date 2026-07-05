package de.zannagh.armorhider.client.common;

import de.zannagh.armorhider.common.ItemInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class EquippableInformation {

    private @NonNull ItemStack stack;

    private @NonNull EquipmentSlot slot;

    private ItemInfo itemInfo;

    private boolean isValid;

    public EquippableInformation(@Nullable Object carrier, @Nullable EquipmentSlot slot, @Nullable ItemStack stack) {
        this.stack = ItemStack.EMPTY;
        this.slot = EquipmentSlot.MAINHAND;
        this.itemInfo = new ItemInfo(ItemStack.EMPTY);

        if (carrier == null && slot == null && stack == null) {
            return;
        }

        if (slot != null && stack != null) {
            // Can only resolve without carrier by having both pieces of slot and item.
            this.stack = stack;
            this.slot = slot;
            this.itemInfo = new ItemInfo(this.stack);
            isValid = true;
            return;
        }

        if (carrier instanceof IdentityCarrier identityCarrier) {
            if (slot == null && stack != null) {
                this.stack = stack;
                this.itemInfo = new ItemInfo(stack);
                var equippableSlot = this.itemInfo.getEquippableSlot();
                if (equippableSlot == null) {
                    return;
                }
                this.slot = equippableSlot;
                isValid = true;
                return;
            }
            if (slot != null && stack == null) {
                this.stack = identityCarrier.getItemBySlot(slot);
                this.slot = slot;
                this.itemInfo = new ItemInfo(this.stack);
                isValid = true;
            }
        }
    }

    public boolean isValid() { return isValid; }

    public @NonNull EquipmentSlot getSlot() { return slot; }

    public @NonNull ItemStack getStack() { return stack; }

    public ItemInfo getItemInfo() { return itemInfo; }

}
