package de.zannagh.armorhider.common;

import net.minecraft.item.ItemStack;

public class ItemStackHelper {
    
    public static boolean itemStackContainsElytra(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        return itemStack.getItem().toString().toLowerCase().contains("elytra");
    }
}
