package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

class ArmorModificationContext {
    private static final ThreadLocal<ItemStack> currentItemStack = ThreadLocal.withInitial(() -> ItemStack.EMPTY);
    private static final ThreadLocal<EquipmentSlot> currentSlot = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<ArmorModificationInfo> currentModification = ThreadLocal.withInitial(() -> null);

    public static EquipmentSlot getCurrentSlot() {
        return currentSlot.get();
    }

    public static void setCurrentSlot(EquipmentSlot slot) {
        currentSlot.set(slot);
    }

    public static ItemStack getCurrentItemStack() {
        return currentItemStack.get();
    }

    public static void setCurrentItemStack(@NotNull ItemStack stack) {
        currentItemStack.set(stack);
    }

    public static ArmorModificationInfo getCurrentModification() {
        return currentModification.get();
    }

    public static void setCurrentModification(ArmorModificationInfo modification) {
        currentModification.set(modification);
    }

    public static boolean hasActiveContext() {
        return currentModification.get() != null;
    }

    public static boolean shouldHideEquipment() {
        ArmorModificationInfo modification = currentModification.get();
        return modification != null && modification.shouldHide();
    }

    public static boolean shouldModifyEquipment() {
        ArmorModificationInfo modification = currentModification.get();
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHiderForOthers.getValue()
                && modification != null
                && modification.isConfigForRemotePlayer(ArmorHiderClient.getCurrentPlayerName())) {
            return false;
        }
        if (modification != null 
                && modification.equipmentSlot() == EquipmentSlot.HEAD 
                && ItemsUtil.isSkullBlockItem(getCurrentItemStack().getItem()) // assumes that any context setup with skull blocks is applying the current item as well
                && !modification.playerConfig().opacityAffectingHatOrSkull.getValue()) {
            return false;
        }
        if (modification != null 
                && modification.equipmentSlot() == EquipmentSlot.CHEST 
                && ItemsUtil.itemStackContainsElytra(getCurrentItemStack()) 
                && !modification.playerConfig().opacityAffectingElytra.getValue()) {
            return false;
        }
        return modification != null && modification.shouldModify();
    }

    public static void clearAll() {
        currentSlot.remove();
        currentModification.remove();
        currentItemStack.set(ItemStack.EMPTY);
    }
}
