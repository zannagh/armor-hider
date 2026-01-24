package de.zannagh.armorhider.mixin.client.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import de.zannagh.armorhider.resources.ArmorModificationInfo;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
//? }
//? if = 1.21.10 || 1.21.9 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRenderMixin {

    //? if >= 1.21.11 {
    @ModifyVariable(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    //?}
    //? if = 1.21.10 || 1.21.9 {
    /*@ModifyVariable(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    *///?}
    private static int modifyRenderOrder(int value) {
        return ArmorRenderPipeline.modifyRenderPriority(value);
    }

    
    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static <S> void interceptRender(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, CallbackInfo ci) {
        var equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return;
        }

        // FIX START: Check if the render state is actually a HumanoidRenderState before casting.
        // This prevents ClassCastException when rendering non-humanoid entities with equipment (e.g., Wolves with Armor).
        if (!(object instanceof HumanoidRenderState)) {
            return;
        }
        // FIX END

        var slot = equippable.slot();
        ArmorRenderPipeline.setupContext(itemStack, slot, (HumanoidRenderState) object);

        if (!ArmorRenderPipeline.shouldModifyEquipment() || ArmorRenderPipeline.renderStateDoesNotTargetPlayer(object)) {
            return;
        }

        if (layerType == EquipmentClientInfo.LayerType.WINGS && !ArmorRenderPipeline.getCurrentModification().playerConfig().opacityAffectingElytra.getValue()) {
            ArmorRenderPipeline.clearContext();
            return;
        }

        if (ArmorRenderPipeline.shouldHideEquipment() && ci != null) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;II)V",
            at = @At("RETURN")
    )
    private static <S> void resetContext(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, CallbackInfo ci) {
        ArmorRenderPipeline.clearContext();
    }

    //? if >= 1.21.11 {
    
    @ModifyExpressionValue(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"
            )
    )
    //?}
    //? if = 1.21.10 || 1.21.9 {
    /*@ModifyExpressionValue(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"
            )
    )
    *///?}
    private boolean modifyGlint(boolean original) {
        ArmorModificationInfo modification = ArmorRenderPipeline.getCurrentModification();
        if (modification != null && modification.shouldModify() && ArmorRenderPipeline.shouldModifyEquipment()) {
            // Disable glint if transparency is low enough (armor hidden or nearly hidden)
            double transparency = modification.getTransparency();
            // Hide glint when armor is hidden
            return original && transparency > 0;
        }
        return original;
    }

    //? if >= 1.21.11 {
    
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original) {
     
    //?}
    //? if = 1.21.10 || 1.21.9 {
    /*@WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(ResourceLocation texture, Operation<RenderType> original) {
    *///?}
        return ArmorRenderPipeline.getTranslucentArmorRenderTypeIfApplicable(texture, original.call(texture));
    }

    //? if >= 1.21.11 {
    
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    //?}
    //? if = 1.21.10 || 1.21.9 {
    /*@WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    *///?}
    private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original) {
        return ArmorRenderPipeline.getTranslucentArmorRenderTypeIfApplicable(Sheets.ARMOR_TRIMS_SHEET, original.call(decal));
    }

    //? if >= 1.21.11 {
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private <S> void modifyArmorColor(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite, int param9, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original) {
        var modifiedColor = ArmorRenderPipeline.applyArmorTransparency(color);
        original.call(collector, model, state, poseStack, renderType, light, overlay, modifiedColor, sprite, param9, crumblingOverlay);
    }
    //?}

    //? if = 1.21.10 || 1.21.9 {
    /*@WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I"
            )
    )
    private <S> int modifyArmorColor(EquipmentClientInfo.Layer layer, int i, Operation<Integer> original) {
        int originalColor = original.call(layer, i);
        return ArmorRenderPipeline.applyArmorTransparency(originalColor);
    }
    *///?}
}