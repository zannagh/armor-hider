//? if >= 1.21.9 {
/*package de.zannagh.armorhider.client.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.renderer.SubmitNodeCollection;
//? if < 26.2-1.pre
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//? if <= 26.1.2 {
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
//?}
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.RenderType;

//? if >= 26.2-1.pre {
/^import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
^///?}

//? if >= 26.3-0.snapshot.2
//import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectorMixin {

    //? if <= 26.1.2 {
    @WrapOperation(
            method = "submitModelPart",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
            )
    )
    private void wrapModelPartAdd(ModelPartFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit, Operation<Void> original) {

        // Only check OFFHAND scope here. HEAD scope is handled directly by SkullBlockRenderMixin
        // (which targets the skull-specific path). Including HEAD here would leak the helmet/skull
        // opacity onto every model-part submission while CustomHeadLayer's HEAD scope is open —
        // including ElytraTrims's trim layers submitted under the same scope bracket.
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (!ctx.isEmpty()) {
            var modApi = ctx.renderModificationApi();
            float alpha = modApi.getTransparencyAlpha();

            SubmitNodeStorage.ModelPartSubmit modified = getModelPartSubmit(submit, modApi.colors().scaleAlpha(submit.tintedColor(), alpha));

            RenderType translucentType = renderType;
            if (submit.sprite() != null) {
                translucentType =
                        modApi.renderTypes().getTranslucentEntityRenderType(submit.sprite().atlasLocation());
            }

            original.call(storage, translucentType, modified);
        } else {
            original.call(storage, renderType, submit);
        }
    }

    @Unique
    private static SubmitNodeStorage.@NonNull ModelPartSubmit getModelPartSubmit(SubmitNodeStorage.ModelPartSubmit submit, int modifiedColor) {
        return new SubmitNodeStorage.ModelPartSubmit(
                submit.pose(), submit.modelPart(), submit.lightCoords(), submit.overlayCoords(),
                submit.sprite(), submit.sheeted(), submit.hasFoil(),
                modifiedColor,
                submit.crumblingOverlay(), submit.outlineColor()
        );
    }
    //?}

    //? if < 26.2-1.pre {
    @WrapOperation(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
            )
    )

    private <S> void wrapModelAdd(ModelFeatureRenderer.Storage storage, RenderType renderType, SubmitNodeStorage.ModelSubmit<S> submit, Operation<Void> original) {

        // OFFHAND scope only — see comment on wrapModelPartAdd above. HEAD scope leaks onto
        // unrelated submissions (e.g. ElytraTrims trim layers) when a helmet is worn.
        var activeCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (activeCtx.isEmpty()) {
            original.call(storage, renderType, submit);
            return;
        }
        var modApi = activeCtx.renderModificationApi();
        float alpha = modApi.getTransparencyAlpha();
        int modifiedColor = modApi.colors().scaleAlpha(submit.tintedColor(), alpha);

        SubmitNodeStorage.ModelSubmit<S> modified = new SubmitNodeStorage.ModelSubmit<>(
                submit.pose(), submit.model(), submit.state(),
                submit.lightCoords(), submit.overlayCoords(), modifiedColor,
                submit.sprite(), submit.outlineColor(), submit.crumblingOverlay()
        );

        RenderType translucentType = renderType;
        if (submit.sprite() != null) {
            translucentType =
                    modApi.renderTypes().getTranslucentEntityRenderType(submit.sprite().atlasLocation());
        }

        original.call(storage, translucentType, modified);
    }
    //?}

    //? if >= 26.2-1.pre {
    /^// Shields/skulls submit with an opaque cutout RenderType. submitModel routes via
    // RenderType.hasBlending() — opaque → solid phase (unwrapped); translucent → translucentModels
    // phase (wrapped below). When our OFFHAND/HEAD scope is active, force hasBlending=true so the
    // submit is routed through the translucent phase and wrapModelSubmit can rebuild it with a
    // translucent type + alpha-tinted color.
    @ModifyExpressionValue(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;hasBlending()Z"
            )
    )
    private boolean forceTranslucentRoute(boolean original) {
        if (original) {
            return true;
        }
        // OFFHAND only — HEAD scope can be open while CustomHeadLayer's bracket is active and
        // would erroneously force unrelated opaque models (e.g. ElytraTrims trim layers) onto
        // the translucent path with helmet-opacity alpha applied.
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (ctx.isEmpty()) {
            return false;
        }
        // Only force the translucent/OIT route when the item is actually being faded. At full
        // opacity (>=1.0) forcing the OIT route makes the item vanish — OIT weight is (1 - alpha),
        // so alpha 1.0 contributes nothing and there is no opaque copy. Leave it on the normal path.
        return armorHider$isFading(ctx.modification().transparency());
    }

    // 26.3 retyped SubmitNodeCollection.translucentModels from the concrete
    // TranslucentFeatureRenderPhase to the FeatureRenderPhase interface, so submitModel now emits
    // `invokeinterface FeatureRenderPhase.submit(SubmitNode)`. Targeting the old concrete
    // TranslucentFeatureRenderPhase.submit silently misses (the render type is never swapped to a
    // translucent one). Under "Improved Transparency" the translucentModels phase IS the OIT phase,
    // so a forced-but-unswapped opaque offhand item (e.g. a shield's entity_solid) reaches
    // PreparedRenderType.drawFromBufferOit with no OIT pipelines and hard-crashes. Target the
    // interface invoke instead so the swap runs on 26.3.
    //? if >= 26.3-0.snapshot.2 {
    /^¹@WrapOperation(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"
            )
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void wrapModelSubmit(FeatureRenderPhase phase, SubmitNode submit, Operation<Void> original) {
        if (!(submit instanceof ModelFeatureRenderer.Submit<?> modelSubmit)) {
            original.call(phase, submit);
            return;
        }
        // OFFHAND only — see comment on forceTranslucentRoute.
        var activeCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (activeCtx.isEmpty() || !armorHider$isFading(activeCtx.modification().transparency())) {
            original.call(phase, submit);
            return;
        }
        var modApi = activeCtx.renderModificationApi();
        float alpha = modApi.getTransparencyAlpha();
        int modifiedColor = modApi.colors().scaleAlpha(modelSubmit.tintedColor(), alpha);

        RenderType translucentType = modelSubmit.renderType();
        // 26.3: Submit stores a UvMapping instead of a TextureAtlasSprite; narrow to recover the atlas.
        if (modelSubmit.uvMapping() instanceof TextureAtlasSprite sprite) {
            translucentType = modApi.renderTypes().getTranslucentEntityRenderType(sprite.atlasLocation());
        }

        var modified = new ModelFeatureRenderer.Submit(
                translucentType, modelSubmit.pose(), modelSubmit.model(), modelSubmit.state(),
                modelSubmit.lightCoords(), modelSubmit.overlayCoords(), modifiedColor,
                modelSubmit.uvMapping(), modelSubmit.sheetedDecalPose()
        );

        original.call(phase, (SubmitNode) modified);
    }
    ¹^///?} else {
    @WrapOperation(
            method = "submitModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/TranslucentSubmit;)V"
            )
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void wrapModelSubmit(TranslucentFeatureRenderPhase phase, TranslucentSubmit submit, Operation<Void> original) {
        if (!(submit instanceof ModelFeatureRenderer.Submit<?> modelSubmit)) {
            original.call(phase, submit);
            return;
        }
        // OFFHAND only — see comment on forceTranslucentRoute.
        var activeCtx = AhRenderManagementApi.getActiveScope(RenderScope.OFFHAND);
        if (activeCtx.isEmpty() || !armorHider$isFading(activeCtx.modification().transparency())) {
            original.call(phase, submit);
            return;
        }
        var modApi = activeCtx.renderModificationApi();
        float alpha = modApi.getTransparencyAlpha();
        int modifiedColor = modApi.colors().scaleAlpha(modelSubmit.tintedColor(), alpha);

        RenderType translucentType = modelSubmit.renderType();
        if (modelSubmit.sprite() != null) {
            translucentType = modApi.renderTypes().getTranslucentEntityRenderType(modelSubmit.sprite().atlasLocation());
        }

        var modified = new ModelFeatureRenderer.Submit(
                translucentType, modelSubmit.pose(), modelSubmit.model(), modelSubmit.state(),
                modelSubmit.lightCoords(), modelSubmit.overlayCoords(), modifiedColor,
                modelSubmit.sprite(), modelSubmit.sheetedDecalPose()
        );

        original.call(phase, (TranslucentSubmit) modified);
    }
    //?}

    // Only rebuild the offhand submit as translucent when it is genuinely being faded. At full
    // opacity forcing the OIT/translucent route makes the item invisible (OIT weight = 1 - alpha).
    @Unique
    private static boolean armorHider$isFading(double transparency) {
        return transparency > 0.0 && transparency < 1.0;
    }
    ^///?}
}
*///? }
