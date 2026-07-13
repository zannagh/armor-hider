package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.net.ClientPacketSender;
import de.zannagh.armorhider.combat.DefaultCombatEvent;
import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side combat logic. Not part of the public API; reached through the static methods
 * on {@link de.zannagh.armorhider.client.api.AhCombatApi}.
 */
@ApiStatus.Internal
public final class AhCombatApiImpl {

    private AhCombatApiImpl() {}

    public static void handleCombat(DamageSource damageSource, @Nullable Player victim) {
        if (victim != null && shouldLogCombatForPlayer(victim)) {
            handleCombatFor(victim);
        }
        if (damageSource.getEntity() instanceof AbstractClientPlayer attacker && shouldLogCombatForPlayer(attacker)) {
            handleCombatFor(attacker);
        }
    }

    public static boolean shouldLogCombatForPlayer(Player player) {
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        boolean serverUsesCombatDetection = serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();

        if (serverUsesCombatDetection) {
            return true;
        }

        var playerName = PlayerNameUtil.getPlayerName(player);
        var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);
        return config.enableCombatDetection.getValue();
    }

    public static boolean shouldApplyCombatDetectionFor(String playerName) {
        if (playerName == null || ArmorHiderClient.CLIENT_CONFIG_MANAGER.isArmorHiderGloballyDisabled()) {
            return false;
        }

        PlayerConfig config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(playerName);

        if (config.enableCombatDetection.getValue()) {
            return true;
        }
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        return serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
    }

    private static void handleCombatFor(@NotNull Player victim) {
        var victimName = PlayerNameUtil.getPlayerName(victim);
        if (victimName != null) {
            ArmorHiderApi.getInstance().getCombatManagement().registerCombatEvent(new DefaultCombatEvent(victimName, System.currentTimeMillis()));
            if (Minecraft.getInstance().player != null) {
                ClientPacketSender.sendToServer(new CombatLogEventPacket(victim, Minecraft.getInstance().player.getUUID()));
            }
        }
    }
}
