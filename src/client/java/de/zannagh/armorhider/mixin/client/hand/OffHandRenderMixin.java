//? if >= 1.21.11 {

package de.zannagh.armorhider.mixin.client.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ItemInHandRenderer.class)
public class OffHandRenderMixin {

    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD")
    )
    private void onRenderItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci){
        if (itemStack.is(Items.AIR)) {
            return;
        }
        if (interactionHand == InteractionHand.MAIN_HAND){
            return;
        }
        ArmorRenderPipeline.setupContext(itemStack, EquipmentSlot.OFFHAND, abstractClientPlayer.getGameProfile());
    }

    @WrapOperation(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void modifyItemSubmit(ItemStackRenderState instance, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, int color, Operation<Void> original) {
        if (ArmorRenderPipeline.hasActiveContext()
                && ArmorRenderPipeline.shouldModifyEquipment()
                && ArmorRenderPipeline.shouldHideEquipment()) {
            return;
        }
        // Partial opacity is handled downstream by ItemRenderMixin
        // which modifies tint layers and render types at the submitItem level.
        original.call(instance, poseStack, submitNodeCollector, light, overlay, color);
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At("TAIL")
    )
    private void releaseContext(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }
}
//?}
