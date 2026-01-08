// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class EquipmentRenderMixin<T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>> {

    // Cancel rendering if armor should be hidden
    @Inject(
            method = "renderArmor",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void interceptRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, T entity, EquipmentSlot armorSlot, int light, A model, CallbackInfo ci) {
        if (!ArmorRenderPipeline.hasActiveContext() || !ArmorRenderPipeline.shouldModifyEquipment()) {
            return;
        }

        if (ArmorRenderPipeline.renderStateDoesNotTargetPlayer(entity)) {
            return;
        }

        if (ArmorRenderPipeline.shouldHideEquipment()) {
            ci.cancel();
        }
    }

    // Hide glint when armor is hidden
    @ModifyExpressionValue(
            method = "renderArmor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;hasGlint()Z"
            )
    )
    private boolean modifyGlint(boolean original) {
        if (!ArmorRenderPipeline.hasActiveContext()) {
            return original;
        }
        return original && !ArmorRenderPipeline.shouldHideEquipment();
    }

    // Modify render layer to support transparency
    @WrapOperation(
            method = "renderArmorParts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private RenderLayer modifyArmorCutoutNoCull(Identifier texture, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getRenderLayer(texture, original.call(texture));
    }

    // Modify trim render layer for transparency
    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/TexturedRenderLayers;getArmorTrims(Z)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private RenderLayer modifyTrimRenderLayer(boolean decal, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getTrimRenderLayer(decal, original.call(decal));
    }

    // Apply transparency to armor color
    @WrapOperation(
            method = "renderArmorParts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void modifyArmorColor(BipedEntityModel instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int k, Operation<Void> original) {
        int modifiedOverlay = ArmorRenderPipeline.applyTransparency(k);
        original.call(instance, matrixStack, vertexConsumer, i, j, modifiedOverlay);
    }

    // Apply transparency to armor trim color
    @WrapOperation(
            method = "renderTrim",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"
            )
    )
    private void modifyTrimColor(BipedEntityModel<?> model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, Operation<Void> original) {
        int modifiedOverlay = ArmorRenderPipeline.applyTransparency(overlay);
        original.call(model, matrices, vertices, light, modifiedOverlay);
    }

    // Clear context after rendering
    @Inject(
            method = "renderArmor",
            at = @At("RETURN")
    )
    private void resetContext(MatrixStack matrices, VertexConsumerProvider vertexConsumers, T entity, EquipmentSlot armorSlot, int light, A model, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }
}
