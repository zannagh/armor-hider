//? if >= 1.21.9 {
package de.zannagh.armorhider.mixin.client.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.util.ArrayList;
import java.util.List;

//? if >= 26.1-snapshot-7
//import net.minecraft.client.renderer.Sheets;
//? if >= 1.21.11
import net.minecraft.client.renderer.rendertype.RenderType;
//? if < 1.21.11
//import net.minecraft.client.renderer.RenderType;

/// Intercepts item layer rendering to apply transparency to off-hand items.
/// Modifies tint layer alpha values and assigns a synthetic tint index to non-tinted
/// quads so that all quads receive the desired alpha during deferred rendering.
/// This is only used in 1.21.9 and upwards as below, ItemRender.renderStatic takes care of this.
@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ItemStackRenderState.LayerRenderState.class)
public class ItemRenderStateMixin {

    @WrapOperation(
            method = "submit",
            at = @At(
                value = "INVOKE",
                //? if >= 26.1-snapshot-7
                //target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
                //? if >= 1.21.11 && < 26.1-snapshot-7
                target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
                //? if 1.21.9 || 1.21.10
                //target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
            )
    )
    //? if >= 26.1-snapshot-7
    //private void wrapSubmitItem(SubmitNodeCollector instance, PoseStack poseStack, ItemDisplayContext itemDisplayContext, int light, int overlay, int color, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType, Operation<Void> original) {
    //? if >= 1.21.11 && < 26.1-snapshot-7
    private void wrapSubmitItem(SubmitNodeCollector instance, PoseStack poseStack, ItemDisplayContext itemDisplayContext, int light, int overlay, int color, int[] tintLayers, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foilType, Operation<Void> original) {
    //? if 1.21.9 || 1.21.10
    //private void wrapSubmitItem(SubmitNodeCollector instance, PoseStack poseStack, ItemDisplayContext itemDisplayContext, int light, int overlay, int color, int[] tintLayers, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foilType, Operation<Void> original) {
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            float alpha = ArmorRenderPipeline.getTransparencyAlpha();
            //? if < 26.1-snapshot-7
            RenderType translucentType = ArmorRenderPipeline.getTranslucentItemRenderTypeIfApplicable(renderType);

            // Use one extra slot for non-tinted quads: white with our alpha
            int syntheticTintIndex = tintLayers.length;
            int alphaInt = Math.round(alpha * 255);

            int[] modifiedTints = new int[syntheticTintIndex + 1];
            for (int t = 0; t < tintLayers.length; t++) {
                int origAlpha = (tintLayers[t] >> 24) & 0xFF;
                int newAlpha = Math.round(alpha * origAlpha);
                modifiedTints[t] = (tintLayers[t] & 0x00FFFFFF) | (newAlpha << 24);
            }
            modifiedTints[syntheticTintIndex] = (alphaInt << 24) | 0x00FFFFFF;

            // Reassign non-tinted quads to the synthetic tint index so they pick up alpha
            List<BakedQuad> modifiedQuads = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                if (!quad.isTinted()) {
                    //? if >= 26.1-snapshot-7 {
                    /*modifiedQuads.add(new BakedQuad(
                            quad.position0(), quad.position1(), quad.position2(), quad.position3(), 
                            quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                            syntheticTintIndex, quad.direction(), makeTranslucent(quad.spriteInfo()), quad.shade(), quad.lightEmission()
                    ));
                    *///?}
                    //? if >= 1.21.11 && < 26.1-snapshot-7 {
                    
                    modifiedQuads.add(new BakedQuad(
                            quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                            quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                            syntheticTintIndex, quad.direction(), quad.sprite(), quad.shade(), quad.lightEmission()
                    ));
                    //? }
                    
                    //? if 1.21.9 || 1.21.10 {
                    /*modifiedQuads.add(new BakedQuad(
                            quad.vertices(), syntheticTintIndex, quad.direction(), quad.sprite(), quad.shade(), quad.lightEmission()
                    ));
                    *///?}
                    
                } else {
                    //? if >= 26.1-snapshot-7 {
                    /*modifiedQuads.add(new BakedQuad(
                            quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                            quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                            quad.tintIndex(), quad.direction(), makeTranslucent(quad.spriteInfo()), quad.shade(), quad.lightEmission()
                    ));
                    *///? }
                    //? if < 26.1-snapshot-7
                    modifiedQuads.add(quad);
                }
            }
            //? if >= 26.1-snapshot-7
            //original.call(instance, poseStack, itemDisplayContext, light, overlay, color, modifiedTints, modifiedQuads, foilType);
            //? if < 26.1-snapshot-7
            original.call(instance, poseStack, itemDisplayContext, light, overlay, color, modifiedTints, modifiedQuads, translucentType, foilType);
        } else {
            //? if >= 26.1-snapshot-7
            //original.call(instance, poseStack, itemDisplayContext, light, overlay, color, tintLayers, quads, foilType);
            //? if < 26.1-snapshot-7
            original.call(instance, poseStack, itemDisplayContext, light, overlay, color, tintLayers, quads, renderType, foilType);
        }
    }

    //? if >= 26.1-snapshot-7 {
    /*@Unique
    private static BakedQuad.SpriteInfo makeTranslucent(BakedQuad.SpriteInfo info) {
        RenderType current = info.itemRenderType();
        if (current == Sheets.cutoutBlockItemSheet()) {
            return new BakedQuad.SpriteInfo(info.sprite(), info.layer(), Sheets.translucentBlockItemSheet());
        }
        if (current == Sheets.cutoutItemSheet()) {
            return new BakedQuad.SpriteInfo(info.sprite(), info.layer(), Sheets.translucentItemSheet());
        }
        return info;
    }
    *///? }
}
//?}
