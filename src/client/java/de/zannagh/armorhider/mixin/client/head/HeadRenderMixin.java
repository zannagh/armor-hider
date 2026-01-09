package de.zannagh.armorhider.mixin.client.head;

import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeadFeatureRenderer.class)
public abstract class HeadRenderMixin<T extends LivingEntity, M extends EntityModel<T> & ModelWithHead> extends FeatureRenderer<T, M> {
    public HeadRenderMixin(FeatureRendererContext<T, M> context, EntityModelLoader loader, HeldItemRenderer heldItemRenderer) {
        super(context);
    }
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
    cancellable = true)
    private void grabSkullBlockRenderContext(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        ArmorRenderPipeline.setupContext(null, EquipmentSlot.HEAD, livingEntity);
        
        if (!ArmorRenderPipeline.hasActiveContext()) {
            return;
        }

        if (!ArmorRenderPipeline.shouldModifyEquipment()) {
            return;
        }
        
        if (ArmorRenderPipeline.renderStateDoesNotTargetPlayer(livingEntity)) {
            return;
        }

        if (ArmorRenderPipeline.shouldHideEquipment()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At("RETURN")
    )
    private void resetSkullBlockRenderContext(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }
}

