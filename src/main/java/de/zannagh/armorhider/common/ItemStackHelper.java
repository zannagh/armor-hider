package de.zannagh.armorhider.common;

import net.minecraft.item.ItemStack;
import net.minecraft.world.chunk.PaletteType;

public class ItemStackHelper {
    
    public static boolean itemStackContainsElytra(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        return itemStack.getItem().toString().toLowerCase().contains("elytra");
    }
}
