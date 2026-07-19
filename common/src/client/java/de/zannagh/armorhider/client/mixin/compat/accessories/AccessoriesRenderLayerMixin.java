//? if >= 1.21.9 {
package de.zannagh.armorhider.client.mixin.compat.accessories;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.compat.AccessoryHidingCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Compat for the Accessories accessory provider ({@code io.wispforest.accessories}, the Wisp Forest mod
 * behind Modrinth's <i>Accessories</i>) — issue #246. {@code AccessoriesRenderLayer} draws each equipped
 * accessory by calling {@code AccessoryRenderer.render(accessoryState, entityState, model, pose, collector)}
 * once per accessory, inside the {@code states.forEach(…)} lambda {@code lambda$submit$0} (the third-person
 * body layer). Wrapping that call lets Armor Hider skip an accessory's render — the only way to hide one
 * generically, since {@code AccessoryRenderer.render} carries no colour/alpha to fade with.
 * <p>
 * Only the third-person lambda is hooked: the sibling {@code lambda$submitFirstPerson$1} is a <i>static</i>
 * method (so a single non-static handler cannot target both), and the first-person pass renders only the
 * local player's hands — none of the four regions Armor Hider maps (head / chest / legs / feet) are
 * visible there, so hiding them first-person would be a no-op anyway.
 * <p>
 * The slot name is {@code accessoryState.getStateData(SLOT_PATH).slotName()} ({@code hat} / {@code necklace}
 * / {@code belt} / {@code shoes} / …, resolved and cached in {@link AccessoryHidingCompat}); the 2nd
 * argument is the wearer's render state. The {@code @At} target is descriptor-less (owner + name) so it
 * stays correct across Minecraft intermediary remaps, and every reference argument is {@code @Coerce Object}
 * so no Minecraft type needs remapping. {@code @Pseudo} + {@code require = 0}: absent Accessories → skipped;
 * a future Accessories that relocates the dispatch degrades to a no-op instead of crashing.
 * <p>
 * Accessories renders an elytra worn in its glider slot through the vanilla {@code WingsLayer}, so that
 * elytra is handled by the ELYTRA scope in {@code EquipmentRenderMixin}, not here.
 */
@Pseudo
@Mixin(targets = "io.wispforest.accessories.client.AccessoriesRenderLayer", remap = false)
public class AccessoriesRenderLayerMixin {

    @WrapOperation(
            method = "lambda$submit$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/wispforest/accessories/api/client/renderers/AccessoryRenderer;render",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideAccessory(@Coerce Object renderer,
                                               @Coerce Object accessoryState,
                                               @Coerce Object entityState,
                                               @Coerce Object model,
                                               @Coerce Object poseStack,
                                               @Coerce Object collector,
                                               Operation<Void> original) {
        if (AccessoryHidingCompat.shouldHideAccessoriesAccessory(accessoryState, entityState)) {
            return;
        }
        original.call(renderer, accessoryState, entityState, model, poseStack, collector);
    }
}
//?}
