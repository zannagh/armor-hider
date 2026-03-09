// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.combat.ClientCombatManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
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
}
