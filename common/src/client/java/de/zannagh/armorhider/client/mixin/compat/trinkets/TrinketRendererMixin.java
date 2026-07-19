//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.compat.trinkets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.compat.AccessoryHidingCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Compat for the Trinkets accessory provider ({@code dev.emi.trinkets} — the Trinkets / Trinkets-Canary
 * mod, the most-installed Fabric accessory framework) — issue #246.
 * <p>
 * {@code TrinketFeatureRenderer} draws each equipped trinket by calling
 * {@code TrinketRenderer.render(stack, slotReference, model, pose, collector, light, state, …)} once per
 * trinket (inside the {@code lambda$render$1} it dispatches per item — verified by decompiling
 * trinkets-canary 3.11.1). Wrapping that call lets Armor Hider skip a trinket's render — the only way to
 * hide it generically, since {@code TrinketRenderer.render} carries no colour/alpha to fade with.
 * <p>
 * The body region is {@code slotReference.inventory().getSlotType().getGroup()}
 * ({@code head}/{@code chest}/{@code legs}/{@code feet}); the 7th argument is the wearer's render state.
 * The {@code @At} target is descriptor-less (matched by owner+name) so it stays correct across Minecraft
 * intermediary remaps, and every reference-type argument is {@code @Coerce Object} so no Minecraft type
 * needs remapping. {@code @Pseudo} + {@code require = 0}: absent Trinkets → skipped; if a future Trinkets
 * relocates the dispatch out of {@code lambda$render$1}, this degrades to a no-op instead of crashing.
 */
@Pseudo
@Mixin(targets = "dev.emi.trinkets.TrinketFeatureRenderer", remap = false)
public class TrinketRendererMixin {

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
}
//?}
