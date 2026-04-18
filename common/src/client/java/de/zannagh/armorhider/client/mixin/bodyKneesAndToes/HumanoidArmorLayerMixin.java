package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
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
/*import de.zannagh.armorhider.client.scopes.ActiveModification;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if < 1.21.4 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
*///?}

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin
//? if < 1.21.4
//<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
{
    // ===== Player/state capture (render HEAD) =====

    //? if >= 1.21.4 && < 1.21.9 {
    /*@Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD")
    )
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        if (humanoidRenderState instanceof IdentityCarrier carrier) {
            ArmorHiderClient.RENDER_CONTEXT.setCurrentPlayer(carrier.armorHider$playerName());
        }
    }
    *///?}

    //? if >= 1.21 && < 1.21.4 {
    /*@Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD")
    )
    private void capturePlayerName(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float f1, float f2, float f3, float f4, float f5, float f6, CallbackInfo ci) {
        if (entity instanceof IdentityCarrier carrier) {
            ArmorHiderClient.RENDER_CONTEXT.setCurrentPlayer(carrier.armorHider$playerName());
        }
    }
    *///?}

    // ===== Per-piece scope setup (renderArmorPiece HEAD) =====

    //? if >= 1.21.9 {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private <S extends HumanoidRenderState> void captureContext(
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci) {
        if (!(humanoidRenderState instanceof IdentityCarrier identityCarrier)) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        var mod = identityCarrier.createModification(equipmentSlot, itemStack);

        if (mod != null && mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
        }
    }
    //?}

    //? if >= 1.21.4 && < 1.21.9 {
    /*@Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, HumanoidModel<?> model, CallbackInfo ci) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        String playerName = ctx.currentPlayerName();
        if (playerName == null) {
            return;
        }
        if (itemStack.is(Items.AIR)) {
            return;
        }
        var mod = ActiveModification.create(playerName, equipmentSlot, itemStack);
        if (mod != null) {
            ctx.setActiveModification(mod);
        }
        if (mod != null && mod.shouldHide()) {
            ctx.clearActiveModification();
            ci.cancel();
        }
    }
    *///?}

    //? if >= 1.21 && < 1.21.4 {
    /*@Inject(
            method = "renderArmorPiece",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, CallbackInfo ci) {
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        ItemStack itemStack = entity.getItemBySlot(slot);
        if (itemStack.is(Items.AIR)) {
            return;
        }

        var mod = carrier.createModification(slot, itemStack);
        if (mod != null && mod.shouldHide()) {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
            ci.cancel();
        }
    }
    *///?}

    //? if < 1.21 {
    /*@Inject(
            method = "renderArmorPiece",
            at = @At("HEAD")
    )
    private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, CallbackInfo ci) {
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        if (entity.getItemBySlot(slot).is(Items.AIR)) {
            return;
        }
        carrier.createModification(slot, entity.getItemBySlot(slot));
    }
    *///?}

    // ===== Per-piece scope cleanup (renderArmorPiece RETURN) =====
    // In 1.21.4–1.21.8 this is handled by EquipmentRenderMixin.resetContext at the renderLayers level.

    //? if >= 1.21.9 {
    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private <S extends HumanoidRenderState> void onRenderArmorPieceReturn(
            PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
    //?}

    //? if < 1.21.4 {
    /*@Inject(
            method = "renderArmorPiece",
            at = @At("RETURN")
    )
    private void onRenderArmorPieceReturn(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
    }
    *///?}

    // ===== Render modifications (< 1.21.4 only — 1.21.4+ uses EquipmentRenderMixin) =====

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

    // Trim transparency support for 1.20.x (uses float RGBA)
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
