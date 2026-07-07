//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.VanillaArmorTextureManager;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//?}
//? if < 1.21.9 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if >= 1.21.11 {
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if >= 26.3-0.snapshot.2
import net.minecraft.client.renderer.texture.UvMapping;
//?}

@SuppressWarnings("UnusedMixin")
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRenderMixin {

    @Unique
    private static final ThreadLocal<Boolean> armorHider$combatSingleLayer = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<ResourceKey<EquipmentAsset>> armorHider$combatAssetKey = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<EquipmentClientInfo.LayerType> armorHider$combatLayerType = new ThreadLocal<>();

    //? if >= 1.21.9
    @Unique private static final String RENDER_LAYERS_ENTRY = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;II)V";
    //? if < 1.21.9
    //@Unique private static final String RENDER_LAYERS_ENTRY = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    //? if >= 1.21.9
    @Unique private static final String RENDER_LAYERS_DETAIL = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";
    //? if < 1.21.9
    //@Unique private static final String RENDER_LAYERS_DETAIL = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/Identifier;)V";

    //? if >= 1.21.9 {
    @ModifyVariable(method = RENDER_LAYERS_DETAIL, at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int modifyRenderOrder(int value) {
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().modifyRenderPriority(value);
    }
    //?}

    //? if >= 1.21.9 {
    // In 1.21.9+, the renderLayers entry exposes the entity as a parameter, so we can drive
    // scope entry from here. Older versions don't have that parameter — HumanoidArmorLayerMixin
    // handles scope entry there instead, so the entry/reset hooks are gated to 1.21.9+.
    @Inject(method = RENDER_LAYERS_ENTRY, at = @At("HEAD"), cancellable = true)
    private <S> void interceptRender(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, CallbackInfo ci) {
        var targetScope = RenderScope.of(null, itemStack);
        var renderer = AhRenderInterceptionRegistryApi.getRenderer(targetScope);
        var result = renderer.intercept(object, targetScope == RenderScope.ELYTRA ? EquipmentSlot.CHEST : null, itemStack, ci);
        if (result.shouldCancel() || !result.shouldIntercept()) {
            return;
        }
        if (targetScope == RenderScope.ELYTRA) {
            return;
        }

        var ctx = AhRenderManagementApi.enterScope(result);
        String playerName = ctx.modification().playerName();
        if (playerName == null || !armorHider$shouldForceVanillaCombatModel(playerName)) {
            return;
        }

        if (armorHider$shouldForceVanillaCombatModel(playerName)) {
            armorHider$combatSingleLayer.set(Boolean.FALSE);
            armorHider$combatAssetKey.set(resourceKey);
            armorHider$combatLayerType.set(layerType);
        }
    }

    @Inject(method = RENDER_LAYERS_ENTRY, at = @At("RETURN"))
    private static <S> void resetContext(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, CallbackInfo ci) {
        AhRenderManagementApi.exitScopes(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        armorHider$combatSingleLayer.remove();
        armorHider$combatAssetKey.remove();
        armorHider$combatLayerType.remove();
    }
    //?}

    // Scope entry happens per-piece in HumanoidArmorLayerMixin (renderLayers has no entity
    // parameter here) — only the combat vanilla-model bookkeeping is driven from this level.
    //? if < 1.21.9 {
    /*@Inject(method = RENDER_LAYERS_ENTRY, at = @At("HEAD"))
    private void interceptRender(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model model, ItemStack itemStack, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) {
            return;
        }
        String playerName = ctx.modification().playerName();
        if (playerName != null && armorHider$shouldForceVanillaCombatModel(playerName)) {
            armorHider$combatSingleLayer.set(Boolean.FALSE);
            armorHider$combatAssetKey.set(resourceKey);
            armorHider$combatLayerType.set(layerType);
        }
    }

    @Inject(method = RENDER_LAYERS_ENTRY, at = @At("RETURN"))
    private void resetContext(CallbackInfo ci) {
        armorHider$combatSingleLayer.remove();
        armorHider$combatAssetKey.remove();
        armorHider$combatLayerType.remove();
    }
    *///?}

    @ModifyExpressionValue(
            method = RENDER_LAYERS_DETAIL,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z")
    )
    private boolean modifyGlint(boolean original) {
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().getHasFoil(original);
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? if < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/rendertype/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original,
                                              @Local(argsOnly = true) EquipmentClientInfo.LayerType layerType) {
        ResourceKey<EquipmentAsset> assetKey = armorHider$combatAssetKey.get();
        EquipmentClientInfo.LayerType combatLayerType = armorHider$combatLayerType.get();
        if (assetKey != null && combatLayerType != null) {
            Identifier vanillaTexture = VanillaArmorTextureManager.resolveVanillaEquipmentTexture(assetKey, combatLayerType);
            if (vanillaTexture != null) {
                return original.call(vanillaTexture);
            }
        }
        return armorHider$swapArmorRenderType(texture, original);
    }

    // Enchanted armor renders through RenderTypes.armorCutoutNoCullGlint (not armorCutoutNoCull),
    // which the wrap above never sees — so enchanted pieces kept an opaque cutout type and never
    // faded. Wrap the glint variant the same way so hidden enchanted armor becomes translucent too.
    //? if >= 26.3-0.snapshot.2 {
    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType modifyArmorGlintRenderLayer(Identifier texture, Operation<RenderType> original) {
        return armorHider$swapArmorRenderType(texture, original);
    }
    //?}

    @Unique
    private RenderType armorHider$swapArmorRenderType(Identifier texture, Operation<RenderType> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        if (ctx.isEmpty()) {
            return original.call(texture);
        }

        Identifier resolved = VanillaArmorTextureManager.resolveArmorTexture(ctx.modification(), texture);
        var originalType = original.call(resolved);
        return ctx.renderModificationApi().getTranslucentArmorRenderType(resolved, originalType) instanceof RenderType rt ? rt : originalType;
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3-0.snapshot.2 {
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorTrim(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? } elif >= 1.21.11 {
                    /*target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    *///? } else {
                    /*target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"*/
                    //? }
            )
    )
    // 26.3 renders trims via RenderTypes.armorTrim(texture, decal) using a per-material paletted
    // texture (the single ARMOR_TRIMS_SHEET atlas is gone). We now have the real trim texture, so
    // build the translucent type from it directly via the armor-render-type path.
    //? if >= 26.3-0.snapshot.2 {
    private RenderType modifyTrimRenderLayer(Identifier texture, boolean decal, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi();
        var originalType = original.call(texture, decal);
        if (modApi.getTranslucentArmorRenderType(texture, originalType) instanceof RenderType renderType) {
            return renderType;
        }
        return originalType;
    }
    //? } else {
    /*private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi();
        var originalType = original.call(decal);
        if (modApi.getTrimRenderLayer(decal, originalType) instanceof RenderType renderType) {
            return renderType;
        }
        return originalType;
    }
    *///? }

    //? if >= 1.21.11 {
    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3-0.snapshot.2 {
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"
                    //? } else {
                    /*target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
                    *///? }
            )
    )
    // 26.3 submitModel dropped the CrumblingOverlay arg and swapped the sprite for a UvMapping.
    //? if >= 26.3-0.snapshot.2 {
    private <S> void modifyArmorColor(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, UvMapping uvMapping, int param9, Operation<Void> original) {
        Boolean singleLayer = armorHider$combatSingleLayer.get();
        if (singleLayer != null) {
            if (singleLayer) {
                if (DebugLogger.isEnabled()) {
                    DebugLogger.log("[CombatSingleLayer] Blocked extra layer submit | renderType={}", renderType);
                }
                return;
            }
            armorHider$combatSingleLayer.set(Boolean.TRUE);
            if (DebugLogger.isEnabled()) {
                DebugLogger.log("[CombatSingleLayer] Allowed first layer submit | renderType={}", renderType);
            }
        }
        var modifiedColor = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().applyArmorTransparency(color);
        original.call(collector, model, state, poseStack, renderType, light, overlay, modifiedColor, uvMapping, param9);
    }
    //? } else {
    /*private <S> void modifyArmorColor(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite, int param9, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original) {
        Boolean singleLayer = armorHider$combatSingleLayer.get();
        if (singleLayer != null) {
            if (singleLayer) {
                if (DebugLogger.isEnabled()) {
                    DebugLogger.log("[CombatSingleLayer] Blocked extra layer submit | renderType={}", renderType);
                }
                return;
            }
            armorHider$combatSingleLayer.set(Boolean.TRUE);
            if (DebugLogger.isEnabled()) {
                DebugLogger.log("[CombatSingleLayer] Allowed first layer submit | renderType={}", renderType);
            }
        }
        var modifiedColor = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().applyArmorTransparency(color);
        original.call(collector, model, state, poseStack, renderType, light, overlay, modifiedColor, sprite, param9, crumblingOverlay);
    }
    *///? }
    //?}

    //? if >= 1.21.9 && < 1.21.11 {
    /*@WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I"
            )
    )
    private <S> int modifyArmorColor(EquipmentClientInfo.Layer layer, int i, Operation<Integer> original) {
        Boolean singleLayer = armorHider$combatSingleLayer.get();
        if (singleLayer != null) {
            if (singleLayer) { return 0; }
            armorHider$combatSingleLayer.set(Boolean.TRUE);
        }
        int originalColor = original.call(layer, i);
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().applyArmorTransparency(originalColor);
    }
    *///?}

    // NeoForge patches renderLayers and never invokes getColorForLayer, so the color is
    // modified at the renderToBuffer call itself — that call exists on both loaders.
    //? if < 1.21.9 {
    /*@WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            )
    )
    private void modifyArmorColor(Model model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color, Operation<Void> original) {
        Boolean singleLayer = armorHider$combatSingleLayer.get();
        if (singleLayer != null) {
            if (singleLayer) { return; }
            armorHider$combatSingleLayer.set(Boolean.TRUE);
        }
        int modifiedColor = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().applyArmorTransparency(color);
        original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay, modifiedColor);
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"
            )
    )
    private void modifyTrimColor(Model model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        if (ctx.isEmpty()) {
            original.call(model, poseStack, vertexConsumer, packedLight, packedOverlay);
            return;
        }
        int color = ctx.renderModificationApi().applyTransparencyFromWhite(0xFFFFFFFF);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
    *///?}
    @Unique
    private static boolean armorHider$shouldForceVanillaCombatModel(String playerName) {
        if (ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderDisabled()) {
            return false;
        }
        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
        if (!config.enableCombatDetection.getValue()) {
            var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
            if (serverConfig == null || !serverConfig.serverWideSettings.enableCombatDetection.getValue()) {
                return false;
            }
        }
        if (!ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerName)) {
            return false;
        }
        return config.inCombatUseDefaultModel.getValue();
    }
}
//?}
