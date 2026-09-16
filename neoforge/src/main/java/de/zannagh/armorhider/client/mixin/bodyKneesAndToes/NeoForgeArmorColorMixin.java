//? if >= 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.bodyKneesAndToes;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.SubmitNodeCollection;
//? if < 26.2-1.pre
//import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//? if < 26.2-1.pre
//import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 1.21.11 {
/^import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypes;
^///? }

//? if 1.21.9 || 1.21.10
//import net.minecraft.client.renderer.RenderType;

//? if >= 26.3-0.snapshot.2 {
/^import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
^///?} elif >= 26.2-1.pre {
/^import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
^///?}
// 26.3 swapped ModelFeatureRenderer.Submit's TextureAtlasSprite slot for a UvMapping (sprites still
// implement it), mirroring the submitModel change EquipmentRenderMixin tracks.
//? if >= 26.3-0.snapshot.2
//import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/^*
 * NeoForge-specific armor color transparency mixin.
 * <p>
 * On Fabric, armor color is modified upstream via {@code getColorForLayer} in renderLayers.
 * NeoForge patches renderLayers and never invokes {@code getColorForLayer}, so we handle
 * armor transparency at the SubmitNodeCollection level instead - the same approach used
 * for offhand items in {@code SubmitNodeCollectorMixin}.
 ^/
@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(SubmitNodeCollection.class)
public class NeoForgeArmorColorMixin {

