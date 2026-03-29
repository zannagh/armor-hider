package de.zannagh.armorhider.client.mixin.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if < 1.21.4 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
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
        if (!(renderState instanceof HumanoidRenderState humanoidState)) {
        //? if < 1.21.4
        //if (!(renderState instanceof Player humanoidState)) {
            return;
        }
        if (itemState.isEmpty()) {
            return;
        }

        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        //? if >= 1.21.11
        var mod = ActiveModification.forCarrier(humanoidState, EquipmentSlot.OFFHAND, itemStack);
        //? if >= 1.21.4 && < 1.21.11
        //var mod = ActiveModification.forCarrier(humanoidState, EquipmentSlot.OFFHAND, null);
        //? if < 1.21.4
        //var mod = ActiveModification.forCarrier(humanoidState, EquipmentSlot.OFFHAND, itemState);
        if (mod != null) { ctx.setActiveModification(mod); }

        if (mod != null && mod.shouldHide()) {
            ctx.clearActiveModification();
            ci.cancel();
        }
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
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND)
                && ctx.activeModification().transparency() < 1.0
                && ctx.activeModification().transparency() > 0) {
            float alpha = RenderModifications.getTransparencyAlpha(ctx);
            MultiBufferSource wrapped = RenderModifications.wrapTranslucentBufferSource(bufferSource, alpha);
            original.call(renderer, entity, itemStack, displayContext, isLeftHand, poseStack, wrapped, light);
        } else {
            original.call(renderer, entity, itemStack, displayContext, isLeftHand, poseStack, bufferSource, light);
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
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
        }
    }
}
