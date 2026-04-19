//? if < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import de.zannagh.armorhider.client.rendering.RenderTypeFactory;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private MultiBufferSource wrapBufferSourceForTransparency(MultiBufferSource bufferSource) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (!ctx.hasActiveModification(EquipmentSlot.OFFHAND) && !ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            return bufferSource;
        }
        var mod = ctx.activeModification();
        if (mod == null || mod.transparency() >= 1.0 || mod.transparency() <= 0) {
            return bufferSource;
        }
        // Wrap to swap cutout render types to translucent equivalents
        return (RenderType renderType) -> {
            if (renderType == Sheets.cutoutBlockSheet()) {
                return bufferSource.getBuffer(Sheets.translucentItemSheet());
            }
            if (renderType == Sheets.shieldSheet()) {
                return bufferSource.getBuffer(RenderTypeFactory.translucentEntity(Sheets.SHIELD_SHEET));
            }
            if (renderType == Sheets.bannerSheet()) {
                return bufferSource.getBuffer(RenderTypeFactory.translucentEntity(Sheets.BANNER_SHEET));
            }
            return bufferSource.getBuffer(renderType);
        };
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/BlockEntityWithoutLevelRenderer;renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
            )
    )
    private void wrapShieldRender(BlockEntityWithoutLevelRenderer instance, ItemStack itemStack, ItemDisplayContext displayCtx, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, Operation<Void> original) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            MultiBufferSource wrappedSource = renderType -> {
                if (renderType == Sheets.shieldSheet()) {
                    return bufferSource.getBuffer(RenderTypeFactory.translucentEntity(Sheets.SHIELD_SHEET));
                }
                if (renderType == Sheets.bannerSheet()) {
                    return bufferSource.getBuffer(RenderTypeFactory.translucentEntity(Sheets.BANNER_SHEET));
                }
                return bufferSource.getBuffer(renderType);
            };
            original.call(instance, itemStack, displayCtx, poseStack, wrappedSource, light, overlay);
        } else {
            original.call(instance, itemStack, displayCtx, poseStack, bufferSource, light, overlay);
        }
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType wrapGetRenderType(ItemStack itemStack, boolean fabulous, Operation<RenderType> original) {
        RenderType type = original.call(itemStack, fabulous);
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            return RenderModifications.getTranslucentItemRenderType(ctx, type);
        }
        return type;
    }

    //? if !neoforge {
    @WrapOperation(
            method = "renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V"
            )
    )
    private void wrapPutBulkData(VertexConsumer instance, PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float alpha, int light, int overlay, Operation<Void> original) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            float modifiedAlpha = alpha * RenderModifications.getTransparencyAlpha(ctx);
            original.call(instance, pose, quad, r, g, b, modifiedAlpha, light, overlay);
        } else {
            original.call(instance, pose, quad, r, g, b, alpha, light, overlay);
        }
    }
    //?}

    //? if neoforge {
    /^// NeoForge adds an extra boolean parameter to putBulkData
    @WrapOperation(
            method = "renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"
            )
    )
    private void wrapPutBulkData(VertexConsumer instance, PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float alpha, int light, int overlay, boolean useBlockLight, Operation<Void> original) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            float modifiedAlpha = alpha * RenderModifications.getTransparencyAlpha(ctx);
            original.call(instance, pose, quad, r, g, b, modifiedAlpha, light, overlay, useBlockLight);
        } else {
            original.call(instance, pose, quad, r, g, b, alpha, light, overlay, useBlockLight);
        }
    }
    ^///?}
}
*///? }

//? if >= 1.21.4 && < 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.rendering.RenderModifications;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @ModifyVariable(
            //? if >= 1.21.6
            method = "renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            //? if < 1.21.6
            //method = "renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static RenderType modifyRenderType(RenderType renderType) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            return RenderModifications.getTranslucentItemRenderType(ctx, renderType);
        }
        return renderType;
    }

    @WrapOperation(
            method = "renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;[III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V"
            )
    )
    private static void wrapPutBulkData(VertexConsumer instance, PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float alpha, int light, int overlay, Operation<Void> original) {
        var ctx = ArmorHiderClient.RENDER_CONTEXT;
        if (ctx.hasActiveModification(EquipmentSlot.OFFHAND) || ctx.hasActiveModification(EquipmentSlot.HEAD)) {
            float modifiedAlpha = alpha * RenderModifications.getTransparencyAlpha(ctx);
            original.call(instance, pose, quad, r, g, b, modifiedAlpha, light, overlay);
        } else {
            original.call(instance, pose, quad, r, g, b, alpha, light, overlay);
        }
    }
}
*///? }
