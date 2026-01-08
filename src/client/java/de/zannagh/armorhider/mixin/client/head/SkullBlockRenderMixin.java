package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkullBlockEntityRenderer.class)
public abstract class SkullBlockRenderMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/util/math/Direction;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/block/entity/SkullBlockEntityModel;Lnet/minecraft/client/render/RenderLayer;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
            )
    )
    private static <S> void modifyTransparency(OrderedRenderCommandQueue instance, Model<? super S> model, S o, MatrixStack matrixStack, RenderLayer renderLayer, int light, int overlay, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand, Operation<Void> original){
        
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            if (!ArmorRenderPipeline.getCurrentModification().playerConfig().opacityAffectingHatOrSkull.getValue()) {
                original.call(instance, model, o, matrixStack, renderLayer, light, overlay, outlineColor, crumblingOverlayCommand);
                return;
            }
            var newColor = ArmorRenderPipeline.applyTransparency(-1);
            instance.getBatchingQueue(ArmorRenderPipeline.SkullRenderPriority).submitModel(model, o, matrixStack, renderLayer, light, overlay, newColor , null, outlineColor, crumblingOverlayCommand);
        } else {
            original.call(instance, model, o, matrixStack, renderLayer, light, overlay, outlineColor, crumblingOverlayCommand);
        }
    }

    @WrapOperation(
            method = "renderSkull",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/render/block/entity/SkullBlockEntityRenderer.getCutoutRenderLayer(Lnet/minecraft/block/SkullBlock$SkullType;Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private static RenderLayer modifySkullTransparency(SkullBlock.SkullType type, Identifier texture, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getRenderLayer(texture, original.call(type, texture));
    }

    @WrapOperation(
            method = "getCutoutRenderLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayers;entityCutoutNoCullZOffset(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private static RenderLayer getCutoutRenderLayer(Identifier texture, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getRenderLayer(texture, original.call(texture));
    }
}
