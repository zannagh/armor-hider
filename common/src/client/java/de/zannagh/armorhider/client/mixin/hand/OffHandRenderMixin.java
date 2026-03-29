package de.zannagh.armorhider.client.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.ActiveModification;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >= 1.21.9 {

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
 //? }
//? if < 1.21.9 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
*///? }


@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ItemInHandRenderer.class)
public class OffHandRenderMixin {

    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD")
    )
    //? if >= 1.21.9
    private void onRenderItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci){
    //? if < 1.21.9
    //private void onRenderItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, CallbackInfo ci){
        if (itemStack.is(Items.AIR)) {
            return;
        }
        if (interactionHand == InteractionHand.MAIN_HAND){
            return;
        }
        //? if >= 1.21.9
        String name = abstractClientPlayer.getGameProfile().name();
        //? if < 1.21.9
        //String name = abstractClientPlayer.getGameProfile().getName();
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        var mod = ActiveModification.create(name, EquipmentSlot.OFFHAND, itemStack);
        if (mod != null) { ctx.setActiveModification(mod); }
    }

    @WrapOperation(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.9
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
                    //? if >= 1.21.6 && < 1.21.9
                    //target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V"
                    //? if < 1.21.6 && != 1.21.5
                    //target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V"
                    //? if 1.21.5
                    //target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V"
            )
    )
    //? if >= 1.21.9
    private void modifyItemSubmit(ItemStackRenderState instance, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, int color, Operation<Void> original) {
    //? if >= 1.21.6 && < 1.21.9
    //private void modifyItemSubmit(ItemRenderer instance, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource multiBufferSource, Level level, int i, int j, int k, Operation<Void> original) {
    //? if < 1.21.6 && != 1.21.5
    //private void modifyItemSubmit(ItemRenderer instance, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean b, PoseStack poseStack, MultiBufferSource multiBufferSource, Level level, int i, int j, int k, Operation<Void> original) {
    //? if 1.21.5
    //private void modifyItemSubmit(ItemRenderer instance, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource multiBufferSource, Level level, int i, int j, int k, Operation<Void> original) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) && ctx.activeModification().shouldHide()) {
            return;
        }
        //? if >= 1.21.9
        original.call(instance, poseStack, submitNodeCollector, light, overlay, color);
        //? if < 1.21.9 {
        /*// Wrap buffer source for transparency: swap opaque render types to translucent
        MultiBufferSource source = multiBufferSource;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND)
                && ctx.activeModification().transparency() < 1.0
                && ctx.activeModification().transparency() > 0) {
            source = RenderModifications.wrapTranslucentBufferSource(multiBufferSource,
                    RenderModifications.getTransparencyAlpha(ctx));
        }
        *///? }
        //? if >= 1.21.6 && < 1.21.9
        //original.call(instance, livingEntity, itemStack, itemDisplayContext, poseStack, source, level, i, j, k);
        //? if < 1.21.6 && != 1.21.5
        //original.call(instance, livingEntity, itemStack, itemDisplayContext, b, poseStack, source, level, i, j, k);
        //? if 1.21.5
        //original.call(instance, livingEntity, itemStack, itemDisplayContext, poseStack, source, level, i, j, k);
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At("TAIL")
    )
    //? if >= 1.21.9
    private void releaseContext(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
    //? if < 1.21.9
    //private void releaseContext(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
}
