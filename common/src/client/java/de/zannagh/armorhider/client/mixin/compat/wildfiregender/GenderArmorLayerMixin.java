//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.common.RenderScope;

import de.zannagh.armorhider.client.common.IdentityCarrier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
//? }
//? if >= 1.21.9 && < 1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
*///?}

/**
 * Compatibility mixin for Wildfire's Female Gender Mod.
 * Applies chest armor hiding, transparency, and glint control
 * to the breast armor geometry rendered by {@code GenderArmorLayer}.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "com.wildfire.render.GenderArmorLayer", remap = false)
public class GenderArmorLayerMixin {

    // ========================
    // renderBreastArmor
    // ========================

    @Inject(method = "renderBreastArmor", at = @At("HEAD"), cancellable = true)
    private void interceptBreastArmor(Identifier texture, PoseStack poseStack,
            SubmitNodeCollector queue, HumanoidRenderState state,
            @Coerce Object side, int color, boolean glint, CallbackInfo ci) {
        if (!(state instanceof IdentityCarrier carrier)) return;
        ItemStack chestItem = (state instanceof AvatarRenderState avatar) ? avatar.chestEquipment : null;
        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var ctx = api.enterScope(RenderScope.ARMOR_PIECE, carrier, EquipmentSlot.CHEST, chestItem);
        if (!ctx.isEmpty() && ctx.shouldCancel()) {
            api.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
    }

    @Inject(method = "renderBreastArmor", at = @At("RETURN"))
    private void clearBreastArmorContext(Identifier texture, PoseStack poseStack,
            SubmitNodeCollector queue, HumanoidRenderState state,
            @Coerce Object side, int color, boolean glint, CallbackInfo ci) {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
    }

    @WrapOperation(
            method = "renderBreastArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/ARGB;opaque(I)I",
                    remap = true)
    )
    private int modifyBreastArmorColor(int color, Operation<Integer> original) {
        int opaqueColor = original.call(color);
        return ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(opaqueColor);
    }

    @WrapOperation(
            method = "renderBreastArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    //? if >= 1.21.9 && < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true)
    )
    //? if >= 1.21.11
    private RenderType modifyBreastArmorRenderType(Identifier texture, Operation<RenderType> original) {
    //? if >= 1.21.9 && < 1.21.11
    //private RenderType modifyBreastArmorRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (modApi.getTranslucentArmorRenderType(texture, original.call(texture)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(texture);
    }

    // ========================
    // renderArmorTrim
    // ========================

    @Inject(method = "renderArmorTrim", at = @At("HEAD"), cancellable = true)
    private void interceptArmorTrim(@Coerce Object armorModel, PoseStack poseStack,
            SubmitNodeCollector queue, HumanoidRenderState state,
            @Coerce Object trim, @Coerce Object side, boolean glint, CallbackInfo ci) {
        if (!(state instanceof IdentityCarrier carrier)) return;
        ItemStack chestItem = (state instanceof AvatarRenderState avatar) ? avatar.chestEquipment : null;
        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var ctx = api.enterScope(RenderScope.ARMOR_PIECE, carrier, EquipmentSlot.CHEST, chestItem);
        if (!ctx.isEmpty() && ctx.shouldCancel()) {
            api.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorTrim", at = @At("RETURN"))
    private void clearArmorTrimContext(@Coerce Object armorModel, PoseStack poseStack,
            SubmitNodeCollector queue, HumanoidRenderState state,
            @Coerce Object trim, @Coerce Object side, boolean glint, CallbackInfo ci) {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
    }

    @WrapOperation(
            method = "renderArmorTrim",
            require = 0,
            at = @At(value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    //? if >= 1.21.9 && < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true)
    )
    //? if >= 1.21.11
    private RenderType modifyTrimRenderType(boolean decal, Operation<RenderType> original) {
    //? if >= 1.21.9 && < 1.21.11
    //private RenderType modifyTrimRenderType(boolean decal, Operation<RenderType> original) {
        var trimModApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (trimModApi.getTrimRenderLayer(decal, original.call(decal)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(decal);
    }

    // ========================
    // renderGlint (Era 3 only)
    // ========================

    @Inject(method = "renderGlint", at = @At("HEAD"), cancellable = true)
    private void interceptGlint(PoseStack poseStack, SubmitNodeCollector queue,
            HumanoidRenderState state, @Coerce Object box, CallbackInfo ci) {
        if (!(state instanceof IdentityCarrier carrier)) return;
        ItemStack chestItem = (state instanceof AvatarRenderState avatar) ? avatar.chestEquipment : null;
        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var ctx = api.enterScope(RenderScope.ARMOR_PIECE, carrier, EquipmentSlot.CHEST, chestItem);
        if (ctx.shouldCancel() || ctx.modification().shouldDisableGlint()) {
            api.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
            return;
        }
        api.exitScope(RenderScope.ARMOR_PIECE);
    }
}
//?}

//? if >= 1.21 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.common.RenderScope;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wildfire.render.GenderArmorLayer", remap = false)
public class GenderArmorLayerMixin {

    // ========================
    // renderBreastArmor
    // ========================

    @Inject(method = "renderBreastArmor", at = @At("HEAD"), cancellable = true)
    private void interceptBreastArmor(Identifier texture, PoseStack poseStack,
            MultiBufferSource bufferSource, int light,
            @Coerce Object side, int color, boolean glint, CallbackInfo ci) {
        String playerName = ArmorHiderClientApi.getInstance().getRenderingScopeApi().currentlyHandledPlayerName();
        if (playerName == null || playerName.isBlank()) return;
        var mod = SlotModification.of(playerName, EquipmentSlot.CHEST, null);
        if (mod.needsModification()) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().setActiveModification(mod);
        }
        if (mod.shouldHide()) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
    }

    @Inject(method = "renderBreastArmor", at = @At("RETURN"))
    private void clearBreastArmorContext(Identifier texture, PoseStack poseStack,
            MultiBufferSource bufferSource, int light,
            @Coerce Object side, int color, boolean glint, CallbackInfo ci) {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
    }

    // Target ARGB.opaque (1.21.4+) — silently skipped on older versions via require = 0
    @WrapOperation(
            method = "renderBreastArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/ARGB;opaque(I)I",
                    remap = true)
    )
    private int modifyBreastArmorColor(int color, Operation<Integer> original) {
        int opaqueColor = original.call(color);
        return ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(opaqueColor);
    }

    // Target FastColor.ARGB32.opaque (1.21–1.21.1) — silently skipped on newer versions via require = 0
    @WrapOperation(
            method = "renderBreastArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/FastColor$ARGB32;opaque(I)I",
                    remap = true)
    )
    private int modifyBreastArmorColorLegacy(int color, Operation<Integer> original) {
        int opaqueColor = original.call(color);
        return ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(opaqueColor);
    }

    @WrapOperation(
            method = "renderBreastArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true)
    )
    private RenderType modifyBreastArmorRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (modApi.getTranslucentArmorRenderType(texture, original.call(texture)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(texture);
    }

    // ========================
    // renderArmorTrim
    // ========================

    @Inject(method = "renderArmorTrim", at = @At("HEAD"), cancellable = true)
    private void interceptArmorTrim(@Coerce Object armorModel, PoseStack poseStack,
            MultiBufferSource bufferSource, int light,
            @Coerce Object trim, boolean glint, @Coerce Object side, CallbackInfo ci) {
        String playerName = ArmorHiderClientApi.getInstance().getRenderingScopeApi().currentlyHandledPlayerName();
        if (playerName == null || playerName.isBlank()) return;
        var mod = SlotModification.of(playerName, EquipmentSlot.CHEST, null);
        if (mod.needsModification()) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().setActiveModification(mod);
        }
        if (mod.shouldHide()) {
            ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorTrim", at = @At("RETURN"))
    private void clearArmorTrimContext(@Coerce Object armorModel, PoseStack poseStack,
            MultiBufferSource bufferSource, int light,
            @Coerce Object trim, boolean glint, @Coerce Object side, CallbackInfo ci) {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
    }

    @WrapOperation(
            method = "renderArmorTrim",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true)
    )
    private RenderType modifyTrimRenderType(boolean decal, Operation<RenderType> original) {
        var trimModApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (trimModApi.getTrimRenderLayer(decal, original.call(decal)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(decal);
    }
}
*///?}

