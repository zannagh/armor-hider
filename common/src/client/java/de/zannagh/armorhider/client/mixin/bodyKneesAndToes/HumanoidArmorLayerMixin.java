package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;

//? if >= 1.21.4
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

//? if >= 1.21.4 && < 1.21.9 {
/*import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if < 1.21.4 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.render.VanillaArmorTextureManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
*///?}

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin
//? if < 1.21.4
//<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
{

    // ===== Per-piece scope setup (renderArmorPiece HEAD) =====

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.9
    private <S extends HumanoidRenderState> void captureContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int i, S humanoidRenderState, CallbackInfo ci) {
    //? if >= 1.21.4 && < 1.21.9
    //private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot slot, int i, HumanoidModel<?> humanoidRenderState, CallbackInfo ci) {
    //? if < 1.21.4
    //private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T humanoidRenderState, EquipmentSlot slot, int packedLight, A itemStack, CallbackInfo ci) {

        IdentityCarrier carrier = humanoidRenderState instanceof IdentityCarrier ic ? ic : null;
        if (carrier == null) return;

        
        //? if >= 1.21.4
        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ARMOR_PIECE).intercept(carrier, slot, itemStack, ci);
        //? if < 1.21.4
        //var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ARMOR_PIECE).intercept(carrier, slot, null, ci);
        if (result.shouldCancel() || !result.shouldIntercept()) return;
        AhRenderManagementApi.enterScope(result);
    }

    // ===== Per-piece scope cleanup (renderArmorPiece RETURN) =====
    // In 1.21.4–1.21.8 this is handled by EquipmentRenderMixin.resetContext at the renderLayers level.

    //? if >= 1.21.9 || < 1.21.4 {
    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void onRenderArmorPieceReturn(CallbackInfo ci) {
        AhRenderManagementApi.exitScope(RenderScope.ARMOR_PIECE);
    }
    //?}

    // === Render changes where MC does handle it within HumanoidArmorLayer ===

    //? if < 1.21.4 {
    /*@ModifyExpressionValue(
            method = "renderArmorPiece",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"
            )
    )
    private boolean modifyGlint(boolean original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        return ctx.renderModificationApi().getHasFoil(original);
    }

    @WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) return original.call(texture);
        Identifier resolved = VanillaArmorTextureManager.resolveArmorTexture(ctx.modification(), texture);
        var originalType = original.call(resolved);
        if (ctx.renderModificationApi().getTranslucentArmorRenderType(resolved, originalType) instanceof RenderType rt) {
            return rt;
        }
        return originalType;
    }
    *///?}

    //? if >= 1.21 && < 1.21.4 {
    /*@WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            )
    )
    private void modifyArmorColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, color); return; }
        int modifiedColor = ctx.renderModificationApi().applyArmorTransparency(color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) return original.call(decal);
        var originalType = original.call(decal);
        if (ctx.renderModificationApi().getTrimRenderLayer(decal, originalType) instanceof RenderType rt) {
            return rt;
        }
        return originalType;
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private void modifyTrimColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay); return; }
        int modifiedColor = ctx.renderModificationApi().applyTransparencyFromWhite(packedOverlay);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }
    *///?}

    //? if < 1.21 {
    /*@WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            )
    )
    private void modifyArmorColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float v, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, v); return; }
        float modifiedAlpha = ctx.renderModificationApi().getTransparencyAlpha();
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            require = 0
    )
    private void modifyTrimColor(HumanoidModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha); return; }
        float modifiedAlpha = ctx.renderModificationApi().getTransparencyAlpha();
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }
    *///?}
}
