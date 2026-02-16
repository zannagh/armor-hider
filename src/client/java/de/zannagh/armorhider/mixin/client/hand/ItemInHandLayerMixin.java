package de.zannagh.armorhider.mixin.client.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

/// Sets up and tears down the ArmorRenderPipeline context for off-hand items
/// rendered in third person. Downstream mixins (ItemRenderStateMixin for regular
/// items, ModelPartSubmitMixin for shields/banners) apply the actual transparency.
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

        //? if >= 1.21.11
        ArmorRenderPipeline.setupContext(itemStack, EquipmentSlot.OFFHAND, humanoidState);
        //? if >= 1.21.4 && < 1.21.11
        //ArmorRenderPipeline.setupContext(null, EquipmentSlot.OFFHAND, humanoidState);
        //? if < 1.21.4
        //ArmorRenderPipeline.setupContext(itemState, EquipmentSlot.OFFHAND, humanoidState);

        if (ArmorRenderPipeline.hasActiveContext()
                && ArmorRenderPipeline.shouldModifyEquipment()
                && ArmorRenderPipeline.shouldHideEquipment()) {
            ArmorRenderPipeline.clearContext();
            ci.cancel();
        }
    }

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
            ArmorRenderPipeline.clearContext();
        }
    }
}
