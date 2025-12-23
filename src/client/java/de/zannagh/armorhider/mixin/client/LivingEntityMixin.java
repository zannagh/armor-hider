// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.common.CombatManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(
            method = "onDamaged",
            at = @At(value = "HEAD")
    )
    private void triggerCombat(DamageSource damageSource, CallbackInfo ci) {
        if (damageSource.getAttacker() != null) {
            if ((Object) this instanceof ClientPlayerEntity player) {
                CombatManager.logCombat(player.getName().getString());
            }
            if ((Object) this instanceof OtherClientPlayerEntity player) {
                CombatManager.logCombat(player.getName().getString());
            }
            
            if (damageSource.getAttacker() instanceof ClientPlayerEntity player) {
                CombatManager.logCombat(player.getName().getString());
            }
            if (damageSource.getAttacker() instanceof OtherClientPlayerEntity player) {
                CombatManager.logCombat(player.getName().getString());
            }
        }
        
    }
}
