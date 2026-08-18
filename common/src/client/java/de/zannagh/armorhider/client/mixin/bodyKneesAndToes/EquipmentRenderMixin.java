//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.common.VanillaRootAccessor;
import de.zannagh.armorhider.client.render.AhArmProbe;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.VanillaArmorTextureManager;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import de.zannagh.armorhider.log.DebugLogger;
import net.minecraft.client.model.Model;
//? if >= 1.21.11 {
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//?}
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
//import net.minecraft.client.renderer.texture.UvMapping;
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

    //? if >= 1.21.11 {
    /**
     * EMF/Fresh Animations models are rendered later than this equipment submission in 1.21.11+.
     * Keep the vanilla fallback with the queued draw instead of consulting the already-exited render
     * scope from EMFModelPart.render. The original model still supplies the live pose.
     */
    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> Model<? super S> armorHider$vanillaEquipmentModel(
            Model<? super S> original, boolean useVanilla) {
        if (!useVanilla || !((Object) original.root() instanceof VanillaRootAccessor accessor)) {
            return original;
        }
        ModelPart vanillaRoot = accessor.armorHider$getVanillaRoot();
        if (vanillaRoot == null) {
            return original;
        }
        if (AhArmProbe.isEnabled()) {
            AhArmProbe.recordEquipmentFallback();
        }

        // Preserve the concrete vanilla model types. EMF's deferred renderer checks for
        // HumanoidModel before replaying the animated player pose onto armor; a plain Model wrapper
        // skips that step and leaves armor pieces in stale/default poses. ElytraModel likewise owns
        // the live wing rotations used by the queued draw.
        if (original instanceof HumanoidModel<?> humanoid) {
            return (Model<? super S>) new HumanoidModel<HumanoidRenderState>(
                    vanillaRoot, original::renderType) {
                @Override
                public void setupAnim(HumanoidRenderState state) {
                    ((Model) humanoid).setupAnim(state);
                    RenderModifications.synchronisePoses(humanoid.root(), vanillaRoot);
                }
            };
        }
        if (original instanceof ElytraModel elytra) {
            return (Model<? super S>) new ElytraModel(vanillaRoot) {
                @Override
                public void setupAnim(HumanoidRenderState state) {
                    elytra.setupAnim(state);
                    RenderModifications.synchronisePoses(elytra.root(), vanillaRoot);
                }
            };
        }
        return new Model<S>(vanillaRoot, original::renderType) {
            @Override
            public void setupAnim(S state) {
                original.setupAnim(state);
                RenderModifications.synchronisePoses(original.root(), vanillaRoot);
            }
        };
    }
    //?}

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
    // scope entry from here. Older versions don't have that parameter - HumanoidArmorLayerMixin
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

    // Slot-adding mods (Elytra Slot, …) render a custom-slot elytra by calling the vanilla detail
    // renderLayers(WINGS, …) directly - bypassing the entry overload above and the outer
    // WingsLayer.submit scope entry (whose synthetic-stack decision keys off the empty chest slot).
    // The detail wraps below (render type / colour / trim) only apply transparency when an ELYTRA
    // scope is active, so those draws stayed fully opaque. Enter the ELYTRA scope here - from the
    // real render-state carrier and the real elytra stack - so hide/opacity apply deterministically,
    // regardless of injector ordering. This is a general fix: any mod drawing a slotted elytra
    // through vanilla renderLayers(WINGS, …) is covered.
    @Unique
    private static final ThreadLocal<Boolean> armorHider$enteredForeignElytraScope = new ThreadLocal<>();

    @Inject(method = RENDER_LAYERS_DETAIL, at = @At("HEAD"), cancellable = true)
    private <S> void armorHider$interceptForeignElytra(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey, Model<? super S> model, S object, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, Identifier identifier, int j, int k, CallbackInfo ci) {
        // Vanilla chest elytra already has the ELYTRA scope active from the outer WingsLayer.submit
        // HEAD, and armor pieces run under an ARMOR_PIECE scope - leave both untouched so this only
        // ever covers an otherwise-unscoped foreign elytra draw.
        if (AhRenderManagementApi.hasScopeModification(RenderScope.ELYTRA)
                || AhRenderManagementApi.hasScopeModification(RenderScope.ARMOR_PIECE)) {
            return;
        }
        if (RenderScope.of(null, itemStack) != RenderScope.ELYTRA) {
            return;
        }
        // ArmorHiderElytraRenderer handles the flying/ElytraTrims short-circuits, cancels ci on hide,
        // and enters the ELYTRA scope itself on the opacity path.
        var result = AhRenderInterceptionRegistryApi.getRenderer(RenderScope.ELYTRA)
                .intercept(object, EquipmentSlot.CHEST, itemStack, ci);
        if (result.shouldIntercept() && !result.shouldCancel()) {
            armorHider$enteredForeignElytraScope.set(Boolean.TRUE);
        }
    }

    @Inject(method = RENDER_LAYERS_DETAIL, at = @At("RETURN"))
    private void armorHider$exitForeignElytra(CallbackInfo ci) {
        if (armorHider$enteredForeignElytraScope.get() != null) {
            armorHider$enteredForeignElytraScope.remove();
            AhRenderManagementApi.exitScope(RenderScope.ELYTRA);
        }
    }
    //?}

    // Scope entry happens per-piece in HumanoidArmorLayerMixin (renderLayers has no entity
    // parameter here) - only the combat vanilla-model bookkeeping is driven from this level.
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

    // Enchanted armor renders through RenderTypes.armorCutoutNoCullGlint (not armorCutoutNoCull), which
    // the base wrap never sees, so a faded enchanted piece must be intercepted here or it never fades.
    // On 26.3 that glint type is a single combined armor+glint draw whose fragment shader clamps the
    // output alpha up to the Glint Strength setting (color.a = max(color.a, GlintAlpha)) and adds the
    // glint additively - so a translucent clone can neither fade the piece (it stays ~opaque) nor fade
    // the glint, and it has no OIT pipeline set (breaks under "Improved Transparency"). We therefore
    // drop the glint on a faded piece and route it to the plain translucent armor type, which fades
    // correctly and carries OIT. The glint intentionally vanishes on faded armor (the on/off toggle
    // still removes it at full opacity via modifyGlint/getHasFoil). Only genuine translucency takes
    // this path (needsTranslucency); a full-opacity enchanted piece keeps its vanilla fused glint.
    //? if >= 26.3-0.snapshot.2 {
    /*@WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType modifyArmorGlintRenderLayer(Identifier texture, Operation<RenderType> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        if (ctx.isEmpty() || !ctx.needsTranslucency()) {
            return original.call(texture);
        }
        return armorHider$swapArmorRenderType(texture, original);
    }
    *///?}

    // No enchantment-glint swap on 1.21.4..<26.3 (separate armorEntityGlint submit on 1.21.9+, paired
    // foil buffer on 1.21.4..1.21.8): on a faded (translucent, depth-write-disabled) base the vanilla
    // glint's EQUAL depth test fails and the glint vanishes on the faded piece - the intended behaviour.
    // The co-draw glint that re-issued it painted the whole model (chest glint spilling over the whole
    // arms), mismatched modded/texture-pack armor outlines and broke under shaders, so it was removed.
    // The glint on/off toggle still applies via modifyGlint/getHasFoil at full opacity.

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
                    /*target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorTrim(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    *///? } elif >= 1.21.11 {
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    //? } else {
                    /*target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    *///? }
            )
    )
    // 26.3 renders trims via RenderTypes.armorTrim(texture, decal) using a per-material paletted
    // texture (the single ARMOR_TRIMS_SHEET atlas is gone). We now have the real trim texture, so
    // build the translucent type from it directly via the armor-render-type path.
    //? if >= 26.3-0.snapshot.2 {
    /*private RenderType modifyTrimRenderLayer(Identifier texture, boolean decal, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi();
        var originalType = original.call(texture, decal);
        if (modApi.getTranslucentArmorRenderType(texture, originalType) instanceof RenderType renderType) {
            return renderType;
        }
        return originalType;
    }
    *///? } else {
    private RenderType modifyTrimRenderLayer(boolean decal, Operation<RenderType> original) {
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi();
        var originalType = original.call(decal);
        if (modApi.getTrimRenderLayer(decal, originalType) instanceof RenderType renderType) {
            return renderType;
        }
        return originalType;
    }
    //? }

    //? if >= 1.21.11 {
    @WrapOperation(
            method = RENDER_LAYERS_DETAIL,
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3-0.snapshot.2 {
                    /*target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"
                    *///? } else {
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
                    //? }
            )
    )
    // 26.3 submitModel dropped the CrumblingOverlay arg and swapped the sprite for a UvMapping.
    //? if >= 26.3-0.snapshot.2 {
    /*private <S> void modifyArmorColor(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, UvMapping uvMapping, int param9, Operation<Void> original) {
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
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        var modifiedColor = ctx.renderModificationApi().applyArmorTransparency(color);
        var submittedModel = armorHider$vanillaEquipmentModel(model, ctx.modification().needsTranslucency());
        original.call(collector, submittedModel, state, poseStack, renderType, light, overlay, modifiedColor, uvMapping, param9);
    }
    *///? } else {
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
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA);
        var modifiedColor = ctx.renderModificationApi().applyArmorTransparency(color);
        var submittedModel = armorHider$vanillaEquipmentModel(model, ctx.modification().needsTranslucency());
        original.call(collector, submittedModel, state, poseStack, renderType, light, overlay, modifiedColor, sprite, param9, crumblingOverlay);
    }
    //? }
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
    // modified at the renderToBuffer call itself - that call exists on both loaders.
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
        int color = ctx.renderModificationApi().applyTransparencyFromWhite();
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
    *///?}
    @Unique
    private static boolean armorHider$shouldForceVanillaCombatModel(String playerName) {
        return AhRenderManagementApi.shouldEnforceVanillaRendering(playerName);
    }
}
//?}
