//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.client.scopes.IdentityStateCarrier;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the player's identity from the actual entity during {@code extractRenderState},
 * before the entity reference is lost and only the render state remains.
 * <p>
 * The identity is stored directly on the render state object via {@link IdentityCarrier},
 * so it travels with the render state from extraction to submission without any global state.
 * This avoids cross-entity contamination when multiple players are extracted before their
 * render states are submitted (which happens when entity render order changes with camera angle).
 * <p>
 * In 1.21.9+, {@code extractRenderState} and {@code EntityRenderDispatcher.submit} are
 * separate phases. The entity render scope (set by {@code EntityRenderDispatcherMixin}) only
 * starts at {@code submit}, so during extraction {@code isInEntityRender()} is false.
 * This causes {@code EquipmentSlotHidingMixin} to fire during extraction, potentially
 * hiding items from the render state before layers ever see them. We enter the entity render
 * scope at HEAD of extraction to prevent this.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    /**
     * Enters the entity render scope during {@code extractRenderState} so that
     * EquipmentSlotHidingMixin does not
     * intercept {@code getItemBySlot} calls made by vanilla during state extraction.
     * <p>
     * In 1.21.4–1.21.8 this is redundant (the scope is already entered by
     * {@code EntityRenderDispatcherMixin} which hooks the wrapping {@code render(Entity)} method),
     * but harmless — {@code enterEntityRender()} simply resets to SENTINEL.
     * In 1.21.9+ it is required because {@code submit(EntityRenderState)} is a separate call.
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("HEAD")
    )
    private void enterEntityRenderDuringExtraction(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        ArmorHiderClient.RENDER_CONTEXT.enterEntityRender();
    }

    /**
     * Captures the player's identity onto the render state object itself when rendering player entities.
     * For non-player entities, the render state's player name is left unchanged (each renderer owns its
     * own state object, so this is safe).
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void capturePlayerIdentity(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof IdentityCarrier carrier && state instanceof IdentityStateCarrier stateCarrier) {
            stateCarrier.attachCarrier(carrier);
        }
    }
}
//?}
