package de.zannagh.armorhider.rendering;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ItemStackHelper;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class ArmorRenderPipeline {

    /// Elytra to render as last item in usual armor queue (armor items usually use k 0-3) as it 
    /// has the highest chance to overlap other items.
    public static final int ElytraRenderPriority = 100;

    /// See ElytraRenderPriority. Skull should usually render ahead of Elytra, in case the Elytra is visually infront of the skull.
    public static final int SkullRenderPriority = 99;
    
    /// Captures context for the render pipeline, used within other methods of the class.
    /// ItemStack can be null, slot can be null.
    public static void setupContext(ItemStack itemStack, EquipmentSlot slot, LivingEntityRenderState entityRenderState) {
        
        if (slot != null) {
            setCurrentSlot(slot);
        }
        
        // If current slot is null and elytra rendering is requested, set slot to chest
        if (ArmorModificationContext.getCurrentSlot() == null && ItemStackHelper.itemStackContainsElytra(itemStack)) {
            ArmorModificationContext.setCurrentSlot(EquipmentSlot.CHEST);
        }

        if (entityRenderState instanceof PlayerEntityRenderState playerEntityRenderState && getCurrentSlot() != null) {
            var configByEntityState = tryResolveConfigFromPlayerEntityState(
                    getCurrentSlot(),
                    playerEntityRenderState
            );
            setCurrentModification(configByEntityState);
        }
    }

    /// In case context is missing (current modification information), this tries to add the missing context from the entity render state.
    public static void addContext(Object entityRenderState) {
        if (getCurrentModification() == null 
                && getCurrentSlot() != null
                && entityRenderState instanceof PlayerEntityRenderState playerEntityRenderState) {
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

    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(EquipmentSlot slot, PlayerEntityRenderState state){
        boolean isLocalPlayerEntityRenderState = state.displayName == null;
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(isLocalPlayerEntityRenderState ? ArmorHiderClient.getCurrentPlayerName() : state.displayName.getString()));
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
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().disableArmorHider.getValue()) {
            return false;
        }
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig().serverWideSettings instanceof ServerWideSettings serverWideSettings
                && serverWideSettings.forceArmorHiderOff.getValue()) {
            return false;
        }
        return ArmorModificationContext.shouldModifyEquipment();
    }

    public static boolean renderStateDoesNotTargetPlayer(Object renderState) {
        return !(renderState instanceof PlayerEntityRenderState);
    }

    public static int modifyRenderPriority(int originalPriority, ItemStack itemStack) {
        if (ItemStackHelper.itemStackContainsElytra(itemStack)) {
            return ElytraRenderPriority; // Render after all armor (which uses priority 1)
        }
        return originalPriority;
    }

    public static RenderLayer getRenderLayer(Identifier texture, RenderLayer originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        
        return RenderLayers.entityTranslucent(texture);
    }

    public static RenderLayer getTrimRenderLayer(boolean decal, RenderLayer originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        
        return RenderLayers.armorTranslucent(net.minecraft.client.render.TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
    }

    public static int applyTransparency(int originalColor) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalColor;
        }
        
        double transparency = modification.getTransparency();
        return ColorHelper.withAlpha(ColorHelper.channelFromFloat((float)transparency), originalColor);
    }
    //endregion
}
