package de.zannagh.armorhider.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemsUtil {
    public static final ItemStack ELYTRA_ITEM_STACK = new ItemStack(net.minecraft.world.item.Items.ELYTRA);

    public static boolean itemStackContainsElytra(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        return itemStack.getComponents().has(DataComponents.GLIDER) || itemStack.is(ELYTRA_ITEM_STACK.getItem()) || itemStack.getItem().toString().toLowerCase().contains("elytra");
    }
}
