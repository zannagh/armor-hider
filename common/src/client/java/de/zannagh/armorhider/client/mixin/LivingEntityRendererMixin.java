//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.EntityIdentityResolver;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
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
 * This is needed because {@code renderState.nameTag} is null not only for the local player
 * but also for sneaking players, invisible players, and players with hidden nametags
 * (e.g. team settings). Without this hint, the identity resolver would incorrectly
 * treat all null-nameTag players as the local player.
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
        ArmorHiderClient.SCOPE_PROVIDER.enterEntityRender();
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void capturePlayerIdentity(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        // No clearing of identity hint when a non-player identity is resolved.
        // 1.21.9+ upwards extracts all render states and submits later.
        // This can cause the downstream pipeline to not find the identity hint any longer
        // if the render order changes between extraction and submission.
        // The hint is safely cleaned up by exitEntityRender() after the player's submit completes,
        // and resolve() only consumes it for player entities (instanceof AvatarRenderState check).
        // I'm leaving the boolean here to maybe use version-dependent code later down the road.
        boolean clearIdentityHintOnNonPlayerEntity = false;
        if (entity instanceof Player player) {
            player.getDisplayName();
            String name = player.getDisplayName().getString();
            if (!name.isEmpty()) {
                EntityIdentityResolver.setIdentityHint(
                        new EntityIdentityResolver.Identity(name, true)
                );
            } else if (clearIdentityHintOnNonPlayerEntity) {
                EntityIdentityResolver.clearIdentityHint();
            }
        } else if (clearIdentityHintOnNonPlayerEntity) {
            EntityIdentityResolver.clearIdentityHint();
        }
    }
}
//?}
