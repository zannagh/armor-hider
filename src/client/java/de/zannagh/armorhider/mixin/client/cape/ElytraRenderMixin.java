package de.zannagh.armorhider.mixin.client.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WingsLayer.class)
public class ElytraRenderMixin {
    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private <S extends HumanoidRenderState, M extends EntityModel<S>> void interceptElytraRender(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S humanoidRenderState, float f, float g, CallbackInfo ci){
        ArmorRenderPipeline.setupContext(null, net.minecraft.world.entity.EquipmentSlot.CHEST, humanoidRenderState);
        
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
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At(value = "RETURN")
    )
    private <S extends HumanoidRenderState, M extends EntityModel<S>> void releaseContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S humanoidRenderState, float f, float g, CallbackInfo ci){
        ArmorRenderPipeline.clearContext();
    }
}
