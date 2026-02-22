package de.zannagh.armorhider.rendering;

import com.mojang.authlib.GameProfile;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import de.zannagh.armorhider.resources.ServerWideSettings;
import de.zannagh.armorhider.util.ItemsUtil;
//? if >= 1.21.4 {
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
//?}
//? if >= 1.21.9
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//? if >= 1.21.4 && < 1.21.9
//import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//? if < 1.21.4 {
/*import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
//? }
//? if >= 1.21.4 && < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}

public class ArmorRenderPipeline {

    /// Elytra to render as last item in usual armor queue (armor items usually use k 0-3) as it
    /// has the highest chance to overlap other items.
    public static final int ElytraRenderPriority = 100;

    /// See ElytraRenderPriority. Skull should usually render ahead of Elytra, in case the Elytra is visually infront of the skull.
    public static final int SkullRenderPriority = 99;
    
    //? if >= 1.21.4
    public static final ThreadLocal<LivingEntityRenderState> CURRENT_ENTITY_RENDER_STATE = new ThreadLocal<>();
    //? if < 1.21.4
    //public static final ThreadLocal<LivingEntity> CURRENT_ENTITY_RENDER_STATE = new ThreadLocal<>();

    public static void setupContext(@Nullable ItemStack itemStack, @NotNull EquipmentSlot slot, @NotNull GameProfile profile) {
        setCurrentSlot(slot);
        if (getCurrentSlot() != null) {
            //? if >= 1.21.9
            String profileName = profile.name();
            //? if < 1.21.9
            //String profileName = profile.getName();
            var configByEntityState = tryResolveConfigFromPlayerEntityState(
                    getCurrentSlot(),
                    profileName
            );
            setCurrentModification(configByEntityState);
        }
        if (itemStack != null) {
            ArmorModificationContext.setCurrentItemStack(itemStack);
        }
    }

    //? if >= 1.21.4 {
    public static void setupContext(EquipmentSlot slot, HumanoidRenderState entityRenderState) {
        setupContext(null, slot, entityRenderState);
        CURRENT_ENTITY_RENDER_STATE.set(entityRenderState);
    }

    /// Captures context for the render pipeline, used within other methods of the class.
    /// ItemStack can be null, slot can be null.
    public static void setupContext(@Nullable ItemStack itemStack, @NotNull EquipmentSlot slot, HumanoidRenderState entityRenderState) {
        setCurrentSlot(slot);
        CURRENT_ENTITY_RENDER_STATE.set(entityRenderState);
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
    //?}

    //? if < 1.21.4 {
    /*public static void setupContext(EquipmentSlot slot, LivingEntity entity) {
        setupContext(null, slot, entity);
    }

    /// Captures context for the render pipeline, used within other methods of the class.
    /// ItemStack can be null, slot can be null.
    public static void setupContext(@Nullable ItemStack itemStack, @NotNull EquipmentSlot slot, LivingEntity entity) {
        setCurrentSlot(slot);
        CURRENT_ENTITY_RENDER_STATE.set(entity);
        if (itemStack != null) {
            ArmorModificationContext.setCurrentItemStack(itemStack);
        }

        // If current slot is null and elytra rendering is requested, set slot to chest
        if (ArmorModificationContext.getCurrentSlot() == null && ItemsUtil.itemStackContainsElytra(itemStack)) {
            ArmorModificationContext.setCurrentSlot(EquipmentSlot.CHEST);
        }

        if (getCurrentSlot() != null) {
            var configByEntityState = tryResolveConfigFromEntity(
                    getCurrentSlot(),
                    entity
            );
            setCurrentModification(configByEntityState);
        }
    }
    *///?}

    public static boolean noContext() {
        return !ArmorModificationContext.hasActiveContext();
    }

    public static boolean hasActiveContext() {
        return ArmorModificationContext.hasActiveContext();
    }

    public static void clearContext() {
        ArmorModificationContext.clearAll();
        CURRENT_ENTITY_RENDER_STATE.remove();
    }

    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(@NotNull EquipmentSlot slot, String name) {
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(name));
    }

    //? if >= 1.21.4 {
    private static ArmorModificationInfo tryResolveConfigFromPlayerEntityState(@NotNull EquipmentSlot slot, LivingEntityRenderState state) {
        // In official mappings, displayName is called nameTag and is in EntityRenderState
        boolean isLocalPlayerEntityRenderState = state.nameTag == null;
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(
                isLocalPlayerEntityRenderState ? ArmorHiderClient.getCurrentPlayerName() : state.nameTag.getString()
        ));
    }
    //?}

    //? if < 1.21.4 {
    /*private static ArmorModificationInfo tryResolveConfigFromEntity(@NotNull EquipmentSlot slot, LivingEntity entity) {
        String playerName;
        if (entity instanceof Player player) {
            playerName = player.getName().getString();
        } else {
            // For non-player entities, use a default or empty config
            playerName = ArmorHiderClient.getCurrentPlayerName();
        }
        return new ArmorModificationInfo(slot, ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName));
    }
    *///?}

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
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig() != null) {
            ServerWideSettings serverWideSettings = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig().serverWideSettings;
            if (serverWideSettings != null && serverWideSettings.forceArmorHiderOff.getValue()) {
                return false;
            }
        }
        
        return ArmorModificationContext.shouldModifyEquipment();
    }

    //? if >= 1.21.9 {
    public static boolean renderStateDoesNotTargetPlayer(Object renderState) {
        return !(renderState instanceof AvatarRenderState);
    }
    //?}

    //? if >= 1.21.4 && < 1.21.9 {
    /*public static boolean renderStateDoesNotTargetPlayer(Object renderState) {
        return !(renderState instanceof PlayerRenderState);
    }
    *///?}

    //? if < 1.21.4 {
    /*public static boolean entityIsNotPlayer(Object entity) {
        return !(entity instanceof Player);
    }
    *///?}

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

    /**
     * This gets the skull render layer. Requires a context to be set up prior, otherwise it will return the original layer.
    */
    //? if >= 1.21.11 
    public static RenderType getSkullRenderLayer(Identifier texture, RenderType originalLayer) {
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType getSkullRenderLayer(ResourceLocation texture, RenderType originalLayer) {
    //? if < 1.21.4
    //public static RenderType getSkullRenderLayer(ResourceLocation texture, RenderType originalLayer) {
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
        if (transparency < 1.0 && transparency >= 0) {
            //? if >= 1.21.11
            return RenderTypes.entityTranslucent(texture);
            //? if >= 1.21.4 && < 1.21.11
            //return RenderType.entityTranslucent(texture);
            //? if < 1.21.4
            //return RenderType.entityTranslucent(texture);
        }
        return originalLayer;
    }

    //? if >= 1.21.11
    public static RenderType getTranslucentArmorRenderTypeIfApplicable(Identifier texture, RenderType originalLayer) {
    //? if >= 1.21.4 && < 1.21.11
    //public static RenderType getTranslucentArmorRenderTypeIfApplicable(ResourceLocation texture, RenderType originalLayer) {
    //? if < 1.21.4
    //public static RenderType getTranslucentArmorRenderTypeIfApplicable(ResourceLocation texture, RenderType originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        // Only use translucent render type when actually applying transparency (not fully opaque or hidden)
        double transparency = modification.getTransparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        //? if >= 1.21.11
        return RenderTypes.armorTranslucent(texture);
        //? if >= 1.21.4 && < 1.21.11
        //return RenderType.armorTranslucent(texture);
        //? if < 1.21.4
        //return RenderType.entityTranslucent(texture);
    }

    //? if >= 1.21.4 {
    public static RenderType getTrimRenderLayer(boolean decal, RenderType originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        // Only use translucent render type when actually applying transparency
        double transparency = modification.getTransparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        //? if >= 1.21.11
        return RenderTypes.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET);
        //? if >= 1.21.4 && < 1.21.11
        //return RenderType.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET);
    }
    //?}

    /*
     * Returns a translucent render type for item rendering if the current context requires transparency.
     * Swaps cutout block render types to their translucent equivalents for proper alpha blending.
     * Regular items already use translucent sheet types and don't need swapping.
     */
    public static RenderType getTranslucentItemRenderTypeIfApplicable(RenderType originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        double transparency = modification.getTransparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        if (originalLayer == Sheets.cutoutBlockSheet()) {
            //? if >= 1.21.11
            return Sheets.translucentBlockItemSheet();
            //? if < 1.21.11
            //return Sheets.translucentItemSheet();
        }
        return originalLayer;
    }

    public static int applyArmorTransparency(int originalColor) {
        if (getCurrentModification() != null && getCurrentModification().shouldModify() && shouldModifyEquipment()) {
            double transparency = getCurrentModification().getTransparency();
            int alpha = (int) (transparency * 255);
            //? if >= 1.21.4
            return ARGB.color(alpha, ARGB.red(originalColor), ARGB.green(originalColor), ARGB.blue(originalColor));

            //? if < 1.21.4 {
            /*int red = (originalColor >> 16) & 0xFF;
            int green = (originalColor >> 8) & 0xFF;
            int blue = originalColor & 0xFF;
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
            *///?}
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
        //? if >= 1.21.4
        return ARGB.color(alpha, 255, 255, 255);
        //? if < 1.21.4
        //return (alpha << 24) | (255 << 16) | (255 << 8) | 255;
    }

    /**
     * Gets the transparency alpha value as a float for rendering compatibility.
     * @return alpha value between 0.0 and 1.0
     */
    public static float getTransparencyAlpha() {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return 1.0f;
        }
        return (float) modification.getTransparency();
    }

    //? if < 1.21.4 {
    /*/^*
     * Gets a translucent render layer for armor trims in 1.20.x.
     ^/
    public static RenderType getTrimRenderLayer(boolean decal, RenderType originalLayer) {
        ArmorModificationInfo modification = getCurrentModification();
        if (modification == null || !modification.shouldModify() || !shouldModifyEquipment()) {
            return originalLayer;
        }
        double transparency = modification.getTransparency();
        if (transparency >= 1.0 || transparency <= 0) {
            return originalLayer;
        }
        return RenderType.entityTranslucent(net.minecraft.client.renderer.Sheets.ARMOR_TRIMS_SHEET);
    }
    *///?}
    //endregion
}
