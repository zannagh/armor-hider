package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ItemStackHelper;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class ArmorRenderPipeline {

    public static void setupContext(ItemStack itemStack, LivingEntityRenderState entityRenderState) {
        // If current slot is null and elytra rendering is requested, set slot to chest
        if (ArmorModificationContext.getCurrentSlot() == null && ItemStackHelper.itemStackContainsElytra(itemStack)) {
            ArmorModificationContext.setCurrentSlot(EquipmentSlot.CHEST);
        }

        ArmorHiderClient.trySetCurrentSlotFromEntityRenderState(entityRenderState);
    }

    public static void setCurrentSlot(EquipmentSlot slot) {
        ArmorModificationContext.setCurrentSlot(slot);
    }

    public static EquipmentSlot getCurrentSlot() {
        return ArmorModificationContext.getCurrentSlot();
    }

    public static ArmorModificationInfo getCurrentModification() {
        return ArmorModificationContext.getCurrentModification();
    }

    public static void setCurrentModification(ArmorModificationInfo modification) {
        ArmorModificationContext.setCurrentModification(modification);
    }
    public static void clearContext() {
        ArmorModificationContext.clearAll();
    }

    // region RenderMethods
    public static boolean hasActiveContext() {
        return ArmorModificationContext.hasActiveContext();
    }
    
    public static boolean shouldHideEquipment() {
        return ArmorModificationContext.shouldHideEquipment();
    }

    public static boolean shouldModifyEquipment() {
        return ArmorModificationContext.shouldModifyEquipment();
    }

    public static double getTransparency() {
        return ArmorModificationContext.getTransparency();
    }

    public static boolean shouldInterceptRender(Object renderState) {
        return !(renderState instanceof PlayerEntityRenderState);
    }

    public static int modifyRenderPriority(int originalPriority, ItemStack itemStack) {
        if (ItemStackHelper.itemStackContainsElytra(itemStack)) {
            return 100; // Render after all armor (which uses priority 1)
        }
        return originalPriority;
    }

    public static RenderLayer getRenderLayer(Identifier texture, RenderLayer originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null) {
            return originalLayer;
        }

        double transparency = modification.GetTransparency();

        if (transparency < 0.95) {
            return RenderLayer.getEntityTranslucent(texture);
        }

        return originalLayer;
    }

    public static RenderLayer getTrimRenderLayer(boolean decal, RenderLayer originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.ShouldModify()) {
            return originalLayer;
        }

        if (modification.GetTransparency() < 1) {
            return RenderLayer.createArmorTranslucent(net.minecraft.client.render.TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
        }

        return originalLayer;
    }

    public static int applyTransparency(int originalColor) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.ShouldModify()) {
            return originalColor;
        }

        double transparency = modification.GetTransparency();
        return ColorHelper.withAlpha(ColorHelper.channelFromFloat((float)transparency), originalColor);
    }
    //endregion
}
