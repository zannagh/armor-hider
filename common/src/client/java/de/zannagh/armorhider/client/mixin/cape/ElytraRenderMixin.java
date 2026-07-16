package de.zannagh.armorhider.client.mixin.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;

//? if < 1.21.9
//import net.minecraft.client.renderer.MultiBufferSource;

//? if < 1.21.2 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
*///?}

@Mixin(WingsLayer.class)
public class ElytraRenderMixin
    //? if >= 1.21.2
        <S extends net.minecraft.client.renderer.entity.state.HumanoidRenderState, M extends net.minecraft.client.model.EntityModel<S>>
    //? if < 1.21.2
        //<S extends LivingEntity, M extends EntityModel<S>>
    {

    //? if >= 1.21.9
    @Unique private static final String ENTRY_METHOD = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";

    //? if < 1.21.9 && >= 1.21.2
    //@Unique private static final String ENTRY_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";

    //? if < 1.21.2
    //@Unique private static final String ENTRY_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V";

    @Inject(method = ENTRY_METHOD, at = @At(value = "HEAD"), cancellable = true)
    //? if >= 1.21.2 {
    private void interceptElytraRender(PoseStack poseStack,
                                       //? if >= 1.21.9
                                       SubmitNodeCollector submitNodeCollector,
                                       //? if < 1.21.9
                                       //MultiBufferSource multiBufferSource,
                                       int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
    //? }
    //? if < 1.21.2 {
    /*private void interceptElytraRender(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, S humanoidRenderState, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
    *///?}
        AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ELYTRA).interceptFrom(humanoidRenderState, ci);
    }

    @Inject(method = ENTRY_METHOD, at = @At(value = "RETURN"))
    private void releaseContext(CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.ELYTRA);
    }

    //? if < 1.21.2 {
    /*@WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType modifyElytraRenderType(Identifier texture, Operation<RenderType> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ELYTRA);
        var originalType = original.call(texture);
        if (ctx.isEmpty()) return originalType;
        if (ctx.renderModificationApi().getTranslucentArmorRenderType(texture, originalType) instanceof RenderType rt) {
            return rt;
        }
        return originalType;
    }

    //? if >= 1.21 {
    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private void modifyElytraColor(ElytraModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ELYTRA);
        if (ctx.isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay); return; }
        int color = ctx.renderModificationApi().applyArmorTransparency(0xFFFFFFFF);
        if (color != 0xFFFFFFFF) {
            model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        } else {
            original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay);
        }
    }
    //?}

    //? if < 1.21 {
    /^@WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            )
    )
    private void modifyElytraColor(ElytraModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ELYTRA);
        if (ctx.isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha); return; }
        float modifiedAlpha = alpha * ctx.renderModificationApi().getTransparencyAlpha();
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }
    ^///?}
    *///?}
}
