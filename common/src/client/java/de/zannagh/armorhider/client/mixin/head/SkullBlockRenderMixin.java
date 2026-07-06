package de.zannagh.armorhider.client.mixin.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 1.21.9 {
import de.zannagh.armorhider.client.render.RenderModifications;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.SkullBlock;
//? } else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.SkullModelBase;
*///?}

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRenderMixin {

    @Unique
    private static final String RENDER_ENTRY =
            //? if >= 1.21.9 {
            "submitSkull";
            //? } else
            /*"renderSkull";*/

    @Unique
    private static final String RENDER_TARGET =
            //? if >= 26.3-0.snapshot.2 {
            /*"Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;III)V";
            *///? } elif >= 1.21.9 {
            "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V";
            //? } elif >= 1.21 {
            /*"Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V";*/
            //? } else {
            /*"Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";*/
            //? }

    @WrapOperation(
            method = RENDER_ENTRY,
            at = @At(value = "INVOKE", target = RENDER_TARGET)
    )
    //? if >= 26.3-0.snapshot.2 {
    /*private static <S> void modifyTransparency(SubmitNodeCollector instance, Model<? super S> model, S o, PoseStack poseStack, RenderType renderType, int i, int j, int k, Operation<Void> original) {
    *///? } elif >= 1.21.9 {
    private static <S> void modifyTransparency(SubmitNodeCollector instance, Model<? super S> model, S o, PoseStack poseStack, RenderType renderType, int i, int j, int k, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original) {
    //? } elif >= 1.21 {
    /*private static void modifyTransparency(SkullModelBase instance, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, Operation<Void> original) {*/
    //? } else {
    /*private static void modifyTransparency(SkullModelBase instance, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha, Operation<Void> original) {*/
    //? }
        var headCtx = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        if (headCtx.shouldCancel()) {
            return;
        }
        // Short-circuit on !needsModification, not just isEmpty: the scope is non-empty whenever
        // a slot modification is in effect (even for full opacity), and falling through to
        // applyTransparencyFromWhite with the no-modification fallback color invisibly returns
        // 255 (= ARGB 0x000000FF, alpha 0), which would render the skull as transparent black.
        if (!headCtx.needsModification()) {
            //? if >= 26.3-0.snapshot.2 {
            /*original.call(instance, model, o, poseStack, renderType, i, j, k);
            *///? } elif >= 1.21.9 {
            original.call(instance, model, o, poseStack, renderType, i, j, k, crumblingOverlay);
            //? } elif >= 1.21 {
            /*original.call(instance, poseStack, vertexConsumer, light, overlay);*/
            //? } else {
            /*original.call(instance, poseStack, vertexConsumer, light, overlay, red, green, blue, alpha);*/
            //? }
            return;
        }
        //? if >= 26.3-0.snapshot.2 {
        /*// 26.3 removed the CrumblingOverlay parameter (crumbling is now a separate
        // submitCrumblingOverlay call) and the sprite/UvMapping arg; skulls pass no sprite,
        // so the plain 7-arg submitModel with the alpha-modified color is sufficient.
        var modifiedColor = headCtx.renderModificationApi().applyTransparencyFromWhite(0xFFFFFFFF);
        instance.order(RenderModifications.SKULL_RENDER_PRIORITY).submitModel(model, o, poseStack, renderType, i, j, modifiedColor);
        *///? } elif >= 1.21.9 {
        var modifiedColor = headCtx.renderModificationApi().applyTransparencyFromWhite(0xFFFFFFFF);
        instance.order(RenderModifications.SKULL_RENDER_PRIORITY).submitModel(model, o, poseStack, renderType, i, j, modifiedColor, null, k, crumblingOverlay);
        //? } elif >= 1.21 {
        /*int modifiedColor = headCtx.renderModificationApi().applyTransparencyFromWhite(0xFFFFFFFF);
        instance.renderToBuffer(poseStack, vertexConsumer, light, overlay, modifiedColor);*/
        //? } else {
        /*float newAlpha = headCtx.renderModificationApi().getTransparencyAlpha();
        instance.renderToBuffer(poseStack, vertexConsumer, light, overlay, red, green, blue, newAlpha);*/
        //? }
    }

    @Unique
    private static RenderType applyLayer(Identifier texture, RenderType original) {
        // A null texture reaches here from resolveSkullRenderType's getSkullRenderType(type, null)
        // call — the real texture is resolved *inside* getSkullRenderType (handled by
        // getCutoutRenderLayer). Building a translucent type from a null texture would NPE the
        // memoize cache, so leave it to the inner wrap.
        if (texture == null) {
            return original;
        }
        var headCtx = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        if (!headCtx.isEmpty() && headCtx.renderModificationApi().getSkullRenderLayer(texture, original) instanceof RenderType rt) {
            return rt;
        }
        return original;
    }

    //? if >= 1.21.9 {
    @WrapOperation(
            method = "resolveSkullRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;getSkullRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private static RenderType wrapResolveSkullRenderType(SkullBlock.Type type, Identifier identifier, Operation<RenderType> original) {
        return applyLayer(identifier, original.call(type, identifier));
    }
    //? }

    //? if (>= 1.21.4 && < 1.21.9) || < 1.21 {
    /*@WrapOperation(
            //? if >= 1.21.6 && < 1.21.9
            method = "getPlayerSkinRenderType",
            //? if >= 1.21.4 && < 1.21.6
            //method = "getRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/world/item/component/ResolvableProfile;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            //? if < 1.21
            //method = "getRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderType;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private static RenderType wrapTranslucentLayer(Identifier texture, Operation<RenderType> original) {
        return applyLayer(texture, original.call(texture));
    }
    *///?}

    @WrapOperation(
            //? if >= 1.21.6 {
            method = "getSkullRenderType",
            //? } elif >= 1.21.4 {
            /*method = "getRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/world/item/component/ResolvableProfile;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;",*/
            //? } else {
            /*method = "getRenderType",*/
            //? }
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.1-0.snapshot.6 {
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutoutZOffset(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? } elif >= 1.21.11 {
                    /*target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutoutNoCullZOffset(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"*/
                    //? } else {
                    /*target = "Lnet/minecraft/client/renderer/rendertype/RenderType;entityCutoutNoCullZOffset(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"*/
                    //? }
            )
    )
    private static RenderType getCutoutRenderLayer(Identifier texture, Operation<RenderType> original) {
        return applyLayer(texture, original.call(texture));
    }
}
