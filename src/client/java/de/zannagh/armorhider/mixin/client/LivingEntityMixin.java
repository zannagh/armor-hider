// | --------------------------------------------------- |
// | This mechanic is inspired by Show Me Your Skin!     |
// | The source for this mod is to be found on:          |
// | https://github.com/enjarai/show-me-your-skin        |
// | --------------------------------------------------- |

package de.zannagh.armorhider.mixin.client;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.CombatManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    /**
     * Determines if combat should be logged for a specific player.
     * <p>
     * Logic:
     * - If server has combat detection enabled: always log combat (ignore player preference)
     * - If server has combat detection disabled: use the player's individual preference
     * - If server config is not available (mod not on server/older version): use player preference
     *
     * @param player The player entity to check.
     * @return true if combat should be logged for this player
     */
    @Unique
    private static boolean shouldLogCombatForPlayer(AbstractClientPlayer player) {
        boolean isClientPlayer = !(player instanceof RemotePlayer);

        // Null safety: Check if server config and serverWideSettings are available
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        boolean serverUsesCombatDetection = serverConfig != null
                && serverConfig.serverWideSettings != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();

        // If server enforces combat detection, always log combat (potential PvP advantage prevention)
        if (serverUsesCombatDetection) {
            return true;
        }

        // Server has combat detection disabled or not configured - use individual player preference
        boolean playerUsesCombatDetection;
        if (isClientPlayer) {
            playerUsesCombatDetection = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue().enableCombatDetection.getValue();
        } else {
            var playerConfig = serverConfig != null ? serverConfig.getPlayerConfigOrDefault(player) : null;
            playerUsesCombatDetection = playerConfig != null ? playerConfig.enableCombatDetection.getValue() : true;
        }

        return playerUsesCombatDetection;
    }

    @Inject(
            method = "actuallyHurt",
            at = @At(value = "HEAD")
    )
    private void triggerCombat(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfo ci) {
        if (damageSource.getEntity() == null) {
            return;
        }

        if ((Object) this instanceof LocalPlayer player) {
            if (shouldLogCombatForPlayer(player)) {
                CombatManager.logCombat(player.getDisplayName().getString());
            }
        }
        if ((Object) this instanceof AbstractClientPlayer otherPlayer) {
            if (shouldLogCombatForPlayer(otherPlayer)) {
                CombatManager.logCombat(otherPlayer.getDisplayName().getString());
            }
        }

        if (damageSource.getEntity() instanceof LocalPlayer player) {
            if (shouldLogCombatForPlayer(player)) {
                CombatManager.logCombat(Objects.requireNonNull(player.getDisplayName()).getString());
            }
        }
        if (damageSource.getEntity() instanceof AbstractClientPlayer otherPlayer) {
            if (shouldLogCombatForPlayer(otherPlayer)) {
                CombatManager.logCombat(Objects.requireNonNull(otherPlayer.getDisplayName()).getString());
            }
        }

    }
}
