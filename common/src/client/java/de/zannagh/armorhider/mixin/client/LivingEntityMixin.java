// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.combat.ClientCombatManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(
            method = "actuallyHurt",
            at = @At(value = "HEAD")
    )
    //? if > 1.21.1 
    private void triggerCombat(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfo ci) {
    //? if <= 1.21.1 
    //private void triggerCombat(DamageSource damageSource, float f, CallbackInfo ci) {
        if (damageSource.getEntity() == null) {
            return;
        }
        if ((Object) this instanceof LocalPlayer localPlayer) {
            ClientCombatManager.handleCombat(ClientCombatManager::shouldLogCombatForPlayer, damageSource, localPlayer);
        }
        if ((Object) this instanceof AbstractClientPlayer remotePlayer) {
            ClientCombatManager.handleCombat(ClientCombatManager::shouldLogCombatForPlayer, damageSource, remotePlayer);
        }
    }
}
