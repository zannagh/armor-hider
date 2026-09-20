//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.compat.elytratrims;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.zannagh.armorhider.client.compat.ElytraTrimsFade;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// 26.3 swapped submitModel's TextureAtlasSprite slot for a UvMapping and dropped the trailing
// CrumblingOverlay, exactly as EquipmentRenderMixin tracks for the vanilla call site.
//? if >= 26.3-0.snapshot.2 {
/*import net.minecraft.client.renderer.texture.UvMapping;
*///? } else {
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? }

/**
 * Compat for ElytraTrims (ET) <b>4.9.0 and newer</b> - makes ET's custom elytra decorations respond to
 * the player's configured elytra transparency. {@link ETElytraTrimSubmitMixin} is the same hook on ET up
 * to 4.8.x, which kept the helper in {@code api.impl.ETRenderingAPIUtilsKt}; 4.9.0 deleted that class and
 * moved the helper to {@code render.ETRenderingActionsKt}, so exactly one of the two mixins binds on any
 * given install and the other is silently skipped.
 * <p>
 * The mechanism is unchanged: ET has no elytra pipeline of its own, it injects into the vanilla
 * equipment-layer renderer (wings only) and draws every decorator through one shared helper that funnels
 * into {@code OrderedSubmitNodeCollector.submitModel(...)}, threading its own ARGB {@code color}. We wrap
 * that one {@code submitModel} and, while the {@code ELYTRA} scope carries a modification, scale the
 * color's alpha so the trim fades in lockstep with the wing ({@link ElytraTrimsFade#fadeTrimColor(int)}).
 * 4.9.0 draws its trims on ET's own {@code elytraTranslucent} type, which blends that alpha directly. The
 * base elytra itself is faded separately at the {@code renderLayers} call site.
 * <p>
 * {@code @Pseudo} + {@code require = 0}: ET is optional and Kotlin (the target is the file-class
 * {@code ETRenderingActionsKt}); absent → skipped. {@code @Mixin(remap = false)} because the target is a
 * mod class, but the wrapped {@code submitModel} is a Minecraft method, so its {@code @At} keeps
 * {@code remap = true} (a no-op on the Mojmap NeoForge runtime; remapped to intermediary on Fabric).
 * 4.9.0's {@code submitToCollector} returns {@code boolean} rather than {@code void}, which the
 * name-only method selector does not care about.
 */
@Pseudo
@Mixin(targets = "dev.kikugie.elytratrims.render.ETRenderingActionsKt", remap = false)
public class ETRenderingActionsSubmitMixin {

    @WrapOperation(
            method = "submitToCollector",
            at = @At(
                    value = "INVOKE",
                    //? if >= 26.3-0.snapshot.2 {
                    /*target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V",
                    *///? } else {
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
                    //? }
                    remap = true
            ),
            require = 0
    )
    //? if >= 26.3-0.snapshot.2 {
    /*private static <S> void armorHider$fadeElytraTrim(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
                                                      PoseStack poseStack, RenderType renderType, int light, int overlay,
                                                      int color, UvMapping uvMapping, int outline,
                                                      Operation<Void> original) {
        original.call(collector, model, state, poseStack, renderType, light, overlay,
                ElytraTrimsFade.fadeTrimColor(color), uvMapping, ElytraTrimsFade.sanitizeOutline(outline));
    }
    *///? } else {
    private static <S> void armorHider$fadeElytraTrim(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
                                                      PoseStack poseStack, RenderType renderType, int light, int overlay,
                                                      int color, TextureAtlasSprite sprite, int outline,
                                                      ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                                      Operation<Void> original) {
        original.call(collector, model, state, poseStack, renderType, light, overlay,
                ElytraTrimsFade.fadeTrimColor(color), sprite, ElytraTrimsFade.sanitizeOutline(outline),
                crumblingOverlay);
    }
    //? }
}
//?}
