//? if gender {
package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wildfire.render.BreastSide;
import com.wildfire.render.GenderArmorLayer;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21 {
import com.mojang.datafixers.util.Pair;
//?}

//? if >= 1.21.9 {
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderInterceptionResult;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
//?}

//? if >= 1.21 && < 1.21.9 {
/*import de.zannagh.armorhider.client.common.SlotModification;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if < 1.21 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
*///?}

/**
 * Compatibility mixin for Wildfire's Female Gender Mod.
 * Applies chest armor hiding, transparency, and glint control
 * to the breast armor geometry rendered by {@code GenderArmorLayer}
 * (or {@code GenderLayer} on pre-1.21 versions).
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
//? if >= 1.21
@Mixin(value = GenderArmorLayer.class, remap = false)
//? if < 1.21
//@Mixin(value = GenderLayer.class, remap = false)
public class GenderArmorLayerMixin {

    @Unique
    //? if >= 1.21
    private static final String BREAST_METHOD = "renderBreastArmor";
    //? if < 1.21
    //private static final String BREAST_METHOD = "renderVanillaLikeBreastArmor";

    @Unique
    private static final String TRIM_METHOD = "renderArmorTrim";

    //? if >= 1.21.9 {
    private Pair<Boolean, RenderInterceptionResult> interceptArmor(HumanoidRenderState state, EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        var interceptionResult = AhRenderInterceptionRegistryApi
                .getRenderer(RenderScope.ARMOR_PIECE).intercept(state, slot, stack, ci);
        if (!interceptionResult.shouldIntercept()) {
            return Pair.of(false, interceptionResult);
        }
        if (interceptionResult.shouldCancel()) {
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            return Pair.of(false, interceptionResult);
        }
        return Pair.of(true, interceptionResult);
    }
    //?}

    // ========================
    // renderBreastArmor (renderVanillaLikeBreastArmor on < 1.21)
    // ========================

    @Inject(method = "renderBreastArmor", at = @At("HEAD"), cancellable = true)
    private void interceptBreastArmor(
            //? if >= 1.21.9 {
            Identifier texture, PoseStack poseStack, SubmitNodeCollector collector, HumanoidRenderState state, BreastSide side, int color, MutableBoolean glint, MutableInt order, CallbackInfo ci
            //? } elif >= 1.21 {
            /*Identifier texture, PoseStack poseStack, MultiBufferSource bufferSource, int light, @Coerce Object side, int color, boolean glint,
            *///? } else {
            /*Player player, PoseStack poseStack, MultiBufferSource bufferSource, ArmorItem armorItem, ItemStack itemStack, int light, boolean isLeft,
            *///?}
    ) {
        //? if >= 1.21.9 {
        var interceptionResult = interceptArmor(state, EquipmentSlot.CHEST, state.chestEquipment, ci);
        if (!interceptionResult.getFirst()) {
            return;
        }
        AhRenderManagementApi.enterScope(interceptionResult.getSecond());
        //? } elif >= 1.21 {
        /*String playerName = AhRenderManagementApi.currentlyHandledPlayerName();
        if (playerName == null || playerName.isBlank()) return;
        var mod = SlotModification.of(playerName, EquipmentSlot.CHEST, null);
        if (mod.needsModification()) {
            AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, mod);
        }
        if (mod.shouldHide()) {
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
        *///? } else {
        /*if (!(player instanceof IdentityCarrier carrier)) return;
        var ctx = AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, carrier, EquipmentSlot.CHEST, itemStack);
        if (!ctx.isEmpty() && ctx.shouldCancel()) {
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
        *///?}
    }

    @Inject(method = BREAST_METHOD, at = @At("RETURN"))
    private void clearBreastArmorContext(
            //? if >= 1.21.9 {
            Identifier texture, PoseStack poseStack, SubmitNodeCollector collector, HumanoidRenderState state, BreastSide side, int color, MutableBoolean glint, MutableInt order, CallbackInfo ci
            //? } elif >= 1.21 {
            /*Identifier texture, PoseStack poseStack, MultiBufferSource bufferSource, int light, @Coerce Object side, int color, boolean glint,
            *///? } else {
            /*Player player, PoseStack poseStack, MultiBufferSource bufferSource, ArmorItem armorItem, ItemStack itemStack, int light, boolean isLeft,
            *///?}
    ) {
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }

    //? if >= 1.21 {
    @WrapOperation(
            method = BREAST_METHOD,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/ARGB;opaque(I)I",
                    remap = true)
    )
    private int modifyBreastArmorColor(int i, Operation<Integer> original) {
        int opaqueColor = original.call(i);
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(opaqueColor);
    }
    //?}

    //? if >= 1.21 && < 1.21.9 {
    /*@WrapOperation(
            method = BREAST_METHOD,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/FastColor$ARGB32;opaque(I)I",
                    remap = true)
    )
    private int modifyBreastArmorColorLegacy(int color, Operation<Integer> original) {
        int opaqueColor = original.call(color);
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(opaqueColor);
    }
    *///?}

    //? if >= 1.21.9 {
    @WrapOperation(
            method = BREAST_METHOD,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType modifyBreastArmorRenderType(Identifier texture, Operation<RenderType> original) {
        return AhRenderManagementApi
                .getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi()
                .renderTypes().getTranslucentArmorRenderType(texture);
    }
    //?}

    //? if < 1.21.9 {
    /*@WrapOperation(
            method = BREAST_METHOD,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType modifyBreastArmorRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (modApi.getTranslucentArmorRenderType(texture, original.call(texture)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(texture);
    }
    *///?}

    //? if < 1.21 {
    /*@ModifyExpressionValue(
            method = BREAST_METHOD,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z",
                    remap = true)
    )
    private boolean modifyGlint(boolean original) {
        var armorCtx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (!armorCtx.isEmpty() && (armorCtx.modification().shouldDisableGlint() || armorCtx.modification().shouldHide())) {
            return false;
        }
        return original;
    }
    *///?}

    // ========================
    // renderArmorTrim
    // ========================

    @Inject(method = TRIM_METHOD, at = @At("HEAD"), cancellable = true)
    private void interceptArmorTrim(
            //? if >= 1.21.9 {
            ResourceKey<EquipmentAsset> armorModel, PoseStack poseStack, SubmitNodeCollector collector, HumanoidRenderState state, ArmorTrim trim, BreastSide side, MutableInt order, CallbackInfo ci
            //? } elif >= 1.21 {
            /*@Coerce Object armorModel, PoseStack poseStack, MultiBufferSource bufferSource, int light, @Coerce Object trim, boolean glint, @Coerce Object side,
            *///? } else {
            /*ArmorMaterial material, PoseStack poseStack, MultiBufferSource bufferSource, int light, ArmorTrim trim, boolean glint, boolean isLeft,
            *///?}
    ) {
        //? if >= 1.21.9 {
        var interceptionResult = interceptArmor(state, EquipmentSlot.CHEST, state.chestEquipment, ci);
        if (!interceptionResult.getFirst()) {
            return;
        }
        AhRenderManagementApi.enterScope(interceptionResult.getSecond());
        //? } elif >= 1.21 {
        /*String playerName = AhRenderManagementApi.currentlyHandledPlayerName();
        if (playerName == null || playerName.isBlank()) return;
        var mod = SlotModification.of(playerName, EquipmentSlot.CHEST, null);
        if (mod.needsModification()) {
            AhRenderManagementApi.enterScope(RenderScope.ARMOR_PIECE, mod);
        }
        if (mod.shouldHide()) {
            AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
        *///? } else {
        /*var armorCtx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (!armorCtx.isEmpty() && armorCtx.modification().shouldHide()) {
            ci.cancel();
        }
        *///?}
    }

    @Inject(method = TRIM_METHOD, at = @At("RETURN"))
    private void clearArmorTrimContext(
            //? if >= 1.21.9 {
            ResourceKey<EquipmentAsset> armorModel, PoseStack poseStack, SubmitNodeCollector collector, HumanoidRenderState state, ArmorTrim trim, BreastSide side, MutableInt order, CallbackInfo ci
            //? } elif >= 1.21 {
            /*@Coerce Object armorModel, PoseStack poseStack, MultiBufferSource bufferSource, int light, @Coerce Object trim, boolean glint, @Coerce Object side,
            *///? } else {
            /*ArmorMaterial material, PoseStack poseStack, MultiBufferSource bufferSource, int light, ArmorTrim trim, boolean glint, boolean isLeft,
            *///?}
    ) {
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }

    //? if >= 1.21.9 {
    @WrapOperation(
            method = TRIM_METHOD,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType modifyTrimRenderType(boolean decal, Operation<RenderType> original) {
        return AhRenderManagementApi
                .getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi()
                .renderTypes().getTranslucentArmorTrimRenderType(decal);
    }
    //?}

    //? if < 1.21.9 {
    /*@WrapOperation(
            method = TRIM_METHOD,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    remap = true)
    )
    private RenderType modifyTrimRenderType(boolean decal, Operation<RenderType> original) {
        var trimModApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (trimModApi.getTrimRenderLayer(decal, original.call(decal)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(decal);
    }
    *///?}

    // ========================
    // renderGlint (era 3 / 1.21.9+ only)
    // ========================

    //? if >= 1.21.9 {
    @Inject(method = "renderGlint", at = @At("HEAD"), cancellable = true)
    private void interceptGlint(PoseStack poseStack, SubmitNodeCollector queue,
            HumanoidRenderState state, @Coerce Object box, CallbackInfo ci) {
        if (!(state instanceof IdentityCarrier carrier)) return;
        ItemStack chestItem = (state instanceof AvatarRenderState avatar) ? avatar.chestEquipment : null;
        var mod = carrier.getModification(EquipmentSlot.CHEST, chestItem);
        if (mod.shouldHide() || mod.shouldDisableGlint()) {
            ci.cancel();
        }
    }
    //?}
}
//?}
