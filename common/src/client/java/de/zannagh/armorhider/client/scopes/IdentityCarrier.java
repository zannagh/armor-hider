//? if >= 1.21.4 {
package de.zannagh.armorhider.client.scopes;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Duck interface injected onto {@code LivingEntityRenderState} via mixin.
 * Carries the player identity captured during {@code extractRenderState} (where the actual
 * entity is still available) through to the submission/rendering phase (where only the
 * render state remains).
 * <p>
 * This replaces the previous ThreadLocal hint approach which suffered from cross-entity
 * contamination when multiple players were extracted before their render states were submitted.
 */
public interface IdentityCarrier {
    @Nullable String armorHider$getPlayerName();
    void armorHider$setPlayerName(@Nullable String name);
    
    @Nullable ItemStack armorHider$customHeadItem();
    void armorHider$setCustomHeadItem(@Nullable ItemStack item);
    
    boolean armorHider$isPlayerFlying();
    void armorHider$setPlayerFlying(boolean flying);
}
//?}
