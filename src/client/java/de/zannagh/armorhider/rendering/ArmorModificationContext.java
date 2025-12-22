package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.resources.ArmorModificationInfo;
import net.minecraft.entity.EquipmentSlot;

class ArmorModificationContext {

    private static final ThreadLocal<EquipmentSlot> currentSlot = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<ArmorModificationInfo> currentModification = ThreadLocal.withInitial(() -> null);

    public static EquipmentSlot getCurrentSlot() {
        return currentSlot.get();
    }

    public static void setCurrentSlot(EquipmentSlot slot) {
        currentSlot.set(slot);
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
        return modification != null && modification.ShouldHide();
    }

    public static boolean shouldModifyEquipment() {
        ArmorModificationInfo modification = currentModification.get();
        return modification != null && modification.ShouldModify();
    }

    public static double getTransparency() {
        ArmorModificationInfo modification = currentModification.get();
        return modification != null ? modification.GetTransparency() : 1.0;
    }

    public static void clearSlot() {
        currentSlot.remove();
    }

    public static void clearModification() {
        currentModification.remove();
    }

    public static void clearAll() {
        currentSlot.remove();
        currentModification.remove();
    }
}
