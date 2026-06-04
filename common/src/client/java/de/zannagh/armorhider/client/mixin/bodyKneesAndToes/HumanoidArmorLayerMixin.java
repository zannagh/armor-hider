package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.*;
import de.zannagh.armorhider.client.api.render.AhRenderInterceptionApi;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;

//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

//? if >= 1.21.4 && < 1.21.9 {
/*import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if < 1.21.4 {
/*import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
*///?}

//? if < 1.21.4 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import de.zannagh.armorhider.client.rendering.VanillaArmorTextureManager;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.At;
*///?}

//? if >= 1.21 && < 1.21.4 {
/*import net.minecraft.client.renderer.Sheets;
 *///?}

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin
//? if < 1.21.4
//<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
{
    @Unique
    @Final
    private final AhRenderInterceptionApi renderApi = ArmorHiderClientApi.getInstance().getRenderApi();

    // ===== Player/state capture + early-out (render HEAD) =====

    //? if < 1.21.9 {
    /*
    //? if >= 1.21.4 && < 1.21.9 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S entity, float f, float g, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
    //? }
    //? if >= 1.21 && < 1.21.4 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void capturePlayerName(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float f1, float f2, float f3, float f4, float f5, float f6, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
    //? }
    //? if < 1.21 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipIfAllHidden(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float f1, float f2, float f3, float f4, float f5, float f6, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
    //?}

        var result = renderApi.interceptRenderCall(AhRenderInterceptionApi.InterceptionContext.PER_PLAYER_CAPTURE, entity, identityCarrier);
        if (result.shouldCancel()) {
            renderApi.wrapAndCancelRenderCall(ci);
        }
    }
    *///?}

    // ===== Per-piece scope setup (renderArmorPiece HEAD) =====

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.9
    private <S extends HumanoidRenderState> void captureContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int i, S humanoidRenderState, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
    //? if >= 1.21.4 && < 1.21.9
    //private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot slot, int i, HumanoidModel<?> humanoidRenderState, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
    //? if < 1.21.4
    //private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T humanoidRenderState, EquipmentSlot slot, int packedLight, A itemStack, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
        var interceptionResult = renderApi.interceptRenderCall(AhRenderInterceptionApi.InterceptionContext.PER_PIECE_LAYER, renderApi.tryGetIdentityCarrierFromLocalRef(humanoidRenderState, identityCarrier), slot,
                //? if >= 1.21.4
                itemStack,
                //? if < 1.21.4
                //null,
                identityCarrier);
        if (!interceptionResult.shouldIntercept()) {
            return;
        }
        if (interceptionResult.shouldCancel()) {
            renderApi.wrapAndCancelRenderCall(ci);
        }
    }

    // ===== Per-piece scope cleanup (renderArmorPiece RETURN) =====
    // In 1.21.4–1.21.8 this is handled by EquipmentRenderMixin.resetContext at the renderLayers level.

    //? if >= 1.21.9 || < 1.21.4 {
    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    //? if >= 1.21.9
    private <S extends HumanoidRenderState> void onRenderArmorPieceReturn(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
    //? if < 1.21.4
    //private void onRenderArmorPieceReturn(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<ScopeHandover> identityCarrier) {
        identityCarrier.set(null);
        renderApi.releaseContext();
    }
    //?}

    // === Render changes where MC does handle it within HumanoidArmorLayer ===

    //? if < 1.21.4 {
    /*@ModifyExpressionValue(
            method = "renderArmorPiece",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"
            )
    )
    private boolean modifyGlint(boolean original, @Share(value = "identityCarrier") LocalRef<ScopeHandover> scopeHandover) {
        var mod = scopeHandover.get() == null ? SlotModification.empty() : scopeHandover.get().modification();
        if (mod.shouldDisableGlint() || mod.shouldHide()) {
            return false;
        }
        return original;
    }

    @WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original, @Share(value = "identityCarrier") LocalRef<ScopeHandover> scopeHandover) {
        Identifier resolved = VanillaArmorTextureManager.resolveArmorTexture(scopeHandover.get().modification(), texture);
        return new RenderModifications(scopeHandover.get().modification()).getTranslucentArmorRenderType(resolved, original.call(resolved));
    }
    *///?}

    //? if >= 1.21 && < 1.21.4 {
    /*@WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            )
    )
    private void modifyArmorColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color, Operation<Void> original, @Share(value = "identityCarrier") LocalRef<ScopeHandover> scopeHandover) {
        int modifiedColor = new RenderModifications(scopeHandover.get().modification()).applyArmorTransparency(color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original, @Share(value = "identityCarrier") LocalRef<ScopeHandover> scopeHandover) {
        return new RenderModifications(scopeHandover.get().modification()).getTrimRenderLayer(decal, original.call(decal));
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private void modifyTrimColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original, @Share(value = "identityCarrier") LocalRef<ScopeHandover> scopeHandover) {
        int modifiedColor = new RenderModifications(scopeHandover.get().modification()).applyTransparencyFromWhite(packedOverlay);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }
    *///?}

    //? if < 1.21 {
    /*@WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            )
    )
    private void modifyArmorColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float v, Operation<Void> original, @Share(value = "identityCarrier") LocalRef<ScopeHandover> scopeHandover) {
        float modifiedAlpha = new RenderModifications(scopeHandover.get().modification()).getTransparencyAlpha();
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            require = 0
    )
    private void modifyTrimColor(HumanoidModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Operation<Void> original, @Share(value = "identityCarrier") LocalRef<ScopeHandover> scopeHandover) {
        float modifiedAlpha = new RenderModifications(scopeHandover.get().modification()).getTransparencyAlpha();
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }
    *///?}
}
