package de.zannagh.armorhider.client.mixin.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.util.ItemsUtil;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Conditional imports
//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;
//? if >= 1.21.4

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
                                       int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
    //? }
    //? if < 1.21.4 {
    /*private void interceptElytraRender(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, S humanoidRenderState, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
    *///?}
        if (!(humanoidRenderState instanceof IdentityCarrier carrier)) {
            return;
        }
        
        if (carrier.isPlayerFlying()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            return;
        }
        
        var mod = carrier.createModificationAndSetContext(EquipmentSlot.CHEST, ItemsUtil.ELYTRA_ITEM_STACK);

        // 1.21.4 and above has the elytra handled in the equipment render mixins.
        
        if (mod == null) {
            return;
        }

        if (mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
            return;
        }
        if (ArmorHiderClient.ET_LOADED) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
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
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
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
    private RenderType modifyElytraRenderType(Identifier texture, Operation<RenderType> original) {
        return RenderModifications.getTranslucentArmorRenderType(ArmorHiderClient.RENDER_CONTEXT, texture, original.call(texture));
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
    private void modifyElytraColor(ElytraModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original) {
        int color = RenderModifications.applyArmorTransparency(ArmorHiderClient.RENDER_CONTEXT, 0xFFFFFFFF);
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
        float modifiedAlpha = alpha * RenderModifications.getTransparencyAlpha(ArmorHiderClient.RENDER_CONTEXT);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }
    ^///?}
     *///?}
}
