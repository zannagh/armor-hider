//? if >= 1.21 && < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderDecisions;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import de.zannagh.armorhider.client.scopes.ScopeFactory;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// NeoForge-specific armor rendering mixin for 1.21/1.21.1.
//
// NeoForge patches HumanoidArmorLayer in ways that break common mixins:
//
// 1) renderArmorPiece: NeoForge adds a 12-parameter overload with extra floats.
//    render() calls this overload directly, bypassing the vanilla 6-param version.
//
// 2) renderModel: NeoForge's non-typed overload takes Model (not HumanoidModel),
//    so invokevirtual targets Model.renderToBuffer instead of HumanoidModel.renderToBuffer.
//
// 3) renderTrim: Same Model vs HumanoidModel owner mismatch for renderToBuffer.
//
// This mixin is self-contained: scope setup, render type change, and color modification.
@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(HumanoidArmorLayer.class)
public class NeoForgeHumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {

    // --- Scope setup: target NeoForge 12-param renderArmorPiece ---

    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ItemStack itemStack = entity.getItemBySlot(slot);
        if (itemStack.is(Items.AIR)) {
            return;
        }

        var scopes = ArmorHiderClient.SCOPE_PROVIDER;
        var scope = ScopeFactory.createItemScope(scopes, itemStack, slot, entity);
        if (scope != null) {
            scopes.enterItemRender(scope);
        }

        if (RenderDecisions.shouldCancelRender(scopes)) {
            scopes.exitItemRender();
            ci.cancel();
        }
    }

    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("RETURN")
    )
    private void onRenderArmorPieceReturn(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ArmorHiderClient.SCOPE_PROVIDER.exitItemRender();
    }

    // --- Render type: swap armorCutoutNoCull → translucent in renderModel ---

    @WrapOperation(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(ResourceLocation texture, Operation<RenderType> original) {
        return RenderModifications.getTranslucentArmorRenderType(ArmorHiderClient.SCOPE_PROVIDER, texture, original.call(texture));
    }

    // --- Armor color: target Model.renderToBuffer in renderModel ---

    @WrapOperation(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            )
    )
    private void modifyArmorColor(Model model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color, Operation<Void> original) {
        int modifiedColor = RenderModifications.applyArmorTransparency(ArmorHiderClient.SCOPE_PROVIDER, color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    // --- Trim color: target Model.renderToBuffer in renderTrim ---

    @WrapOperation(
            method = "renderTrim(Lnet/minecraft/core/Holder;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/Model;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            ),
            require = 0
    )
    private void modifyTrimColor(Model model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original) {
        int modifiedColor = RenderModifications.applyArmorTransparency(ArmorHiderClient.SCOPE_PROVIDER, packedOverlay);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }
}
*///?}
