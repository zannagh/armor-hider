//? if >= 1.21.9 {
package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//?if >= 1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
//? }
//? if < 1.21.11 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.SkullModel;
*///?}

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRenderMixin {

    @WrapOperation(
            method = "submitSkull",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
                    //? if < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    //? if >= 1.21.11
    private static <S> void modifyTransparency(SubmitNodeCollector instance, Model<? super S> model, S o, PoseStack poseStack, RenderType renderType, int i, int j, int k, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original) {
    //? if < 1.21.11
    //private static <S> void modifyTransparency(SubmitNodeCollector instance, Model<? super S> model, S o, PoseStack poseStack, RenderType renderType, int i, int j, int k, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original) {
        try {
            if (ArmorRenderPipeline.noContext() || !ArmorRenderPipeline.shouldModifyEquipment()) {
                original.call(instance, model, o, poseStack, renderType, i, j, k, crumblingOverlay);
                return;
            }
            var modifiedColor = ArmorRenderPipeline.applyTransparencyFromWhite(255);
            instance.order(ArmorRenderPipeline.SkullRenderPriority).submitModel(model, o, poseStack, renderType, i, j, modifiedColor, null, k, crumblingOverlay);
        } finally {
            ArmorRenderPipeline.clearContext();
        }
    }

    @WrapOperation(
            method = "resolveSkullRenderType",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                     target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;getSkullRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? if < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;getSkullRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
                    
            )
    )
    //? if >= 1.21.11
     private static RenderType getSkullRenderType(SkullBlock.Type type, Identifier identifier, Operation<RenderType> original) {
    //? if < 1.21.11
    //private static RenderType getSkullRenderType(SkullBlock.Type type, ResourceLocation identifier, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(identifier, original.call(type, identifier));
    }

    @WrapOperation(
            method = "getSkullRenderType",
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.1-snapshot-6
                    //target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutoutZOffset(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? if >= 1.21.11 && < 26.1-snapshot-6
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutoutNoCullZOffset(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? if >= 1.21.9 && < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/RenderType;entityCutoutNoCullZOffset(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    //? if >= 1.21.11
    private static RenderType getCutoutRenderLayer(Identifier texture, Operation<RenderType> original) {
    //? if < 1.21.11
    //private static RenderType getCutoutRenderLayer(ResourceLocation texture, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(texture, original.call(texture));
    }
}
//?}

//? if >= 1.21 && < 1.21.4 {
/*package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRenderMixin {
    @WrapOperation(
            method = "renderSkull",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private static void modifyTransparency(SkullModelBase instance, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, Operation<Void> original) {
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            int modifiedColor = ArmorRenderPipeline.applyTransparencyFromWhite(-1);
            instance.renderToBuffer(poseStack, vertexConsumer, light, overlay, modifiedColor);
        } else {
            original.call(instance, poseStack, vertexConsumer, light, overlay);
        }
    }

    @WrapOperation(
            method = "getRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;getSkullRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private static RenderType modifySkullTransparency(SkullBlock.Type type, ResourceLocation resourceLocation, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(resourceLocation, original.call(type, resourceLocation));
    }
}
*///?}

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRenderMixin {
    @WrapOperation(
            method = "renderSkull",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private static void modifyTransparency(SkullModelBase instance, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, Operation<Void> original) {
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            int modifiedColor = ArmorRenderPipeline.applyTransparencyFromWhite(-1);
            instance.renderToBuffer(poseStack, vertexConsumer, light, overlay, modifiedColor);
        } else {
            original.call(instance, poseStack, vertexConsumer, light, overlay);
        }
    }

    @WrapOperation(
            //? if >= 1.21.6
            //method = "getPlayerSkinRenderType",
            //? if < 1.21.6
            method = "getRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/world/item/component/ResolvableProfile;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private static RenderType modifySkullTransparency(ResourceLocation texture, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(texture, original.call(texture));
    }

    @WrapOperation(
            //? if >= 1.21.6
            //method = "getSkullRenderType",
            //? if < 1.21.6
            method = "getRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/world/item/component/ResolvableProfile;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityCutoutNoCullZOffset(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private static RenderType getCutoutRenderLayer(ResourceLocation texture, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(texture, original.call(texture));
    }
}
*///?}

//? if < 1.21 {
/*package de.zannagh.armorhider.mixin.client.head;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.rendering.ArmorRenderPipeline;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRenderMixin {
    @WrapOperation(
            method = "renderSkull",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/SkullModelBase;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            )
    )
    private static void modifyTransparency(SkullModelBase instance, PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha, Operation<Void> original) {
        if (ArmorRenderPipeline.hasActiveContext() && ArmorRenderPipeline.shouldModifyEquipment()) {
            if (!ArmorRenderPipeline.getCurrentModification().playerConfig().opacityAffectingHatOrSkull.getValue()) {
                original.call(instance, poseStack, vertexConsumer, light, overlay, red, green, blue, alpha);
                return;
            }

            float newAlpha = ArmorRenderPipeline.getTransparencyAlpha();
            instance.renderToBuffer(poseStack, vertexConsumer, light, overlay, red, green, blue, newAlpha);
        } else {
            original.call(instance, poseStack, vertexConsumer, light, overlay, red, green, blue, alpha);
        }
    }

    @WrapOperation(
            method = "getRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private static RenderType modifySkullTransparency(ResourceLocation texture, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(texture, original.call(texture));
    }

    @WrapOperation(
            method = "getRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityCutoutNoCullZOffset(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private static RenderType getCutoutRenderLayer(ResourceLocation texture, Operation<RenderType> original) {
        return ArmorRenderPipeline.getSkullRenderLayer(texture, original.call(texture));
    }
}
*///?}
