package de.zannagh.armorhider.client.mixin.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if < 1.21.4 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.render.RenderModifications;
import net.minecraft.client.renderer.ItemInHandRenderer;
*///? }

//? if >= 1.21.4 && < 1.21.9 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.render.RenderModifications;
*///? }

//? if >= 1.21.4 {
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
//? }
//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;
//? if >= 1.21.4 && < 1.21.9
//import net.minecraft.client.renderer.MultiBufferSource;
//? if < 1.21.4 {
/*import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
*///? }

/// Sets up and tears down the item scope for off-hand items rendered in third person.
/// Downstream mixins (ItemRenderStateMixin for regular items, SubmitNodeCollectorMixin
/// for shields/banners) apply the actual transparency.
@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    //? if >= 1.21.9
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    //? if < 1.21.9
    //@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.11
    private void setupOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
    //? if 1.21.9 || 1.21.10
    //private void setupOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
    //? if >= 1.21.4 && < 1.21.9
    //private void setupOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, HumanoidArm arm, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
    //? if < 1.21.4
    //private void setupOffhandContext(LivingEntity renderState, ItemStack itemState, ItemDisplayContext itemDisplayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        //? if >= 1.21.4
        if (arm == renderState.mainArm) return;
        //? if < 1.21.4
        //if (arm == renderState.getMainArm()) return;
        //? if >= 1.21.4
        if (!(renderState instanceof HumanoidRenderState humanoidState)) return;
        //? if < 1.21.4
        //if (!(renderState instanceof Player humanoidState)) return;
        if (itemState.isEmpty()) return;
        if (!(humanoidState instanceof IdentityCarrier carrier)) return;

        
        //? if >= 1.21.11
        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.OFFHAND).intercept(carrier, null, itemStack, ci);
        //? if >= 1.21.4 && < 1.21.11
        //var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.OFFHAND).interceptFrom(carrier, ci);
        //? if < 1.21.4
        //var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.OFFHAND).intercept(carrier, null, itemState, ci);

        if (result.shouldCancel() || !result.shouldIntercept()) return;
        AhRenderManagementApi.enterScope(result);
    }

    //? if < 1.21.4 {
    /*@WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void wrapOffhandRenderItem(ItemInHandRenderer renderer, LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext, boolean isLeftHand, PoseStack poseStack, MultiBufferSource bufferSource, int light, Operation<Void> original) {
        var offhandCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (!offhandCtx.isEmpty()
                && offhandCtx.modification().transparency() < 1.0
                && offhandCtx.modification().transparency() > 0) {
            float alpha = offhandCtx.renderModificationApi().getTransparencyAlpha();
            MultiBufferSource wrapped = RenderModifications.wrapTranslucentBufferSource(bufferSource, alpha);
            original.call(renderer, entity, itemStack, displayContext, isLeftHand, poseStack, wrapped, light);
        } else {
            original.call(renderer, entity, itemStack, displayContext, isLeftHand, poseStack, bufferSource, light);
        }
    }
    *///? }

    // Third-person offhand: swap the buffer source to translucent equivalents while the OFFHAND
    // scope is active. Without this the item keeps its opaque render type, so the vertex-alpha
    // applied by ItemRendererMixin is ignored (item stays fully opaque / whitens). First person
    // does the equivalent wrap in OffHandRenderMixin. 1.21.9+ uses the submit-based mixins instead.
    //? if >= 1.21.4 && < 1.21.9 {
    /*@WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
            )
    )
    private void wrapOffhandBufferSource(ItemStackRenderState itemState, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, Operation<Void> original) {
        var offhandCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (!offhandCtx.isEmpty()
                && offhandCtx.modification().transparency() < 1.0
                && offhandCtx.modification().transparency() > 0) {
            float alpha = offhandCtx.renderModificationApi().getTransparencyAlpha();
            MultiBufferSource wrapped = RenderModifications.wrapTranslucentBufferSource(bufferSource, alpha);
            original.call(itemState, poseStack, wrapped, light, overlay);
        } else {
            original.call(itemState, poseStack, bufferSource, light, overlay);
        }
    }
    *///? }

    //? if >= 1.21.9
    @Inject(method = "submitArmWithItem", at = @At("TAIL"))
    //? if < 1.21.9
    //@Inject(method = "renderArmWithItem", at = @At("TAIL"))
    //? if >= 1.21.11
    private void clearOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, ItemStack itemStack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
    //? if 1.21.9 || 1.21.10
    //private void clearOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
    //? if >= 1.21.4 && < 1.21.9
    //private void clearOffhandContext(ArmedEntityRenderState renderState, ItemStackRenderState itemState, HumanoidArm arm, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
    //? if < 1.21.4
    //private void clearOffhandContext(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        //? if >= 1.21.4
        if (arm != renderState.mainArm) {
        //? if < 1.21.4
        //if (arm != livingEntity.getMainArm()) {
            AhRenderManagementApi.exitScope(RenderScope.OFFHAND);
        }
    }
}
