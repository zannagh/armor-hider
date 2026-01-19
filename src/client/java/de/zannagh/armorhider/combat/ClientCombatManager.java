package de.zannagh.armorhider.combat;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.common.CombatManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.Function;

public final class ClientCombatManager {
    public static void handleCombat(Function<Player, Boolean> shouldLogCombat, DamageSource damageSource, Player entity) {
        if (entity instanceof LocalPlayer player) {
            if (shouldLogCombat.apply(player)) {
                CombatManager.logCombat(player.getDisplayName().getString());
            }
        }
        else if (entity instanceof AbstractClientPlayer otherPlayer) {
            if (shouldLogCombat.apply(otherPlayer)) {
                CombatManager.logCombat(otherPlayer.getDisplayName().getString());
            }
        }

        if (damageSource.getEntity() instanceof LocalPlayer player) {
            if (shouldLogCombat.apply(player)) {
                CombatManager.logCombat(Objects.requireNonNull(player.getDisplayName()).getString());
            }
        }
        else if (damageSource.getEntity() instanceof AbstractClientPlayer otherPlayer) {
            if (shouldLogCombat.apply(otherPlayer)) {
                CombatManager.logCombat(Objects.requireNonNull(otherPlayer.getDisplayName()).getString());
            }
        }
    }

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
    public static boolean shouldLogCombatForPlayer(Player player) {
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
}
