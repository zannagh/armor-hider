package de.zannagh.armorhider.client.mixin.cape;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.ScopeHandover;
import de.zannagh.armorhider.client.api.render.AhRenderInterceptionApi;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.model.EntityModel;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.MultiBufferSource;

// Conditional imports
//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;
//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//? if <= 1.21.4 {

/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
*///?}
//? if <= 1.21.1 {

/*import net.minecraft.client.model.ElytraModel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
 *///?}

@Mixin(WingsLayer.class)
public class ElytraRenderMixin
    //? if >= 1.21.4
        <S extends net.minecraft.client.renderer.entity.state.HumanoidRenderState, M extends net.minecraft.client.model.EntityModel<S>>
    //? if < 1.21.4
        //<S extends LivingEntity, M extends EntityModel<S>>
    {

    @Unique
    @Final
    private final AhRenderInterceptionApi renderApi = ArmorHiderClientApi.getInstance().getRenderApi();


    //? if >= 1.21.9
    @Unique private static final String ENTRY_METHOD = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";
    
    //? if < 1.21.9 && >= 1.21.4
    //@Unique private static final String ENTRY_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";
        
    //? if < 1.21.4
    //@Unique private static final String ENTRY_METHOD = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V";
    
    @Inject(method = ENTRY_METHOD, at = @At(value = "HEAD"), cancellable = true)
    //? if >= 1.21.4 {
    private void interceptElytraRender(PoseStack poseStack,
                                       //? if >= 1.21.9
                                       SubmitNodeCollector submitNodeCollector,
                                       //? if < 1.21.9
                                       //MultiBufferSource multiBufferSource,
                                       int i, S humanoidRenderState, float f, float g, CallbackInfo ci,
                                       @Share(value = "scopeHandover") LocalRef<ScopeHandover> scopeHandover) {
    //? }
    //? if < 1.21.4 {
    /*private void interceptElytraRender(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, S humanoidRenderState, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci,
                                        @Share(value = "scopeHandover") LocalRef<ScopeHandover> scopeHandover) {
    *///?}
        var interceptionResult = renderApi.interceptRenderCallAndResolveCarrier(AhRenderInterceptionApi.InterceptionContext.PER_PLAYER_CAPTURE, humanoidRenderState, EquipmentSlot.CHEST, ItemsUtil.ELYTRA_ITEM_STACK, scopeHandover);
        if (!interceptionResult.shouldIntercept()) {
            return;
        }
        if (interceptionResult.carrier().isPlayerFlying()) {
            renderApi.releaseContext();
            return;
        }

        if (interceptionResult.shouldCancel()) {
            renderApi.wrapAndCancelRenderCall(ci);
            return;
        }
        if (ArmorHiderClient.ET_LOADED) {
            renderApi.releaseContext();
            // Suppress transparency on ElytraTrims by clearing the context, only allow full hide/show.
        }
    }

    @Inject(method = ENTRY_METHOD, at = @At(value = "RETURN"))
    //? if >= 1.21.4 {
    private void releaseContext(PoseStack poseStack,
                                //? if >= 1.21.9
                                SubmitNodeCollector submitNodeCollector,
                                //? if < 1.21.9
                                //MultiBufferSource multiBufferSource,
                                int i,
                                S humanoidRenderState, float f, float g, CallbackInfo ci) {
    //? }
    //? if < 1.21.4 {
    
    /*private void releaseContext(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, S humanoidRenderState, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
     *///?}
        renderApi.releaseContext();
    }
    
    // 1.21.4 has to render type swap and all that within this mixin as downstream mixins don't catch the rendering.
    //? if < 1.21.4 {
    
    /*@WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyElytraRenderType(Identifier texture, Operation<RenderType> original,
                                        @Share(value = "scopeHandover") LocalRef<ScopeHandover> scopeHandover) {
        return new RenderModifications(scopeHandover.get().modification()).getTranslucentArmorRenderType(texture, original.call(texture));
    }

    // --- Elytra transparency: color alpha modification ---
    // ElytraModel.renderToBuffer(PoseStack, VertexConsumer, int, int) is the 4-param final
    // method that delegates to the 5-param abstract renderToBuffer with default color 0xFFFFFFFF.
    // We intercept the 4-param call and redirect to the 5-param version with modified alpha.

    //? if >= 1.21 {
    @WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private void modifyElytraColor(ElytraModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original, @Share(value = "scopeHandover") LocalRef<ScopeHandover> scopeHandover) {
        int color = new RenderModifications(scopeHandover.get().modification()).applyArmorTransparency(0xFFFFFFFF);
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
    private void modifyElytraColor(ElytraModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Operation<Void> original, @Share(value = "scopeHandover") LocalRef<ScopeHandover> scopeHandover) {
        float modifiedAlpha = alpha * new RenderModifications(scopeHandover.get().modification()).getTransparencyAlpha();
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }
    ^///?}
     *///?}
}
