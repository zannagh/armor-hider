package de.zannagh.armorhider.client.combat;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.net.ClientPacketSender;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class ClientCombatManager {
    public static void handleCombat(Function<Player, Boolean> shouldLogCombat, DamageSource damageSource, @Nullable Player victim) {
        if (victim != null && shouldLogCombat.apply(victim)) {
            var victimName = PlayerNameUtil.getPlayerName(victim);
            if (victimName != null) {
                CombatManager.logCombat(victimName);
                if (Minecraft.getInstance().player != null) {
                    ClientPacketSender.sendToServer(new CombatLogEventPacket(victim, Minecraft.getInstance().player.getUUID()));
                }
            }
        }

        if (damageSource.getEntity() instanceof AbstractClientPlayer attacker && shouldLogCombat.apply(attacker)) {
            var attackerName = PlayerNameUtil.getPlayerName(attacker);
            if (attackerName != null) {
                CombatManager.logCombat(attackerName);
                if (Minecraft.getInstance().player != null) {
                    ClientPacketSender.sendToServer(new CombatLogEventPacket(attacker, Minecraft.getInstance().player.getUUID()));
                }
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
