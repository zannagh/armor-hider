//? if >= 1.21.9 && < 26.3-0.snapshot.2 {
package de.zannagh.armorhider.client.mixin.compat.elytratrims;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Compat for ElytraTrims (ET) 4.x — makes ET's custom elytra decorations respond to the player's
 * configured elytra transparency instead of the coarse full-hide-or-show the mod falls back to without
 * this hook.
 * <p>
 * ET does not own a separate elytra pipeline: it injects into the vanilla equipment-layer renderer
 * (wings only) and draws each "decorator" layer through the shared helper
 * {@code ETRenderParameters.submitToCollector(...)} → {@code OrderedSubmitNodeCollector.submitModel(...)},
 * threading its own ARGB {@code color}. We wrap that one {@code submitModel}, gated on the active
 * {@link RenderScope#ELYTRA} scope, and behave differently depending on how ET is drawing:
 * <ul>
 *   <li><b>ET draws translucent</b> (its {@code >= 26.x} builds use {@code RenderTypes.armorTranslucent}
 *       for trims) — scale {@code color}'s alpha by the scope transparency (keeping RGB so dyed
 *       color/pattern decorators keep their hue) and let ET's own translucent type blend it. The trim
 *       fades in lockstep with the wing.</li>
 *   <li><b>ET draws cutout</b> (its {@code 1.21.x} builds use {@code armorCutoutNoCull} /
 *       {@code createArmorDecalCutoutNoCull}) — a cutout sheet can't alpha-blend, and swapping it to any
 *       translucent type renders ET's {@code AtlasManager} atlas as flat blue in the world pass (a
 *       Minecraft-side limitation of translucent-rendering that atlas on those versions; it's exactly why
 *       ET itself stays on cutout there). So instead of a broken fade we drop the submit while the wing is
 *       faded — the trim hides below 100% and returns at 100%.</li>
 * </ul>
 * Translucent-vs-cutout is detected at runtime via {@link RenderType#sortOnUpload()} (translucent types
 * sort back-to-front; cutout types don't) — version-agnostic, and it degrades safely either way (a
 * misdetection only ever costs a fade-vs-hide, never the blue). The {@link RenderType#sortOnUpload()}
 * accessor is {@code RenderSetup}-era (1.21.11+); on 1.21.9/1.21.10 ET is always cutout, so we hardcode
 * that branch. The base elytra itself is faded separately at the {@code renderLayers} call site.
 * <p>
 * {@code @Pseudo} + {@code require = 0}: ET is optional and Kotlin (its API class is the file-class
 * {@code ETRenderingAPIUtilsKt}); absent → skipped. {@code @Mixin(remap = false)} because the target is a
 * mod class, but the wrapped {@code submitModel} is a Minecraft method, so its {@code @At} keeps
 * {@code remap = true} (a no-op on the Mojmap NeoForge runtime; remapped to intermediary on Fabric). The
 * {@code submitModel} descriptor matches {@code EquipmentRenderMixin}'s; the 26.3+ UvMapping form is out of
 * range, hence the upper version bound.
 */
@Pseudo
@Mixin(targets = "dev.kikugie.elytratrims.api.impl.ETRenderingAPIUtilsKt", remap = false)
public class ETElytraTrimSubmitMixin {

    @WrapOperation(
            method = "submitToCollector",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
                    remap = true
            ),
            require = 0
    )
    private static <S> void armorHider$fadeElytraTrim(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
                                                      PoseStack poseStack, RenderType renderType, int light, int overlay,
                                                      int color, TextureAtlasSprite sprite, int outline,
                                                      ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                                      Operation<Void> original) {
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ELYTRA);
        if (ctx.isEmpty() || !ctx.needsModification()) {
            original.call(collector, model, state, poseStack, renderType, light, overlay, color, sprite, outline, crumblingOverlay);
            return;
        }
        //? if >= 1.21.11 {
        boolean etDrawsTranslucent = renderType.sortOnUpload();
        //? } else {
        /*boolean etDrawsTranslucent = false; // 1.21.9/1.21.10 ET draws trims cutout
        *///?}
        if (etDrawsTranslucent) {
            // ET already blends (26.x): fade by scaling alpha, keep ET's translucent type.
            var modApi = ctx.renderModificationApi();
            int modifiedColor = modApi.colors().scaleAlpha(color, modApi.getTransparencyAlpha());
            original.call(collector, model, state, poseStack, renderType, light, overlay, modifiedColor, sprite, outline, crumblingOverlay);
        }
        // else: ET draws cutout; a translucent swap renders its atlas flat blue, so hide the trim while
        // the wing is faded by not forwarding the submit. Full opacity is handled by the early return above.
    }
}
//?}
