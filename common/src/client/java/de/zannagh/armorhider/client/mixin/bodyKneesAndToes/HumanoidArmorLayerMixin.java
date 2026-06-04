package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.sugar.Share;import com.llamalad7.mixinextras.sugar.ref.LocalRef;import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.render.AhRenderInterceptionApi;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;import org.spongepowered.asm.mixin.Mixin;
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
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S entity, float f, float g, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
    //? }
    //? if >= 1.21 && < 1.21.4 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void capturePlayerName(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float f1, float f2, float f3, float f4, float f5, float f6, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
    //? }
    //? if < 1.21 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipIfAllHidden(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float f1, float f2, float f3, float f4, float f5, float f6, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
    //?}

        var result = renderApi.interceptRenderCall(AhRenderInterceptionApi.InterceptionContext.PER_PLAYER_CAPTURE, entity);
        if (result.shouldCancel()) {
            renderApi.wrapAndCancelRenderCall(ci);
        }
    }
    *///?}

    // ===== Per-piece scope setup (renderArmorPiece HEAD) =====

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.9
    private <S extends HumanoidRenderState> void captureContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int i, S humanoidRenderState, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
    //? if >= 1.21.4 && < 1.21.9
    //private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot slot, int i, HumanoidModel<?> humanoidRenderState, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
    //? if < 1.21.4
    //private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T humanoidRenderState, EquipmentSlot slot, int packedLight, A itemStack, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
        var handover = identityCarrier.get() == null ? humanoidRenderState : identityCarrier.get();
        var interceptionResult = renderApi.interceptRenderCall(AhRenderInterceptionApi.InterceptionContext.PER_PIECE_LAYER, handover, slot, itemStack);
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
    private <S extends HumanoidRenderState> void onRenderArmorPieceReturn(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
    //? if < 1.21.4
    //private void onRenderArmorPieceReturn(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, CallbackInfo ci, @Share(value = "identityCarrier") LocalRef<IdentityCarrier> identityCarrier) {
        identityCarrier.set(null);
        renderApi.releaseContext();
    }
    //?}
}
