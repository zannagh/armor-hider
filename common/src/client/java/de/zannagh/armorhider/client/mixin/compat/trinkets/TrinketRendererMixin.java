package de.zannagh.armorhider.client.mixin.compat.trinkets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.compat.AccessoryHidingCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Compat for the Trinkets accessory provider ({@code dev.emi.trinkets} - the Trinkets / Trinkets-Canary
 * mod, the most-installed Fabric accessory framework) - issue #246.
 * <p>
 * {@code TrinketFeatureRenderer} draws each equipped trinket by calling
 * {@code TrinketRenderer.render(stack, slotReference, model, …)} once per trinket, inside a synthetic
 * lambda. Wrapping that call lets Armor Hider skip a trinket's render - the only way to hide it
 * generically, since {@code TrinketRenderer.render} carries no colour/alpha to fade with. The body region
 * is {@code slotReference.inventory().getSlotType().getGroup()} ({@code head}/{@code chest}/{@code legs}/
 * {@code feet}), resolved in {@link AccessoryHidingCompat}.
 * <p>
 * Two render eras (verified by decompiling the jars):
 * <ul>
 *   <li><b>&gt;= 1.21.9 (Trinkets-Canary 3.11.x):</b> the submit/render-state API - the dispatch is in
 *       {@code lambda$render$1} and the wearer is the {@code HumanoidRenderState} (7th arg).</li>
 *   <li><b>&lt; 1.21.9 (official Trinkets 3.7.2 on 1.20.1 / 3.10.0 on 1.21.1 - byte-identical, and the
 *       only builds that exist below 1.21.9):</b> the direct-render API - the dispatch is in
 *       {@code lambda$render$0} and the wearer is the {@code LivingEntity} itself (7th arg).</li>
 * </ul>
 * The {@code @At} target is descriptor-less (owner + name) so it stays correct across Minecraft
 * intermediary remaps, and every reference-type argument is {@code @Coerce Object} so no Minecraft type
 * needs remapping. {@code @Pseudo} + {@code require = 0}: absent Trinkets → skipped; a Trinkets that
 * relocates the dispatch degrades to a no-op instead of crashing.
 */
@Pseudo
@Mixin(targets = "dev.emi.trinkets.TrinketFeatureRenderer", remap = false)
public class TrinketRendererMixin {

    //? if >= 1.21.9 {
    @WrapOperation(
            method = "lambda$render$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/emi/trinkets/api/client/TrinketRenderer;render",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideTrinket(@Coerce Object renderer,
                                             @Coerce Object stack,
                                             @Coerce Object slotReference,
                                             @Coerce Object contextModel,
                                             @Coerce Object poseStack,
                                             @Coerce Object collector,
                                             int light,
                                             @Coerce Object renderState,
                                             float limbAngle,
                                             float limbDistance,
                                             Operation<Void> original) {
        if (AccessoryHidingCompat.shouldHideTrinket(slotReference, renderState)) {
            return;
        }
        original.call(renderer, stack, slotReference, contextModel, poseStack, collector, light, renderState, limbAngle, limbDistance);
    }
    //?}

    //? if < 1.21.9 {
    /*@WrapOperation(
            method = "lambda$render$0",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/emi/trinkets/api/client/TrinketRenderer;render",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideTrinket(@Coerce Object renderer,
                                             @Coerce Object stack,
                                             @Coerce Object slotReference,
                                             @Coerce Object contextModel,
                                             @Coerce Object poseStack,
                                             @Coerce Object vertexConsumers,
                                             int light,
                                             @Coerce Object entity,
                                             float limbAngle,
                                             float limbDistance,
                                             float tickDelta,
                                             float animationProgress,
                                             float headYaw,
                                             float headPitch,
                                             Operation<Void> original) {
        // Pre-render-state: the wearer is the LivingEntity itself (Player implements IdentityCarrier).
        if (AccessoryHidingCompat.shouldHideTrinket(slotReference, entity)) {
            return;
        }
        original.call(renderer, stack, slotReference, contextModel, poseStack, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
    }
    *///?}
}
