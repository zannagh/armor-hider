package de.zannagh.armorhider.client.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
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
import de.zannagh.armorhider.client.render.RenderModifications;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
*///? }


@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ItemInHandRenderer.class)
public class OffHandRenderMixin {

    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            cancellable = true
    )
    //? if >= 1.21.9
    private void onRenderItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci){
    //? if < 1.21.9
    //private void onRenderItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, CallbackInfo ci){

        EquipmentSlot slot = EquipmentSlot.OFFHAND;
        if (interactionHand == InteractionHand.MAIN_HAND) {
            slot = EquipmentSlot.MAINHAND;
        }

        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.OFFHAND).intercept(abstractClientPlayer, slot, itemStack, ci);
        if (result.shouldCancel() || !result.shouldIntercept()) {
            return;
        }
        AhRenderManagementApi.enterScope(result);
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

        var offhandCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (!offhandCtx.isEmpty() && offhandCtx.shouldCancel()) {
            return;
        }
        //? if >= 1.21.9
        original.call(instance, poseStack, submitNodeCollector, light, overlay, color);
        //? if < 1.21.9 {
        /*// Wrap buffer source for transparency: swap opaque render types to translucent
        MultiBufferSource source = multiBufferSource;
        if (!offhandCtx.isEmpty()
                && offhandCtx.modification().transparency() < 1.0
                && offhandCtx.modification().transparency() > 0) {
            source = RenderModifications.wrapTranslucentBufferSource(multiBufferSource,
                    offhandCtx.renderModificationApi().getTransparencyAlpha());
        }
        *///? }
        //? if >= 1.21.6 && < 1.21.9
        //original.call(instance, livingEntity, itemStack, itemDisplayContext, poseStack, source, level, i, j, k);
        //? if < 1.21.6 && != 1.21.5
        //original.call(instance, livingEntity, itemStack, itemDisplayContext, b, poseStack, source, level, i, j, k);
        //? if 1.21.5
        //original.call(instance, livingEntity, itemStack, itemDisplayContext, poseStack, source, level, i, j, k);
    }

    @Inject(method = "renderArmWithItem",
            at = @At("TAIL"))
    private void releaseContext(CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.OFFHAND);
    }
}
