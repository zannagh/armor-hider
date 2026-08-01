package de.zannagh.armorhider.client.mixin.compat.curios;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.compat.AccessoryHidingCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Compat for the Curios accessory provider (issue #246). Curios draws every equipped accessory from its client
 * render layer, calling {@code ICurioRenderer.render(stack, slotContext, …)} once per curio. Wrapping that call
 * lets Armor Hider skip a curio's render - the only way to hide an accessory generically, since
 * {@code ICurioRenderer.render} exposes no colour/alpha to fade it. {@code SlotContext.identifier()} (resolved in
 * {@link AccessoryHidingCompat}) gives the accessory slot type. Because Artifacts renders through Curios on
 * NeoForge, this hook also covers Artifacts there. Curios is NeoForge-only (mojmap runtime, so the Minecraft
 * descriptors match without remapping); on Fabric the {@code @Pseudo} mixin is simply skipped.
 * <p>
 * Three render eras (verified by decompiling the jars); the layer class itself is renamed at 1.21.2:
 * <ul>
 *   <li><b>&lt; 1.21.2</b> ({@code client.render.CuriosLayer}): the call is in {@code lambda$render$0}; the wearer
 *       is not passed to {@code render}, so it is read off {@code SlotContext.entity()}.</li>
 *   <li><b>1.21.2 – 1.21.8</b> ({@code client.CuriosLayer}): the call is inline in
 *       {@code render(PoseStack, MultiBufferSource, int, LivingEntityRenderState, float, float)}; the wearer is the
 *       render state (a call argument).</li>
 *   <li><b>&gt;= 1.21.9</b>: the submit API - {@code submit(…SubmitNodeCollector…)}.</li>
 * </ul>
 * {@code @At} targets are descriptor-less (owner + name) and every reference argument is {@code @Coerce Object};
 * {@code @Pseudo} + {@code require = 0} makes a relocated dispatch a no-op instead of a crash.
 */
@Pseudo
//? if < 1.21.2
//@Mixin(targets = "top.theillusivec4.curios.client.render.CuriosLayer", remap = false)
//? if >= 1.21.2
@Mixin(targets = "top.theillusivec4.curios.client.CuriosLayer", remap = false)
public class CuriosLayerMixin {

    //? if >= 1.21.9 {
    @WrapOperation(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/client/ICurioRenderer;render", remap = false),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideCurio(@Coerce Object renderer,
                                           @Coerce Object stack,
                                           @Coerce Object slotContext,
                                           @Coerce Object poseStack,
                                           @Coerce Object collector,
                                           int light,
                                           @Coerce Object renderState,
                                           @Coerce Object renderLayerParent,
                                           @Coerce Object context,
                                           float partialA,
                                           float partialB,
                                           Operation<Void> original) {
        if (AccessoryHidingCompat.shouldHideCurio(slotContext, renderState)) {
            return;
        }
        original.call(renderer, stack, slotContext, poseStack, collector, light, renderState, renderLayerParent, context, partialA, partialB);
    }
    //?}

    //? if >= 1.21.2 && < 1.21.9 {
    /*@WrapOperation(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/client/ICurioRenderer;render", remap = false),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideCurio(@Coerce Object renderer,
                                           @Coerce Object stack,
                                           @Coerce Object slotContext,
                                           @Coerce Object poseStack,
                                           @Coerce Object bufferSource,
                                           int light,
                                           @Coerce Object renderState,
                                           @Coerce Object renderLayerParent,
                                           @Coerce Object context,
                                           float partialA,
                                           float partialB,
                                           Operation<Void> original) {
        if (AccessoryHidingCompat.shouldHideCurio(slotContext, renderState)) {
            return;
        }
        original.call(renderer, stack, slotContext, poseStack, bufferSource, light, renderState, renderLayerParent, context, partialA, partialB);
    }
    *///?}

    //? if < 1.21.2 {
    /*@WrapOperation(
            method = "lambda$render$0",
            at = @At(value = "INVOKE", target = "Ltop/theillusivec4/curios/api/client/ICurioRenderer;render", remap = false),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideCurio(@Coerce Object renderer,
                                           @Coerce Object stack,
                                           @Coerce Object slotContext,
                                           @Coerce Object poseStack,
                                           @Coerce Object renderLayerParent,
                                           @Coerce Object bufferSource,
                                           int light,
                                           float f1,
                                           float f2,
                                           float f3,
                                           float f4,
                                           float f5,
                                           float f6,
                                           Operation<Void> original) {
        // Pre-render-state: render() is handed no wearer, so it is read off SlotContext.entity().
        if (AccessoryHidingCompat.shouldHideCurioByContext(slotContext)) {
            return;
        }
        original.call(renderer, stack, slotContext, poseStack, renderLayerParent, bufferSource, light, f1, f2, f3, f4, f5, f6);
    }
    *///?}
}
