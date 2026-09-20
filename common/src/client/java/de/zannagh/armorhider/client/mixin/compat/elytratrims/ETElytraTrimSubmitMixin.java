//? if >= 1.21.9 && < 26.3-0.snapshot.2 {
package de.zannagh.armorhider.client.mixin.compat.elytratrims;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.compat.ElytraTrimsFade;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Compat for ElytraTrims (ET) <b>up to 4.8.x</b> - makes ET's custom elytra decorations respond to the
 * player's configured elytra transparency instead of the coarse full-hide-or-show the mod falls back to
 * without this hook. {@link ETRenderingActionsSubmitMixin} is the same hook on ET 4.9.0+, which moved the
 * helper to a different class; exactly one of the two binds on any given install.
 * <p>
 * ET does not own a separate elytra pipeline: it injects into the vanilla equipment-layer renderer
 * (wings only) and draws each "decorator" layer through the shared helper
 * {@code ETRenderingAPIUtilsKt.submitToCollector(...)} → {@code OrderedSubmitNodeCollector.submitModel(...)},
 * threading its own ARGB {@code color}. We wrap that one {@code submitModel} and, while the
 * {@code ELYTRA} scope carries a modification, scale the color's alpha so the trim fades in lockstep
 * with the wing ({@link ElytraTrimsFade#fadeTrimColor(int)}). The base elytra itself is faded separately
 * at the {@code renderLayers} call site.
 * <p>
 * The outline argument is additionally sanitised ({@link ElytraTrimsFade#sanitizeOutline(int)}): these ET
 * builds read the wrong local out of vanilla {@code renderLayers} and pass our render-order constant on
 * as an outline color, which vanilla renders as a solid blue full-bright outline.
 * <p>
 * {@code @Pseudo} + {@code require = 0}: ET is optional and Kotlin (its API class is the file-class
 * {@code ETRenderingAPIUtilsKt}); absent → skipped. {@code @Mixin(remap = false)} because the target is a
 * mod class, but the wrapped {@code submitModel} is a Minecraft method, so its {@code @At} keeps
 * {@code remap = true} (a no-op on the Mojmap NeoForge runtime; remapped to intermediary on Fabric). The
 * {@code submitModel} descriptor matches {@code EquipmentRenderMixin}'s. No ET build carrying this class
 * exists for 26.3, hence the upper version bound.
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
        original.call(collector, model, state, poseStack, renderType, light, overlay,
                ElytraTrimsFade.fadeTrimColor(color), sprite, ElytraTrimsFade.sanitizeOutline(outline),
                crumblingOverlay);
    }
}
//?}
