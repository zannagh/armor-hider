package de.zannagh.armorhider.client.mixin.compat.accessories;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.zannagh.armorhider.client.compat.AccessoryHidingCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Compat for the Accessories accessory provider ({@code io.wispforest.accessories}, by Wisp Forest) - issue
 * #246. {@code AccessoriesRenderLayer} draws each equipped accessory by calling {@code AccessoryRenderer.render}
 * once per accessory. Wrapping that call lets Armor Hider skip an accessory's render - the only way to hide one
 * generically, since the render call carries no colour/alpha to fade with.
 * <p>
 * Four render eras exist across the versions Armor Hider targets (each verified by decompiling the jars). The
 * {@code @Mixin} target class is stable ({@code AccessoriesRenderLayer}); the enclosing method, the
 * {@code AccessoryRenderer} interface package, the slot object type and the argument list all move:
 * <ul>
 *   <li><b>&lt; 1.21.2</b> (direct-entity): the call is in the public {@code render(...T extends LivingEntity...)}
 *       body; the slot object is an entity-bearing {@code SlotReference} (arg 2), 6 trailing floats.</li>
 *   <li><b>1.21.2 – 1.21.7</b> (transitional render-state): {@code render(...S render-state...)}, slot object still
 *       an entity-bearing {@code SlotReference}, one trailing float.</li>
 *   <li><b>1.21.8</b> (render-state + SlotPath): interface package moves to {@code ...api.client.renderers}; the
 *       slot object becomes an entity-free {@code SlotPath}, so the render state is used as the carrier.</li>
 *   <li><b>&gt;= 1.21.9</b> (submit): the call moved into {@code lambda$submit$0} with an {@code AccessoryRenderState}
 *       and a {@code SubmitNodeCollector}.</li>
 * </ul>
 * Below 1.21.9 only one {@code render} method exists (the {@code method_4199} bridge aside), so {@code method="render"}
 * is unambiguous. Every reference argument is {@code @Coerce Object} and the {@code @At} target is descriptor-less
 * (owner + name) so nothing needs remapping across the intermediary/mojmap split. {@code @Pseudo} + {@code require = 0}:
 * absent Accessories → skipped; a relocated dispatch degrades to a no-op instead of crashing.
 * <p>
 * Accessories renders an elytra worn in its glider slot through the vanilla {@code WingsLayer}, so that elytra is
 * handled by the ELYTRA scope in {@code EquipmentRenderMixin}, not here.
 */
@Pseudo
@Mixin(targets = "io.wispforest.accessories.client.AccessoriesRenderLayer", remap = false)
public class AccessoriesRenderLayerMixin {

    //? if >= 1.21.9 {
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
    //?}

    //? if >= 1.21.8 && < 1.21.9 {
    /*@WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/wispforest/accessories/api/client/renderers/AccessoryRenderer;render",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideAccessory(@Coerce Object renderer,
                                               @Coerce Object stack,
                                               @Coerce Object slotPath,
                                               @Coerce Object poseStack,
                                               @Coerce Object model,
                                               @Coerce Object renderState,
                                               @Coerce Object bufferSource,
                                               int light,
                                               float partialTick,
                                               Operation<Void> original) {
        // 1.21.8: SlotPath is entity-free, so the render state is the carrier.
        if (AccessoryHidingCompat.shouldHideAccessoriesBySlotPath(slotPath, renderState)) {
            return;
        }
        original.call(renderer, stack, slotPath, poseStack, model, renderState, bufferSource, light, partialTick);
    }
    *///?}

    //? if >= 1.21.2 && < 1.21.8 {
    /*@WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/wispforest/accessories/api/client/AccessoryRenderer;render",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideAccessory(@Coerce Object renderer,
                                               @Coerce Object stack,
                                               @Coerce Object slotReference,
                                               @Coerce Object poseStack,
                                               @Coerce Object model,
                                               @Coerce Object renderState,
                                               @Coerce Object bufferSource,
                                               int light,
                                               float partialTick,
                                               Operation<Void> original) {
        // 1.21.4: transitional render state, but SlotReference is still entity-bearing.
        if (AccessoryHidingCompat.shouldHideAccessoriesBySlotReference(slotReference)) {
            return;
        }
        original.call(renderer, stack, slotReference, poseStack, model, renderState, bufferSource, light, partialTick);
    }
    *///?}

    //? if < 1.21.2 {
    /*@WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/wispforest/accessories/api/client/AccessoryRenderer;render",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void armorHider$maybeHideAccessory(@Coerce Object renderer,
                                               @Coerce Object stack,
                                               @Coerce Object slotReference,
                                               @Coerce Object poseStack,
                                               @Coerce Object model,
                                               @Coerce Object bufferSource,
                                               int light,
                                               float f1,
                                               float f2,
                                               float f3,
                                               float f4,
                                               float f5,
                                               float f6,
                                               Operation<Void> original) {
        // Direct-entity era: SlotReference (arg 2) carries both the slot name and the wearer.
        if (AccessoryHidingCompat.shouldHideAccessoriesBySlotReference(slotReference)) {
            return;
        }
        original.call(renderer, stack, slotReference, poseStack, model, bufferSource, light, f1, f2, f3, f4, f5, f6);
    }
    *///?}
}
