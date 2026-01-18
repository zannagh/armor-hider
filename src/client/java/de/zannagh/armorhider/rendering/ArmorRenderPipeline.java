package de.zannagh.armorhider.rendering;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.ItemStackHelper;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArmorRenderPipeline {

    /// Elytra to render as last item in usual armor queue (armor items usually use k 0-3) as it 
    /// has the highest chance to overlap other items.
    public static final int ElytraRenderPriority = 100;

    /// See ElytraRenderPriority. Skull should usually render ahead of Elytra, in case the Elytra is visually infront of the skull.
    public static final int SkullRenderPriority = 99;

    public static void setupContext(EquipmentSlot slot, GameProfile profile) {

        if (slot != null) {
            setCurrentSlot(slot);
        }
        
        if (getCurrentSlot() != null) {
            var configByEntityState = tryResolveConfigFromPlayerEntityState(
                    getCurrentSlot(),
                    profile.name()
            );
            setCurrentModification(configByEntityState);
        }
    }
    
    /// Captures context for the render pipeline, used within other methods of the class.
    /// ItemStack can be null, slot can be null.
    public static void setupContext(@Nullable ItemStack itemStack, @NotNull EquipmentSlot slot, HumanoidRenderState entityRenderState) {
        setCurrentSlot(slot);
        
        // If current slot is null and elytra rendering is requested, set slot to chest
        if (ArmorModificationContext.getCurrentSlot() == null && ItemStackHelper.itemStackContainsElytra(itemStack)) {
            ArmorModificationContext.setCurrentSlot(EquipmentSlot.CHEST);
        }

        if (getCurrentSlot() != null) {
            var configByEntityState = tryResolveConfigFromPlayerEntityState(
                    getCurrentSlot(),
                    entityRenderState
            );
            setCurrentModification(configByEntityState);
        }
    }

    /// In case context is missing (current modification information), this tries to add the missing context from the entity render state.
    public static void addContext(Object entityRenderState) {
        if (getCurrentModification() == null 
                && getCurrentSlot() != null
                && entityRenderState instanceof LivingEntityRenderState playerEntityRenderState) {
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

    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(@NotNull EquipmentSlot slot, String name){
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(name));
    }
    
    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(@NotNull EquipmentSlot slot, LivingEntityRenderState state){
        // In official mappings, displayName is called nameTag and is in EntityRenderState
        boolean isLocalPlayerEntityRenderState = state.nameTag == null;
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(
                isLocalPlayerEntityRenderState ? ArmorHiderClient.getCurrentPlayerName() : state.nameTag.getString()
        ));
    }

    private static void setCurrentSlot(@NotNull EquipmentSlot slot) {
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
        return !(renderState instanceof AvatarRenderState);
    }

    public static int modifyRenderPriority(int originalPriority, boolean isElytra) {
        if (isElytra) {
            return ElytraRenderPriority; // Render after all armor (which uses priority 1)
        }
        return originalPriority;
    }

    public static RenderType getRenderLayer(Identifier texture, RenderType originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        return RenderTypes.armorTranslucent(texture);
    }

    public static RenderType getTrimRenderLayer(boolean decal, RenderType originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        // Use armor trims sheet atlas for translucent trim rendering
        return RenderTypes.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET);
    }

    public static int applyTransparency(int originalColor) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalColor;
        }

        double transparency = modification.getTransparency();
        if (transparency < 1.0 && transparency > 0) {
            // Apply transparency to the alpha channel using ARGB format
            int alpha = (int) (transparency * 255);
            int red = (originalColor >> 16) & 0xFF;
            int green = (originalColor >> 8) & 0xFF;
            int blue = originalColor & 0xFF;
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        return originalColor;
    }
    //endregion
}
