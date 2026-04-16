//? if >= 1.21.10 {
package de.zannagh.armorhider.client.mixin;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.scopes.IdentityCarrier;
//? if > 1.21.10
import net.minecraft.client.model.player.PlayerModel;
//? if <= 1.21.10
//import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
        resetArmVisibility(state);
    }

    @Unique
    private void resetArmVisibility(Object state) {
        if (!ArmorHiderClient.GECKOLIB_LOADED || !ArmorHiderClient.FA_LOADED) {
            return;
        }
        PlayerModel model = (PlayerModel)(Object)this;
        if (!(state instanceof IdentityCarrier carrier)) {
            return;
        }
        var mod = carrier.createModification(EquipmentSlot.CHEST, null);
        try {
            if (mod == null || !mod.shouldModify()) {
                return;
            }

            model.rightArm.visible = true;
            model.leftArm.visible = true;

            model.rightSleeve.visible = true;
            model.leftSleeve.visible = true;
        } finally {
            ArmorHiderClient.RENDER_CONTEXT.clearActiveModification();
        }
    }
}
//?}
