//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.ArmorHiderClientApi;
import de.zannagh.armorhider.client.api.ArmorHiderRenderApi;
import de.zannagh.armorhider.client.api.ArmorHiderRenderInterceptionRegistry;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.rendering.VanillaArmorTextureManager;
import de.zannagh.armorhider.common.ItemInfo;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.9 {
import de.zannagh.armorhider.client.common.IdentityCarrier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//?}
//? if < 1.21.9 {
/*import de.zannagh.armorhider.client.common.SlotModification;
import de.zannagh.armorhider.client.common.ScopeContext;
import de.zannagh.armorhider.client.api.implementations.RenderModifications;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if >= 1.21.11 {
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//?}

@SuppressWarnings("UnusedMixin")
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRenderMixin {

    @Unique
    private final ArmorHiderRenderApi renderApi = ArmorHiderClientApi.getInstance().getRenderingScopeApi();

    @Unique
    private final ArmorHiderRenderInterceptionRegistry rendererRegistry = ArmorHiderClientApi.getInstance().getRenderers();

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
        return renderApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().modifyRenderPriority(value);
    }
    //?}

    @Inject(method = RENDER_LAYERS_ENTRY, at = @At("HEAD"), cancellable = true)
    //? if >= 1.21.9 {
    private <S> void interceptRender(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, CallbackInfo ci) {
    //?} else {
    /*private void interceptRender(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model model, ItemStack itemStack, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
    *///?}
        var renderer = rendererRegistry.getRenderer(RenderScope.ARMOR_PIECE);
        var result = renderer.intercept(object, null, itemStack, ci);
        if (result.shouldCancel() || !result.shouldIntercept()) {
            return;
        }

        var ctx = renderApi.enterScope(result);
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
    //? if >= 1.21.9 {
    private static <S> void resetContext(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, CallbackInfo ci) {
    //?}
    //? if < 1.21.9 {
    /*private static void resetContext(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model model, ItemStack itemStack, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
    *///?}
        ArmorHiderClientApi.getInstance().getRenderingScopeApi().exitScope(RenderScope.ARMOR_PIECE);
        armorHider$combatSingleLayer.remove();
        armorHider$combatAssetKey.remove();
        armorHider$combatLayerType.remove();
    }

    @ModifyExpressionValue(
            method = RENDER_LAYERS_DETAIL,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z")
    )
    private boolean modifyGlint(boolean original) {
        return renderApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().getHasFoil(original);
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? if < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyArmorRenderLayer(Identifier texture, Operation<RenderType> original) {
        ResourceKey<EquipmentAsset> assetKey = armorHider$combatAssetKey.get();
        EquipmentClientInfo.LayerType layerType = armorHider$combatLayerType.get();
        if (assetKey != null && layerType != null) {
            Identifier vanillaTexture = VanillaArmorTextureManager.resolveVanillaEquipmentTexture(assetKey, layerType);
            if (vanillaTexture != null) {
                return original.call(vanillaTexture);
            }
        }
        var ctx = renderApi.getActiveScope(RenderScope.ARMOR_PIECE);
        if (ctx.isEmpty()) {
            return original.call(texture);
        }

        Identifier resolved = VanillaArmorTextureManager.resolveArmorTexture(ctx.modification(), texture);

        if (ctx.renderModificationApi().getTranslucentArmorRenderType(resolved, original.call(resolved)) instanceof RenderType renderType) {
            return renderType;
        }
        return original.call(resolved);
    }

    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? if < 1.21.11
                    //target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original) {
        var modApi = renderApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi();
        if (modApi.getTrimRenderLayer(decal, original.call(decal)) instanceof RenderType renderType) {
            return renderType;
        }
        return original.call(decal);
    }

    //? if >= 1.21.11 {
    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
            )
    )
    private <S> void modifyArmorColor(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite, int param9, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original) {
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
        var modifiedColor = renderApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(color);
        original.call(collector, model, state, poseStack, renderType, light, overlay, modifiedColor, sprite, param9, crumblingOverlay);
    }
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
        return renderApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(originalColor);
    }
    *///?}

    //? if < 1.21.9 {
    /*@WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I"
            )
    )
    private int modifyArmorColor(EquipmentClientInfo.Layer layer, int i, Operation<Integer> original) {
        Boolean singleLayer = armorHider$combatSingleLayer.get();
        if (singleLayer != null) {
            if (singleLayer) { return 0; }
            armorHider$combatSingleLayer.set(Boolean.TRUE);
        }
        int originalColor = original.call(layer, i);
        return renderApi.getActiveScope(RenderScope.ARMOR_PIECE).renderModificationApi().applyArmorTransparency(originalColor);
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
