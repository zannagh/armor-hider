package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {
    
    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private <S extends LivingEntityRenderState> void interceptHeadLayerRender(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci){
        if (livingEntityRenderState instanceof HumanoidRenderState humanoidState) {
            ArmorRenderPipeline.setupContext(null, net.minecraft.world.entity.EquipmentSlot.HEAD, humanoidState);
        }
        
    }

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private <S extends LivingEntityRenderState> void releaseContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci){
        ArmorRenderPipeline.clearContext();
    }

    @Inject(
            method = "resolveSkullRenderType",
            at = @At("HEAD")
    )
    private void grabSkullRenderContext(LivingEntityRenderState livingEntityRenderState, SkullBlock.Type type, CallbackInfoReturnable<RenderType> cir){
        if (livingEntityRenderState instanceof HumanoidRenderState humanoidState) {
            ArmorRenderPipeline.setupContext(null, net.minecraft.world.entity.EquipmentSlot.HEAD, humanoidState);
        }
    }
}
