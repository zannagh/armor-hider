//? if >= 1.21.4 {
package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.scopes.EntityIdentityResolver;
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
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void capturePlayerIdentity(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof Player player) {
            String name = player.getDisplayName().getString();
            if (name != null && !name.isEmpty()) {
                EntityIdentityResolver.setIdentityHint(
                        new EntityIdentityResolver.Identity(name, true)
                );
            }
        } else {
            EntityIdentityResolver.clearIdentityHint();
        }
    }
}
//?}
