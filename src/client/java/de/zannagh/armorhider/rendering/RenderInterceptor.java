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

public class RenderInterceptor {

    public static void setupRenderContext(ItemStack itemStack, LivingEntityRenderState entityRenderState) {
        // If current slot is null and elytra rendering is requested, set slot to chest
        if (ArmorModificationContext.getCurrentSlot() == null && ItemStackHelper.itemStackContainsElytra(itemStack)) {
            ArmorModificationContext.setCurrentSlot(EquipmentSlot.CHEST);
        }

        ArmorHiderClient.trySetCurrentSlotFromEntityRenderState(entityRenderState);
    }
    
    public static int modifyRenderPriority(int originalPriority, ItemStack itemStack) {
        if (ItemStackHelper.itemStackContainsElytra(itemStack)) {
            return 100; // Render after all armor (which uses priority 1)
        }
        return originalPriority;
    }

    public static RenderLayer getRenderLayer(Identifier texture, RenderLayer originalLayer, ArmorModificationInfo armorModInfo) {
        if (armorModInfo == null) {
            return originalLayer;
        }

        double transparency = armorModInfo.GetTransparency();

        if (transparency < 0.95) {
            return RenderLayer.getEntityTranslucent(texture);
        }

        return originalLayer;
    }

    public static RenderLayer getTrimRenderLayer(boolean decal, RenderLayer originalLayer, ArmorModificationInfo armorModInfo) {
        if (armorModInfo == null || !armorModInfo.ShouldModify()) {
            return originalLayer;
        }

        if (armorModInfo.GetTransparency() < 1) {
            return RenderLayer.createArmorTranslucent(net.minecraft.client.render.TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
        }

        return originalLayer;
    }

    public static int applyTransparency(int originalColor, ArmorModificationInfo armorModInfo) {
        if (armorModInfo == null || !armorModInfo.ShouldModify()) {
            return originalColor;
        }

        double transparency = armorModInfo.GetTransparency();
        return ColorHelper.withAlpha(ColorHelper.channelFromFloat((float)transparency), originalColor);
    }
    
    public static boolean shouldHideEquipment(ArmorModificationInfo armorModInfo) {
        return armorModInfo != null && armorModInfo.ShouldHide();
    }

    public static boolean shouldModifyEquipment(ArmorModificationInfo armorModInfo) {
        return armorModInfo != null && armorModInfo.ShouldModify();
    }

    public static boolean shouldNotInterceptRender(Object renderState) {
        return renderState instanceof PlayerEntityRenderState;
    }
}
