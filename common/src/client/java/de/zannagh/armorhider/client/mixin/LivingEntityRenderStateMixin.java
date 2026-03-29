//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import de.zannagh.armorhider.client.scopes.IdentityStateCarrier;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Injects a player name field onto {@code LivingEntityRenderState} so that the identity
 * captured during {@code extractRenderState} travels with the render state object to the
 * submission phase — no global ThreadLocal needed.
 */
@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements IdentityStateCarrier {

    @Unique
    private @Nullable IdentityCarrier armorHider$identityCarrier;
    
    @Override
    public @Nullable String armorHider$getPlayerName() {
        return armorHider$identityCarrier != null ? armorHider$identityCarrier.armorHider$getPlayerName() : null;
    }

    public void armorHider$attachIdentityCarrier(@Nullable IdentityCarrier carrier) {
        armorHider$identityCarrier = carrier;
    }
    
    @Override
    public @Nullable ItemStack armorHider$customHeadItem() {
        return armorHider$identityCarrier != null ? armorHider$identityCarrier.armorHider$customHeadItem() : null;
    }
    
    @Override
    public boolean armorHider$isPlayerFlying() {
        return armorHider$identityCarrier != null && armorHider$identityCarrier.armorHider$isPlayerFlying();
    }
}
//?}
