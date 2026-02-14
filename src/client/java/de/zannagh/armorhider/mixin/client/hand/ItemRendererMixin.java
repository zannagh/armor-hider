//? if < 1.21.9 {
/*package de.zannagh.armorhider.mixin.client.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    
    @WrapOperation(
            method = "renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V"
            )
    )
    private void wrapRenderStatic(ItemRenderer instance, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean b, PoseStack poseStack, MultiBufferSource multiBufferSource, Level level, int light, int overlay, int color, Operation<Void> original) {
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            // TODO: Figure out how to inject a translucent layer as the ItemRenderStateMixin does it for 1.21.9 and above
            return;
        } 
        original.call(instance, livingEntity, itemStack, itemDisplayContext, b, poseStack, multiBufferSource, level, light, overlay, color);
    }
}
*///? }
