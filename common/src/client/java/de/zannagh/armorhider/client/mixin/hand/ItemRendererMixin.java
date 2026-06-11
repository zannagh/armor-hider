//? if < 1.21.4 {
/*package de.zannagh.armorhider.client.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
        
        var offCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx = !offCtx.isEmpty() ? offCtx : hdCtx;
        if (activeCtx.isEmpty()) {
            return bufferSource;
        }
        var mod = activeCtx.modification();
        if (mod.transparency() >= 1.0 || mod.transparency() <= 0) {
            return bufferSource;
        }
        // Wrap to swap cutout render types to translucent equivalents
        return (RenderType renderType) -> {
            if (renderType == Sheets.cutoutBlockSheet()) {
                return bufferSource.getBuffer(Sheets.translucentItemSheet());
            }
            if (renderType == Sheets.shieldSheet()) {
                return bufferSource.getBuffer(ArmorHiderRenderTypes.translucentEntity(Sheets.SHIELD_SHEET));
            }
            if (renderType == Sheets.bannerSheet()) {
                return bufferSource.getBuffer(ArmorHiderRenderTypes.translucentEntity(Sheets.BANNER_SHEET));
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
        
        if (AhRenderManagementApi.hasScopeModification(RenderScope.OFFHAND) || AhRenderManagementApi.hasScopeModification(RenderScope.HEAD)) {
            MultiBufferSource wrappedSource = renderType -> {
                if (renderType == Sheets.shieldSheet()) {
                    return bufferSource.getBuffer(ArmorHiderRenderTypes.translucentEntity(Sheets.SHIELD_SHEET));
                }
                if (renderType == Sheets.bannerSheet()) {
                    return bufferSource.getBuffer(ArmorHiderRenderTypes.translucentEntity(Sheets.BANNER_SHEET));
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
        
        var offCtx3 = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx3 = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx3 = !offCtx3.isEmpty() ? offCtx3 : hdCtx3;
        if (!activeCtx3.isEmpty()) {
            if (activeCtx3.renderModificationApi().getTranslucentItemRenderType(type) instanceof RenderType rt) {
                return rt;
            }
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
        
        var offCtx4 = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx4 = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx4 = !offCtx4.isEmpty() ? offCtx4 : hdCtx4;
        if (!activeCtx4.isEmpty()) {
            float modifiedAlpha = alpha * activeCtx4.renderModificationApi().getTransparencyAlpha();
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
        
        var offCtx5 = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx5 = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx5 = !offCtx5.isEmpty() ? offCtx5 : hdCtx5;
        if (!activeCtx5.isEmpty()) {
            float modifiedAlpha = alpha * activeCtx5.renderModificationApi().getTransparencyAlpha();
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
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
        
        var offCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx = !offCtx.isEmpty() ? offCtx : hdCtx;
        if (!activeCtx.isEmpty()) {
            if (activeCtx.renderModificationApi().getTranslucentItemRenderType(renderType) instanceof RenderType rt) {
                return rt;
            }
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
        
        var offCtx2 = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        var hdCtx2 = AhRenderManagementApi.getActiveScope(RenderScope.HEAD);
        var activeCtx2 = !offCtx2.isEmpty() ? offCtx2 : hdCtx2;
        if (!activeCtx2.isEmpty()) {
            float modifiedAlpha = alpha * activeCtx2.renderModificationApi().getTransparencyAlpha();
            original.call(instance, pose, quad, r, g, b, modifiedAlpha, light, overlay);
        } else {
            original.call(instance, pose, quad, r, g, b, alpha, light, overlay);
        }
    }
}
*///? }
