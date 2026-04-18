package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import org.spongepowered.asm.mixin.Mixin;

//? if < 1.21.4 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.At;
*///?}

//? if >= 1.21 && < 1.21.4 {
/*import net.minecraft.client.renderer.Sheets;
*///?}

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerRenderMixin
//? if < 1.21.4
//<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
{
    //? if < 1.21.4 {
    /*@ModifyExpressionValue(
            method = "renderArmorPiece",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"
            )
    )
    private boolean modifyGlint(boolean original) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        var mod = ctx.activeModification();
        if (mod != null) {
            if (mod.shouldDisableGlint() || mod.shouldHide()) {
                return false;
            }
        }
        return original;
    }

    @WrapOperation(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original) {
        return RenderModifications.getTranslucentArmorRenderType(ArmorHiderClient.RENDER_CONTEXT, texture, original.call(texture));
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
        int modifiedColor = RenderModifications.applyArmorTransparency(ArmorHiderClient.RENDER_CONTEXT, color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original) {
        return RenderModifications.getTrimRenderLayer(ArmorHiderClient.RENDER_CONTEXT, decal, original.call(decal));
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private void modifyTrimColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original) {
        int modifiedColor = RenderModifications.applyArmorTransparency(ArmorHiderClient.RENDER_CONTEXT, packedOverlay);
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
        float modifiedAlpha = RenderModifications.getTransparencyAlpha(ArmorHiderClient.RENDER_CONTEXT);
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
        float modifiedAlpha = RenderModifications.getTransparencyAlpha(ArmorHiderClient.RENDER_CONTEXT);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }
    *///?}
}
