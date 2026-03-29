// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.combat.ClientCombatManager;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements IdentityCarrier {
    @Inject(
            method = "handleDamageEvent",
            at = @At(value = "HEAD")
    )
    private void triggerCombat(DamageSource damageSource, CallbackInfo ci) {
        Player victim = null;
        if ((Object) this instanceof AbstractClientPlayer player) {
            victim = player;
        }
        ClientCombatManager.handleCombat(ClientCombatManager::shouldLogCombatForPlayer, damageSource, victim);
    }

    @Override
    public @Nullable String armorHider$getPlayerName() {
        if ((Object) this instanceof Player player) {
            return player.getName().getString();
        }
        return null;
    }

    @Override
    public void armorHider$setPlayerName(@Nullable String name) {

    }

    @Override
    public @Nullable ItemStack armorHider$customHeadItem() {
        if ((Object) this instanceof Player player) {
            if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return player.getItemBySlot(EquipmentSlot.HEAD);
            }
        }
        return null;
    }

    @Override
    public void armorHider$setCustomHeadItem(@Nullable ItemStack item) {

    }

    @Override
    public boolean armorHider$isPlayerFlying() {
        if ((Object) this instanceof Player player) {
            return player.isFallFlying();
        }
        return false;
    }

    @Override
    public void armorHider$setPlayerFlying(boolean flying) {

    }
}
