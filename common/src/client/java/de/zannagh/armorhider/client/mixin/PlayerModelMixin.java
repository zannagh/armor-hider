//? if >= 1.21.10 {
package de.zannagh.armorhider.client.mixin;
import de.zannagh.armorhider.client.compat.FantasyArmorCompat;
import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
This mixin purely exists to reset the visible setting of FantasyArmor on 1.21.10 and above in case the armor transparency is below 100%. Otherwise, the player model will have no arms.
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
    @Inject(
            method = "setupAnim(Ljava/lang/Object;)V",
            //? if fabric
            order = 1500,
            at = @At("TAIL")
    )
    private void setupAnim(Object state, CallbackInfo ci) {
        FantasyArmorCompat.resetArmVisibility((PlayerModel)(Object)this, state);
    }
}
//?}
