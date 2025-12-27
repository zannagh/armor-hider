// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.common.CombatManager;
import de.zannagh.armorhider.config.ClientConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
        if (damageSource.getAttacker() == null) {
            return;
        }

        if ((Object) this instanceof ClientPlayerEntity player) {
            if (shouldLogCombatForPlayer(player.getName().getString(), true)) {
                CombatManager.logCombat(player.getName().getString());
            }
        }
        if ((Object) this instanceof OtherClientPlayerEntity otherPlayer) {
            if (shouldLogCombatForPlayer(otherPlayer.getName().getString(), false)) {
                CombatManager.logCombat(otherPlayer.getName().getString());
            }
        }

        if (damageSource.getAttacker() instanceof ClientPlayerEntity player) {
            if (shouldLogCombatForPlayer(player.getName().getString(), true)) {
                CombatManager.logCombat(player.getName().getString());
            }
        }
        if (damageSource.getAttacker() instanceof OtherClientPlayerEntity otherPlayer) {
            if (shouldLogCombatForPlayer(otherPlayer.getName().getString(), false)) {
                CombatManager.logCombat(otherPlayer.getName().getString());
            }
        }
    }

    /**
     * Determines if combat should be logged for a specific player.
     * <p>
     * Logic:
     * - If server has combat detection enabled: always log combat (ignore player preference)
     * - If server has combat detection disabled: use the player's individual preference
     *
     * @param playerName The name of the player to check
     * @param isClientPlayer Whether this is the local client player
     * @return true if combat should be logged for this player
     */
    @Unique
    private static boolean shouldLogCombatForPlayer(String playerName, boolean isClientPlayer) {
        boolean serverUsesCombatDetection = ClientConfigManager.getServerConfig().enableCombatDetection;

        // In singleplayer, always use the client player's preference
        if (MinecraftClient.getInstance().isInSingleplayer()) {
            return ClientConfigManager.get().enableCombatDetection;
        }

        // If server enforces combat detection, always log combat (potential PvP advantage prevention)
        if (serverUsesCombatDetection) {
            return true;
        }

        // Server has combat detection disabled - use individual player preference
        boolean playerUsesCombatDetection;
        if (isClientPlayer) {
            playerUsesCombatDetection = ClientConfigManager.get().enableCombatDetection;
        } else {
            var playerConfig = ClientConfigManager.getServerConfig().getPlayerConfigOrDefault(playerName);
            playerUsesCombatDetection = playerConfig != null ? playerConfig.enableCombatDetection : true;
        }

        return playerUsesCombatDetection;
    }
}
