// | --------------------------------------------------- |
// | This logic is inspired by Show Me Your Skin!        |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.ArmorHiderClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
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
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.zannagh.armorhider.client.ArmorHiderClient.tryResolveConfigFromPlayerEntityState;

@Mixin(EquipmentRenderer.class)
public class EquipmentRenderMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static <S> void interceptRender(EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey, Model<? super S> model, S object, ItemStack itemStack, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, Identifier identifier, int j, int k, CallbackInfo ci){
        ArmorHiderClient.trySetCurrentSlotFromEntityRenderState((LivingEntityRenderState) object);
        
        if (ArmorHiderClient.CurrentArmorMod.get() == null) {
            return;
        }

        if (!ArmorHiderClient.CurrentArmorMod.get().ShouldModify()) {
            return;
        }

        if (ArmorHiderClient.shouldNotInterceptRender(object)) {
            return;
        }
        
        if (ArmorHiderClient.CurrentArmorMod.get().ShouldHide()) {
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
        if (ArmorHiderClient.CurrentArmorMod.get() == null) {
            return original;
        }

        if (!ArmorHiderClient.CurrentArmorMod.get().ShouldModify()) {
            return original;
        }
        
        
        return original && ArmorHiderClient.CurrentArmorMod.get().GetTransparency() > 0;
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayers;armorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private static <S> RenderLayer modifyArmourCutoutNoCull(Identifier texture, Operation<RenderLayer> original) {
        if (ArmorHiderClient.CurrentArmorMod.get() == null) {
            return original.call(texture);
        }

        if (!ArmorHiderClient.CurrentArmorMod.get().ShouldModify()) {
            return original.call(texture);
        }
        
        return RenderLayers.armorTranslucent(texture);
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/TexturedRenderLayers;getArmorTrims(Z)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private RenderLayer modifyTrimRenderLayer(boolean decal, Operation<RenderLayer> original) {
        if (ArmorHiderClient.CurrentArmorMod.get() == null) {
            return original.call(decal);
        }

        if (!ArmorHiderClient.CurrentArmorMod.get().ShouldModify()) {
            return original.call(decal);
        }
        
        if (ArmorHiderClient.CurrentArmorMod.get().GetTransparency() < 1) {
            return RenderLayers.armorTranslucent(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
        }

        return original.call(decal);
    }

    @WrapOperation(
        method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        )
    )
    private static <S> void modifyColor(RenderCommandQueue instance, Model<? super S> model, S s, MatrixStack matrixStack, RenderLayer renderLayer, int light, int overlay, int tintedColor, Sprite sprite, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand, Operation<Void> original) {
        if (ArmorHiderClient.CurrentArmorMod.get() == null && s instanceof PlayerEntityRenderState playerEntityRenderState && ArmorHiderClient.CurrentSlot.get() != null) {
            var config = tryResolveConfigFromPlayerEntityState(ArmorHiderClient.CurrentSlot.get(), playerEntityRenderState);
            ArmorHiderClient.CurrentArmorMod.set(config);
        }

        if (!ArmorHiderClient.shouldNotInterceptRender(s)) {
            original.call(instance, model, s, matrixStack, renderLayer, light, overlay, tintedColor, sprite, outlineColor, crumblingOverlayCommand);
            return;
        }
        
        if (ArmorHiderClient.CurrentArmorMod.get() != null && ArmorHiderClient.CurrentArmorMod.get().ShouldModify()) {

            double transparency = ArmorHiderClient.CurrentArmorMod.get().GetTransparency();
            var newColor = ColorHelper.withAlpha(ColorHelper.channelFromFloat((float)transparency), tintedColor);
            original.call(instance, model, s, matrixStack, renderLayer, light, overlay, newColor, sprite, outlineColor, crumblingOverlayCommand);
        }
        else {
            original.call(instance, model, s, matrixStack, renderLayer, light, overlay, tintedColor, sprite, outlineColor, crumblingOverlayCommand);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At("RETURN")
    )
    private static <S> void resetContext(EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> assetKey, Model<? super S> model, S object, ItemStack itemStack, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, Identifier identifier, int j, int k, CallbackInfo ci) {
        ArmorHiderClient.CurrentArmorMod.remove();
        ArmorHiderClient.CurrentSlot.remove();
    }
    
    
    
}
