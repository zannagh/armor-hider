//? if >= 1.21.2 && < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.VanillaArmorTextureManager;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * Applies armor/elytra transparency for the 1.21.2–1.21.3 transitional render-state era.
 *
 * <p>In this window armor and elytra already render through {@link EquipmentLayerRenderer#renderLayers}
 * (WingsLayer/HumanoidArmorLayer both delegate here), exactly like 1.21.4+, but the two leading parameters
 * are still {@code EquipmentModel$LayerType} + {@code Identifier} rather than the 1.21.4
 * {@code EquipmentClientInfo$LayerType} + {@code ResourceKey<EquipmentAsset>}. {@code EquipmentRenderMixin}
 * is therefore gated {@code >= 1.21.4}, leaving these two versions with scope entry (handled by
 * HumanoidArmorLayerMixin / ElytraRenderMixin) but no alpha applied. This mixin fills exactly that gap; the
 * internal render calls it wraps ({@code armorCutoutNoCull}, {@code armorTrimsSheet}, {@code renderToBuffer},
 * {@code hasFoil}) are identical to the 1.21.4–1.21.8 branch. The combat "use vanilla model" bookkeeping is
 * intentionally omitted here — it is a separate, higher-version-only concern.
 ^/
@SuppressWarnings("UnusedMixin")
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererLegacyMixin {

    @Unique
    private static final String RENDER_LAYERS_DETAIL =
            "renderLayers(Lnet/minecraft/world/item/equipment/EquipmentModel$LayerType;Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/Identifier;)V";

    @ModifyExpressionValue(
            method = RENDER_LAYERS_DETAIL,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z")
    )
    private boolean armorHider$modifyGlint(boolean original) {
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA)
                .renderModificationApi().getHasFoil(original);
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType armorHider$modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        if (ctx.isEmpty()) {
            return original.call(texture);
        }
        Identifier resolved = VanillaArmorTextureManager.resolveArmorTexture(ctx.modification(), texture);
        var originalType = original.call(resolved);
        return ctx.renderModificationApi().getTranslucentArmorRenderType(resolved, originalType) instanceof RenderType rt
                ? rt : originalType;
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType armorHider$modifyTrimRenderLayer(boolean decal, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi();
        var originalType = original.call(decal);
        return modApi.getTrimRenderLayer(decal, originalType) instanceof RenderType rt ? rt : originalType;
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private void armorHider$modifyArmorColor(Model model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        if (ctx.isEmpty()) {
            original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, color);
            return;
        }
        int modifiedColor = ctx.renderModificationApi().applyArmorTransparency(color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V")
    )
    private void armorHider$modifyTrimColor(Model model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        if (ctx.isEmpty()) {
            original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay);
            return;
        }
        int color = ctx.renderModificationApi().applyTransparencyFromWhite();
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
*///?}
