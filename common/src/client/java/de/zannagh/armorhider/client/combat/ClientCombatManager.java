package de.zannagh.armorhider.client.combat;

import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.net.ClientPacketSender;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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
     * - If server has combat detection disabled or not configured: use the resolved player config
     *   (respects {@code usePlayerSettingsWhenUndeterminable} for remote players without a known config)
     *
     * @param player The player entity to check.
     * @return true if combat should be logged for this player
     */
    public static boolean shouldLogCombatForPlayer(Player player) {
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        boolean serverUsesCombatDetection = serverConfig != null
                && serverConfig.serverWideSettings != null
                && serverConfig.serverWideSettings.enableCombatDetection != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();

        if (serverUsesCombatDetection) {
            return true;
        }

        var playerName = PlayerNameUtil.getPlayerName(player);
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getConfigForPlayer(playerName);
        return config.enableCombatDetection.getValue();
    }
}