    //? if < 26.2-1.pre {
    /^@WrapOperation(
            method = "submitModelPart",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    //target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
                    //? if 1.21.9 || 1.21.10
                    //target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
            )
    )
    //? if >= 1.21.11
    //private void wrapArmorModelPartAdd(ModelPartFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit, Operation<Void> original) {
    //? if 1.21.9 || 1.21.10
    //private void wrapArmorModelPartAdd(ModelPartFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit, Operation<Void> original) {
        if (shouldApplyArmorTransparency()) {

            float alpha = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().getTransparencyAlpha();

            int origColor = submit.tintedColor();
            int origAlpha = (origColor >> 24) & 0xFF;
            int newAlpha = Math.round(alpha * origAlpha);
            int modifiedColor = (origColor & 0x00FFFFFF) | (newAlpha << 24);

            SubmitNodeStorage.ModelPartSubmit modified = new SubmitNodeStorage.ModelPartSubmit(
                    submit.pose(), submit.modelPart(), submit.lightCoords(), submit.overlayCoords(),
                    submit.sprite(), submit.sheeted(), submit.hasFoil(),
                    modifiedColor,
                    submit.crumblingOverlay(), submit.outlineColor()
            );

            RenderType translucentType = renderType;
            if (submit.sprite() != null) {
                //? if >= 1.21.11
                //translucentType = RenderTypes.entityTranslucent(submit.sprite().atlasLocation());
                //? if 1.21.9 || 1.21.10
                //translucentType = RenderType.entityTranslucent(submit.sprite().atlasLocation());
            }

            original.call(storage, translucentType, modified);
        } else {
            original.call(storage, renderType, submit);
        }
    }

    @WrapOperation(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    //? if >= 1.21.11
                    //target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
                    //? if 1.21.9 || 1.21.10
                    //target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
            )
    )
    //? if >= 1.21.11
    //private <S> void wrapArmorModelAdd(ModelFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelSubmit<S> submit, Operation<Void> original) {
    //? if 1.21.9 || 1.21.10
    //private <S> void wrapArmorModelAdd(ModelFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelSubmit<S> submit, Operation<Void> original) {
        if (shouldApplyArmorTransparency()) {

            float alpha = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi().getTransparencyAlpha();

            int origColor = submit.tintedColor();
            int origAlpha = (origColor >> 24) & 0xFF;
            int newAlpha = Math.round(alpha * origAlpha);
            int modifiedColor = (origColor & 0x00FFFFFF) | (newAlpha << 24);

            SubmitNodeStorage.ModelSubmit<S> modified = new SubmitNodeStorage.ModelSubmit<>(
                    submit.pose(), submit.model(), submit.state(),
                    submit.lightCoords(), submit.overlayCoords(), modifiedColor,
                    submit.sprite(), submit.outlineColor(), submit.crumblingOverlay()
            );

            RenderType translucentType = renderType;
            if (submit.sprite() != null) {
                //? if >= 1.21.11
                //translucentType = RenderTypes.entityTranslucent(submit.sprite().atlasLocation());
                //? if 1.21.9 || 1.21.10
                //translucentType = RenderType.entityTranslucent(submit.sprite().atlasLocation());
            }

            original.call(storage, translucentType, modified);
        } else {
            original.call(storage, renderType, submit);
        }
    }
    ^///?}

    //? if >= 26.2-1.pre {
    // 26.3 retyped SubmitNodeCollection.translucentModels to the FeatureRenderPhase interface, so
    // submitModel now emits `invokeinterface FeatureRenderPhase.submit(SubmitNode)`; the concrete
    // TranslucentFeatureRenderPhase.submit(TranslucentSubmit) target of the 26.2 branch is simply absent
    // there (javap on the 26.3.0.1-beta patched jar) and the wrap silently never applied. Mirrors the
    // 26.3 branch of SubmitNodeCollectorMixin.
    @WrapOperation(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3-0.snapshot.2 {
                    /^target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"
                    ^///? } else {
                    target = "Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/TranslucentSubmit;)V"
                    //? }
            )
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    //? if >= 26.3-0.snapshot.2 {
    /^private void wrapArmorModelSubmit(FeatureRenderPhase phase, SubmitNode submit, Operation<Void> original) {
    ^///? } else {
    private void wrapArmorModelSubmit(TranslucentFeatureRenderPhase phase, TranslucentSubmit submit, Operation<Void> original) {
    //? }
        if (!(submit instanceof ModelFeatureRenderer.Submit<?> modelSubmit)) {
            original.call(phase, submit);
            return;
        }
        // NeoForge's patched EquipmentLayerRenderer.renderLayers still calls RenderTypes.armorCutoutNoCull
        // and OrderedSubmitNodeCollector.submitModel (javap, 26.2 and 26.3), so the common
        // EquipmentRenderMixin already swapped the armor's render type to Armor Hider's translucent one
        // (with its OIT/deferral/depth-write routing) and SET the colour alpha before this nested call.
        // A submit that already carries a blended type has therefore been handled: scaling its alpha
        // again here squared the fade (50% rendered as 25%) - pass it through untouched. Only a submit
        // that reaches the translucent phase with an unblended type still needs this loader-side pass.
        if (!shouldApplyArmorTransparency() || modelSubmit.renderType().hasBlending()) {
            original.call(phase, submit);
            return;
        }
        var modApi = AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA).renderModificationApi();
        int modifiedColor = modApi.colors().scaleAlpha(modelSubmit.tintedColor(), modApi.getTransparencyAlpha());

        // Route through the scope's render types (never vanilla entityTranslucent) so the piece gets the
        // same no-depth/OIT/after-terrain handling as the Fabric and FGM paths.
        RenderType translucentType = modelSubmit.renderType();
        //? if >= 26.3-0.snapshot.2 {
        /^// 26.3: Submit stores a UvMapping instead of a TextureAtlasSprite; narrow to recover the atlas.
        if (modelSubmit.uvMapping() instanceof TextureAtlasSprite sprite) {
            translucentType = modApi.renderTypes().getTranslucentEntityRenderType(sprite.atlasLocation());
        }
        ^///? } else {
        if (modelSubmit.sprite() != null) {
            translucentType = modApi.renderTypes().getTranslucentEntityRenderType(modelSubmit.sprite().atlasLocation());
        }
        //? }

        var modified = new ModelFeatureRenderer.Submit(
                translucentType, modelSubmit.pose(), modelSubmit.model(), modelSubmit.state(),
                modelSubmit.lightCoords(), modelSubmit.overlayCoords(), modifiedColor,
                //? if >= 26.3-0.snapshot.2 {
                /^modelSubmit.uvMapping(), modelSubmit.sheetedDecalPose()
                ^///? } else {
                modelSubmit.sprite(), modelSubmit.sheetedDecalPose()
                //? }
        );
        //? if >= 26.3-0.snapshot.2 {
        /^original.call(phase, (SubmitNode) modified);
        ^///? } else {
        original.call(phase, (TranslucentSubmit) modified);
        //? }
    }
    //?}

    private static boolean shouldApplyArmorTransparency() {
        if (!AhRenderManagementApi.hasScopeModification(RenderScope.ARMOR_PIECE)
                && !AhRenderManagementApi.hasScopeModification(RenderScope.ELYTRA)) {
            return false;
        }
        // An inert modification (exactly 100% opacity) must not swap submits to a translucent
        // render type - at full alpha the translucent trim loses the depth contest against the
        // opaque armor and gets overwritten.
        return AhRenderManagementApi.getActiveScope(RenderScope.ARMOR_PIECE, RenderScope.ELYTRA)
                .renderModificationApi().getTransparencyAlpha() < 1.0f;
    }
}
*///?}
