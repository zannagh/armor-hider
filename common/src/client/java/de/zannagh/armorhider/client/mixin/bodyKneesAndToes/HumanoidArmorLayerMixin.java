package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.configuration.*;
import de.zannagh.armorhider.client.api.render.RenderScope;
import de.zannagh.armorhider.client.api.render.ScopeContext;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import de.zannagh.armorhider.client.rendering.VanillaArmorTextureManager;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
/*import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
*///?}

//? if < 1.21.4 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.At;
*///?}

//? if >= 1.21 && < 1.21.4 {
/*import net.minecraft.client.renderer.Sheets;
 *///?}

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin
//? if < 1.21.4
//<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
{

    // ===== Player/state capture + early-out (render HEAD) =====

    //? if < 1.21.9 {
    /*
    //? if >= 1.21.4 && < 1.21.9 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends HumanoidRenderState> void captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, S entity, float f, float g, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
    //? }
    //? if >= 1.21 && < 1.21.4 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void capturePlayerName(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float f1, float f2, float f3, float f4, float f5, float f6, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
    //? }
    //? if < 1.21 {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipIfAllHidden(PoseStack poseStack, MultiBufferSource bufferSource, int light, T entity, float f1, float f2, float f3, float f4, float f5, float f6, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
    //?}
        if (!(entity instanceof IdentityCarrier carrier)) {
            return;
        }
        if (carrier.armorHider$allSlotsFullyHidden()) {
            var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
            api.setCurrentPlayer(carrier.armorHider$playerName());
            ci.cancel();
            return;
        }
        scopeContext.set(ScopeContext.empty(RenderScope.ARMOR_PIECE));
    }
    *///?}

    // ===== Per-piece scope setup (renderArmorPiece HEAD) =====

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.9
    private <S extends HumanoidRenderState> void captureContext(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot slot, int i, S humanoidRenderState, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
    //? if >= 1.21.4 && < 1.21.9
    //private void captureContext(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot slot, int i, HumanoidModel<?> humanoidRenderState, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
    //? if < 1.21.4
    //private void onRenderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, T humanoidRenderState, EquipmentSlot slot, int packedLight, A itemStack, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {

        //? if >= 1.21.4
        IdentityCarrier carrier = humanoidRenderState instanceof IdentityCarrier ic ? ic : null;
        //? if < 1.21.4
        //IdentityCarrier carrier = humanoidRenderState instanceof IdentityCarrier ic ? ic : (scopeContext.get() != null ? scopeContext.get().carrier() : null);
        if (carrier == null) {
            return;
        }

        var api = ArmorHiderClientApi.getInstance().getRenderingScopeApi();
        //? if >= 1.21.4
        var ctx = api.enterScope(RenderScope.ARMOR_PIECE, carrier, slot, itemStack);
        //? if < 1.21.4
        //var ctx = api.enterScope(RenderScope.ARMOR_PIECE, carrier, slot, null);
        scopeContext.set(ctx);

        if (ctx.shouldCancel()) {
            api.exitScope(RenderScope.ARMOR_PIECE);
            ci.cancel();
        }
    }

    // ===== Per-piece scope cleanup (renderArmorPiece RETURN) =====
    // In 1.21.4–1.21.8 this is handled by EquipmentRenderMixin.resetContext at the renderLayers level.

    //? if >= 1.21.9 || < 1.21.4 {
    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    //? if >= 1.21.9
    private <S extends HumanoidRenderState> void onRenderArmorPieceReturn(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, S humanoidRenderState, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
    //? if < 1.21.4
    //private void onRenderArmorPieceReturn(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int packedLight, A armorModel, CallbackInfo ci, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        scopeContext.set(null);
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
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
    private boolean modifyGlint(boolean original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) return original;
        var mod = scopeContext.get().modification();
        if (mod.shouldDisableGlint() || mod.shouldHide()) {
            return false;
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
    private RenderType modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) return original.call(texture);
        Identifier resolved = VanillaArmorTextureManager.resolveArmorTexture(scopeContext.get().modification(), texture);
        return new RenderModifications(scopeContext.get().modification()).getTranslucentArmorRenderType(resolved, original.call(resolved));
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
    private void modifyArmorColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color, Operation<Void> original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, color); return; }
        int modifiedColor = new RenderModifications(scopeContext.get().modification()).applyArmorTransparency(color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) return original.call(decal);
        return new RenderModifications(scopeContext.get().modification()).getTrimRenderLayer(decal, original.call(decal));
    }

    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private void modifyTrimColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay); return; }
        int modifiedColor = new RenderModifications(scopeContext.get().modification()).applyTransparencyFromWhite(packedOverlay);
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
    private void modifyArmorColor(A model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float v, Operation<Void> original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, v); return; }
        float modifiedAlpha = new RenderModifications(scopeContext.get().modification()).getTransparencyAlpha();
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
    private void modifyTrimColor(HumanoidModel<?> model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, Operation<Void> original, @Share(value = "scopeContext") LocalRef<ScopeContext> scopeContext) {
        if (scopeContext.get() == null || scopeContext.get().isEmpty()) { original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha); return; }
        float modifiedAlpha = new RenderModifications(scopeContext.get().modification()).getTransparencyAlpha();
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, modifiedAlpha);
    }
    *///?}
}
