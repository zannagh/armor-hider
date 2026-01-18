// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRenderMixin {
    @ModifyVariable(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;II)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    private static int modifyRenderOrder(int k, EquipmentClientInfo.LayerType layerType, Object assetKey, Model<?> model, Object object, ItemStack itemStack) {
        return ArmorRenderPipeline.modifyRenderPriority(k, itemStack);
    }

    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static <S> void interceptRender(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, CallbackInfo ci) {

        ArmorRenderPipeline.setupContext(itemStack, null, (LivingEntityRenderState) object);

        if (!ArmorRenderPipeline.shouldModifyEquipment() || ArmorRenderPipeline.renderStateDoesNotTargetPlayer(object)) {
            return;
        }

        if (layerType == EquipmentModel.LayerType.WINGS && !ArmorRenderPipeline.getCurrentModification().playerConfig().opacityAffectingElytra.getValue()) {
            ArmorRenderPipeline.clearContext();
            return;
        }

        if (ArmorRenderPipeline.shouldHideEquipment() && ci != null) {
            ci.cancel();
        }
    }
    
    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V"
            )
    )
    private static <S> void wrap(EquipmentRenderer instance, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey, Model<? super S> model, S state, ItemStack stack, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, @Nullable Identifier textureId, int outlineColor, int initialOrder, Operation<Void> original){
        if (!ArmorRenderPipeline.shouldHideEquipment()) {
            original.call(instance, layerType, assetKey, model, state, stack, matrices, queue, light, textureId, outlineColor, initialOrder);
        }
    }
    
    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;hasGlint()Z"
            )
    )
    private boolean modifyGlint(boolean original) {
        return original && !ArmorRenderPipeline.shouldHideEquipment();
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayers;armorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private static <S> RenderLayer modifyArmourCutoutNoCull(Identifier texture, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getRenderLayer(texture, original.call(texture));
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/TexturedRenderLayers;getArmorTrims(Z)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private RenderLayer modifyTrimRenderLayer(boolean decal, Operation<RenderLayer> original) {
        return ArmorRenderPipeline.getTrimRenderLayer(decal, original.call(decal));
    }

    @WrapOperation(
        method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        )
    )
    private static <S> void modifyColor(RenderCommandQueue instance, Model<? super S> model, S s, MatrixStack matrixStack, RenderLayer renderLayer, int light, int overlay, int tintedColor, Sprite sprite, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand, Operation<Void> original) {

        if (!ArmorRenderPipeline.hasActiveContext()) {
            ArmorRenderPipeline.addContext(s);
        }

        if (ArmorRenderPipeline.renderStateDoesNotTargetPlayer(s)) {
            original.call(instance, model, s, matrixStack, renderLayer, light, overlay, tintedColor, sprite, outlineColor, crumblingOverlayCommand);
            return;
        }

        int modifiedColor = ArmorRenderPipeline.applyTransparency(tintedColor);
        original.call(instance, model, s, matrixStack, renderLayer, light, overlay, modifiedColor, sprite, outlineColor, crumblingOverlayCommand);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At("RETURN")
    )
    private static <S> void resetContext(EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey, Model<? super S> model, S object, ItemStack itemStack, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, Identifier identifier, int j, int k, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }
}
