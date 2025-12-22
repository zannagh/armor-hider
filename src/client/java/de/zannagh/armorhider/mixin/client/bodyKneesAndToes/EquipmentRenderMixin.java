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
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.zannagh.armorhider.client.ArmorHiderClient.tryResolveConfigFromPlayerEntityState;

@Mixin(EquipmentRenderer.class)
public class EquipmentRenderMixin {
    @ModifyVariable(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    private static int modifyRenderOrder(int k, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey, Model<?> model, Object object, ItemStack itemStack) {
        return ArmorRenderPipeline.modifyRenderPriority(k, itemStack);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static <S> void interceptRender(EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey, Model<? super S> model, S object, ItemStack itemStack, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, Identifier identifier, int j, int k, CallbackInfo ci) {

        ArmorRenderPipeline.setupContext(itemStack, (LivingEntityRenderState) object);

        if (!ArmorRenderPipeline.shouldModifyEquipment()
                || ArmorRenderPipeline.shouldInterceptRender(object)) {
            return;
        }

        if (ArmorRenderPipeline.shouldHideEquipment()) {
            if (ci != null) {
                ci.cancel();
            }
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
        if (!ArmorRenderPipeline.shouldModifyEquipment()) {
            return original;
        }

        return original && ArmorRenderPipeline.getTransparency() > 0;
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
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

        // Fallback: try to set modification context if null (edge case handling)
        if (ArmorRenderPipeline.getCurrentModification() == null
            && s instanceof PlayerEntityRenderState playerEntityRenderState
            && ArmorRenderPipeline.getCurrentSlot() != null) {
            var config = tryResolveConfigFromPlayerEntityState(
                ArmorRenderPipeline.getCurrentSlot(),
                playerEntityRenderState
            );
            ArmorRenderPipeline.setCurrentModification(config);
        }

        if (ArmorRenderPipeline.shouldInterceptRender(s)) {
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
