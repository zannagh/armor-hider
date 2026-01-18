package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRenderMixin {
    @WrapOperation(
            method = "submitSkull",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private static <S> void modifyTransparency(SubmitNodeCollector instance, Model<? super S> model, S o, PoseStack poseStack, RenderType renderType, int i, int j, int k, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original){
        try {
            if (!ArmorRenderPipeline.hasActiveContext() || !ArmorRenderPipeline.shouldModifyEquipment()) {
                original.call(instance, model, o, poseStack, renderType, i, j, k, crumblingOverlay);
                return;
            }

            if (!ArmorRenderPipeline.getCurrentModification().playerConfig().opacityAffectingHatOrSkull.getValue()) {
                original.call(instance, model, o, poseStack, renderType, i, j, k, crumblingOverlay);
                return;
            }

            double transparency = ArmorRenderPipeline.getCurrentModification().getTransparency();

            // Fully hidden - skip rendering entirely
            if (transparency <= 0) {
                return;
            }

            // Fully visible - render normally
            if (transparency >= 1.0) {
                original.call(instance, model, o, poseStack, renderType, i, j, k, crumblingOverlay);
                return;
            }

            // Semi-transparent - apply alpha to color
            int alpha = (int) (transparency * 255);
            var modifiedColor = ARGB.color(alpha, ARGB.red(k), ARGB.green(k), ARGB.blue(k));
            original.call(instance, model, o, poseStack, renderType, i, j, modifiedColor, crumblingOverlay);
        } finally {
            ArmorRenderPipeline.clearContext();
        }
    }
    
    @Inject(
            method = "resolveSkullRenderType",
            at = @At("HEAD")
    )
    private void grabSkullRenderContext(SkullBlock.Type type, SkullBlockEntity skullBlockEntity, CallbackInfoReturnable<RenderType> cir){
        if (skullBlockEntity.getOwnerProfile() != null) {
            ArmorRenderPipeline.setupContext(net.minecraft.world.entity.EquipmentSlot.HEAD, skullBlockEntity.getOwnerProfile().partialProfile());
        }
    }

    @WrapOperation(
            method = "resolveSkullRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;getSkullRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private static RenderType modifySkullTransparency(SkullBlock.Type type, Identifier identifier, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(identifier, original.call(type, identifier));
    }

    @WrapOperation(
            method = "getSkullRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutoutNoCullZOffset(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private static RenderType getCutoutRenderLayer(Identifier texture, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(texture, original.call(texture));
    }
}
