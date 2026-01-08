package de.zannagh.armorhider.mixin.client.cape;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraRenderMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void interceptElytraRender(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci){
        ArmorRenderPipeline.setupContext(null, EquipmentSlot.CHEST, livingEntity);
        
        if (!ArmorRenderPipeline.hasActiveContext() || !ArmorRenderPipeline.shouldModifyEquipment()) {
            ArmorRenderPipeline.clearContext();
            return;
        }

        if (!ArmorRenderPipeline.getCurrentModification().playerConfig().opacityAffectingElytra.getValue()) {
            ArmorRenderPipeline.clearContext();
            return;
        }

        if (ArmorRenderPipeline.shouldHideEquipment()) {
            ArmorRenderPipeline.clearContext();
            if (ci != null) {
                ci.cancel();
            }
            return;
        }

        ArmorRenderPipeline.clearContext();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "RETURN")
    )
    private void releaseContext(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci){
        ArmorRenderPipeline.clearContext();
    }
}
