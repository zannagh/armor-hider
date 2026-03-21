//? if >= 1.21.4 {
package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.scopes.IdentityCarrier;
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
public class LivingEntityRenderStateMixin implements IdentityCarrier {

    @Unique
    private @Nullable String armorHider$playerName;
    
    @Unique
    private @Nullable ItemStack armorHider$customHeadItem;

    @Override
    public @Nullable String armorHider$getPlayerName() {
        return armorHider$playerName;
    }

    @Override
    public void armorHider$setPlayerName(@Nullable String name) {
        armorHider$playerName = name;
    }
    
    @Override
    public @Nullable ItemStack armorHider$customHeadItem() {
        return armorHider$customHeadItem;
    }
    
    @Override
    public void armorHider$setCustomHeadItem(@Nullable ItemStack item) {
        armorHider$customHeadItem = item;
    }
}
//?}
