package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkullBlockEntityRenderer.class)
public abstract class SkullBlockRenderMixin {
    @WrapOperation(
            method = "renderSkull",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/entity/SkullBlockEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"
            )
    )
    private static void modifyTransparency(SkullBlockEntityModel instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay, Operation<Void> original){

        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            if (!ArmorRenderPipeline.getCurrentModification().playerConfig().opacityAffectingHatOrSkull.getValue()) {
                original.call(instance, matrixStack, vertexConsumer, light, overlay);
                return;
            }
            
            var newColor = ArmorRenderPipeline.applyTransparency(-1);
            instance.render(matrixStack, vertexConsumer, light, overlay, newColor);
        } else {
            original.call(instance, matrixStack, vertexConsumer, light, overlay);
        }
    }

    @WrapOperation(
            method = "getRenderLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getEntityTranslucent(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private static RenderLayer modifySkullTransparency(Identifier texture, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getRenderLayer(texture, original.call(texture));
    }

    @WrapOperation(
            method = "getRenderLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getEntityCutoutNoCullZOffset(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private static RenderLayer getCutoutRenderLayer(Identifier texture, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getRenderLayer(texture, original.call(texture));
    }
}
