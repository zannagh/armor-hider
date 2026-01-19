package de.zannagh.armorhider.rendering;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.resources.ServerWideSettings;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    
    public static void setupContext(EquipmentSlot slot, HumanoidRenderState entityRenderState) {
        setupContext(null, slot, entityRenderState);
    }

    /// Captures context for the render pipeline, used within other methods of the class.
    /// ItemStack can be null, slot can be null.
    public static void setupContext(@Nullable ItemStack itemStack, @NotNull EquipmentSlot slot, HumanoidRenderState entityRenderState) {
        setCurrentSlot(slot);
        if (itemStack != null) {
            ArmorModificationContext.setCurrentItemStack(itemStack);
        }

        // If current slot is null and elytra rendering is requested, set slot to chest
        if (ArmorModificationContext.getCurrentSlot() == null && ItemsUtil.itemStackContainsElytra(itemStack)) {
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

    public static boolean noContext() {
        return !ArmorModificationContext.hasActiveContext();
    }

    public static void clearContext() {
        ArmorModificationContext.clearAll();
    }

    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(@NotNull EquipmentSlot slot, String name) {
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(name));
    }

    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(@NotNull EquipmentSlot slot, LivingEntityRenderState state) {
        // In official mappings, displayName is called nameTag and is in EntityRenderState
        boolean isLocalPlayerEntityRenderState = state.nameTag == null;
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(
                isLocalPlayerEntityRenderState ? ArmorHiderClient.getCurrentPlayerName() : state.nameTag.getString()
        ));
    }

    private static EquipmentSlot getCurrentSlot() {
        return ArmorModificationContext.getCurrentSlot();
    }

    private static void setCurrentSlot(@NotNull EquipmentSlot slot) {
        ArmorModificationContext.setCurrentSlot(slot);
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

    public static int modifyRenderPriority(int originalPriority) {
        if (getCurrentModification() == null) {
            return originalPriority;
        }
        if (ArmorModificationContext.getCurrentItemStack().is(Items.ELYTRA)) {
            return ElytraRenderPriority; // Render after all armor (which uses priority 1)
        }
        if (ItemsUtil.isSkullBlockItem(ArmorModificationContext.getCurrentItemStack().getItem())) {
            return SkullRenderPriority; // Render after all armor (which uses priority 1)
        }
        return originalPriority; // Fallback, return original.
    }

    public static RenderType getSkullRenderLayer(Identifier texture, RenderType originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        // Only change render type if skull/hat opacity is enabled
        if (!modification.playerConfig().opacityAffectingHatOrSkull.getValue()) {
            return originalLayer;
        }
        // Only use translucent if actually applying transparency (not fully hidden or fully visible)
        double transparency = modification.getTransparency();
        if (transparency < 1.0 && transparency > 0) {
            return RenderTypes.entityTranslucent(texture);
        }
        return originalLayer;
    }

    public static RenderType getTranslucentArmorRenderTypeIfApplicable(Identifier texture, RenderType originalLayer) {
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

    public static int applyArmorTransparency(int originalColor) {
        if (getCurrentModification() != null && getCurrentModification().shouldModify() && shouldModifyEquipment()) {
            double transparency = getCurrentModification().getTransparency();
            int alpha = (int) (transparency * 255);
            return ARGB.color(alpha, ARGB.red(originalColor), ARGB.green(originalColor), ARGB.blue(originalColor));
        }
        return originalColor;
    }

    public static int applyTransparencyFromWhite(int original) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return original;
        }
        double transparency = ArmorRenderPipeline.getCurrentModification().getTransparency();
        int alpha = (int) (transparency * 255);
        return ARGB.color(alpha, 255, 255, 255);
    }
    //endregion
}