//? if < 1.21 {
/*package de.zannagh.armorhider.client.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.common.RenderScope;

import de.zannagh.armorhider.client.common.IdentityCarrier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Compat for Wildfire's Female Gender Mod on 1.20.x.
// On these versions the class is GenderLayer (not GenderArmorLayer)
// and armor rendering happens in renderVanillaLikeBreastArmor.
// No transparency support: Wildfire uses a custom renderBox with hardcoded alpha.
@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "com.wildfire.render.GenderLayer", remap = false)
public class GenderArmorLayerMixin {

    // ========================
    // renderVanillaLikeBreastArmor
    // ========================

    @Inject(method = "renderVanillaLikeBreastArmor", at = @At("HEAD"), cancellable = true)
    private void interceptBreastArmor(Player player, PoseStack poseStack,
            MultiBufferSource bufferSource, ArmorItem armorItem, ItemStack itemStack,
            int light, boolean isLeft, CallbackInfo ci) {
        if (!(player instanceof IdentityCarrier carrier)) return;
        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        var ctx = api.enterScope(RenderScope.ARMOR_PIECE, carrier, EquipmentSlot.CHEST, itemStack);
        if (!ctx.isEmpty() && ctx.shouldCancel()) {
            api.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
            return;
        }
    }

    @Inject(method = "renderVanillaLikeBreastArmor", at = @At("RETURN"))
    private void clearBreastArmorContext(Player player, PoseStack poseStack,
            MultiBufferSource bufferSource, ArmorItem armorItem, ItemStack itemStack,
            int light, boolean isLeft, CallbackInfo ci) {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
    }

    @WrapOperation(
            method = "renderVanillaLikeBreastArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true)
    )
    private RenderType modifyBreastArmorRenderType(Identifier texture, Operation<RenderType> original) {
        var modApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (modApi.getTranslucentArmorRenderType(texture, original.call(texture)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(texture);
    }

    @ModifyExpressionValue(
            method = "renderVanillaLikeBreastArmor",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z",
                    remap = true)
    )
    private boolean modifyGlint(boolean original) {
        var armorCtx = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE);
        if (!armorCtx.isEmpty() && (armorCtx.modification().shouldDisableGlint() || armorCtx.modification().shouldHide())) {
            return false;
        }
        return original;
    }

    // ========================
    // renderArmorTrim
    // ========================

    @Inject(method = "renderArmorTrim", at = @At("HEAD"), cancellable = true)
    private void interceptArmorTrim(ArmorMaterial material, PoseStack poseStack,
            MultiBufferSource bufferSource, int light,
            ArmorTrim trim, boolean glint, boolean isLeft, CallbackInfo ci) {
        var armorCtx = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE);
        if (!armorCtx.isEmpty() && armorCtx.modification().shouldHide()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorTrim", at = @At("RETURN"))
    private void clearArmorTrimContext(ArmorMaterial material, PoseStack poseStack,
            MultiBufferSource bufferSource, int light,
            ArmorTrim trim, boolean glint, boolean isLeft, CallbackInfo ci) {
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
    }

    @WrapOperation(
            method = "renderArmorTrim",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true)
    )
    private RenderType modifyTrimRenderType(boolean decal, Operation<RenderType> original) {
        var trimModApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi().getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (trimModApi.getTrimRenderLayer(decal, original.call(decal)) instanceof RenderType rt) {
            return rt;
        }
        return original.call(decal);
    }
}
*///?}
