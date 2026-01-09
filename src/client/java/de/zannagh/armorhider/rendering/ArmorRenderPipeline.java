package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ItemStackHelper;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ArmorRenderPipeline {

    /// Elytra to render as last item in usual armor queue (armor items usually use k 0-3) as it 
    /// has the highest chance to overlap other items.
    public static final int ElytraRenderPriority = 100;

    /// See ElytraRenderPriority. Skull should usually render ahead of Elytra, in case the Elytra is visually infront of the skull.
    public static final int SkullRenderPriority = 99;

    /// Captures context for the render pipeline, used within other methods of the class.
    /// ItemStack can be null, slot can be null.
    public static void setupContext(ItemStack itemStack, EquipmentSlot slot, LivingEntity entity) {
        
        if (slot != null) {
            setCurrentSlot(slot);
        }
        
        // If current slot is null and elytra rendering is requested, set slot to chest
        if (ArmorModificationContext.getCurrentSlot() == null && ItemStackHelper.itemStackContainsElytra(itemStack)) {
            ArmorModificationContext.setCurrentSlot(EquipmentSlot.CHEST);
        }

        if (getCurrentSlot() != null && entity instanceof PlayerEntity playerEntity) {
            var configByEntityState = tryResolveConfigFromPlayerEntityState(
                    getCurrentSlot(),
                    playerEntity
            );
            setCurrentModification(configByEntityState);
        }
    }

    /// In case context is missing (current modification information), this tries to add the missing context from the entity render state.
    public static void addContext(Object entityRenderState) {
        if (getCurrentModification() == null 
                && getCurrentSlot() != null
                && entityRenderState instanceof PlayerEntity playerEntityRenderState) {
            var config = tryResolveConfigFromPlayerEntityState(
                    ArmorRenderPipeline.getCurrentSlot(),
                    playerEntityRenderState
            );
            setCurrentModification(config);
        }
    }

    public static boolean hasActiveContext() {
        return ArmorModificationContext.hasActiveContext();
    }

    public static void clearContext() {
        ArmorModificationContext.clearAll();
    }

    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(EquipmentSlot slot, PlayerEntity state){
        return state.getDisplayName() == null
                ? new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue())
                : new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(state.getDisplayName().getString()));
    }

    private static void setCurrentSlot(EquipmentSlot slot) {
        ArmorModificationContext.setCurrentSlot(slot);
    }

    private static EquipmentSlot getCurrentSlot() {
        return ArmorModificationContext.getCurrentSlot();
    }

    public static ArmorModificationInfo getCurrentModification() {
        return ArmorModificationContext.getCurrentModification();
    }

    private static void setCurrentModification(ArmorModificationInfo modification) {
        ArmorModificationContext.setCurrentModification(modification);
    }

    // region RenderMethods
    public static boolean shouldHideEquipment() {
        return ArmorModificationContext.shouldHideEquipment();
    }

    public static boolean shouldModifyEquipment() {
        return ArmorModificationContext.shouldModifyEquipment();
    }

    public static boolean renderStateDoesNotTargetPlayer(Object renderState) {
        return !(renderState instanceof PlayerEntity);
    }

    public static int modifyRenderPriority(int originalPriority, ItemStack itemStack) {
        if (ItemStackHelper.itemStackContainsElytra(itemStack)) {
            return ElytraRenderPriority; // Render after all armor (which uses priority 1)
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
            return RenderLayer.getEntityTranslucent(net.minecraft.client.render.TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
        }

        return originalLayer;
    }

    public static int applyTransparency(int originalColor) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.ShouldModify()) {
            return originalColor;
        }

        double transparency = modification.GetTransparency();
        // Convert float (0-1) to channel value (0-255) for 1.20.1 compatibility
        int alphaChannel = (int) Math.round(transparency * 255.0);

        // 1.20.1 compatibility: manually combine alpha with RGB
        // Extract RGB components and combine with new alpha
        int rgb = originalColor & 0x00FFFFFF;  // Keep only RGB, remove alpha
        return (alphaChannel << 24) | rgb;      // Add new alpha channel
    }

    /**
     * Gets the transparency alpha value as a float for 1.20.1 rendering compatibility.
     * @return alpha value between 0.0 and 1.0
     */
    public static float getTransparencyAlpha() {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.ShouldModify()) {
            return 1.0f;
        }
        return (float) modification.GetTransparency();
    }
    //endregion
}
