// Network.java
package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-side communication manager.
 * Handles packet registration and events without Fabric API.
 */
public final class CommsManager {

    public static void initServer() {
        // Register player join handler
        ServerConnectionEvents.registerJoin((player, server) -> {
            ArmorHider.LOGGER.info("Player joined with ID {}. Sending current server config to client...", player.getStringUUID());
            var currentConfig = ServerRuntime.store.getConfig();
            sendToClient(player, currentConfig);
        });

        // Register PlayerConfig handler (C2S)
        PayloadRegistry.registerC2SHandler(PlayerConfig.TYPE, ctx -> {
            if (!(ctx.payload() instanceof PlayerConfig config)) {
                return;
            }
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }

            ArmorHider.LOGGER.info("Server received settings packet from {}", serverCtx.player().getStringUUID());

            try {
                ServerRuntime.put(config.playerId.getValue(), config);
                ServerRuntime.store.saveCurrent();

                var currentConfig = ServerRuntime.store.getConfig();
                sendToAllClientsButSender(config.playerId.getValue(), currentConfig);
            } catch (Exception e) {
                ArmorHider.LOGGER.error("Failed to store player data!", e);
            }
        });

        // Register ServerWideSettings handler (C2S)
        PayloadRegistry.registerC2SHandler(ServerWideSettings.TYPE, ctx -> {
            if (!(ctx.payload() instanceof ServerWideSettings payload)) {
                return;
            }
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }

            ArmorHider.LOGGER.info("Server received admin settings packet.");
            var player = serverCtx.player();
            var currentPlayerPermissionLevel = serverCtx.server().getProfilePermissions(player.nameAndId()).level().id();

            if (currentPlayerPermissionLevel < 3) {
                ArmorHider.LOGGER.info("Non-admin player {} attempted to change server settings. Ignoring.", player.getStringUUID());
                return;
            }

            if (ServerRuntime.store.getConfig().serverWideSettings.enableCombatDetection.getValue() == payload.enableCombatDetection.getValue()
                    && ServerRuntime.store.getConfig().serverWideSettings.forceArmorHiderOff.getValue() == payload.forceArmorHiderOff.getValue()) {
                return;
            }

            ArmorHider.LOGGER.info("Admin player {} is updating server-wide combat detection to: {}", player.getStringUUID(), payload.enableCombatDetection.getValue());
            ServerRuntime.store.setServerCombatDetection(payload.enableCombatDetection.getValue());
            ServerRuntime.store.setGlobalOverride(payload.forceArmorHiderOff.getValue());
            sendToAllClientsButSender(player.getUUID(), ServerRuntime.store.getConfig());
        });
    }

    private static void sendToClient(ServerPlayer player, ServerConfiguration config) {
        PacketSender.sendToPlayer(player, config);
    }

    private static void sendToAllClientsButSender(UUID playerId, ServerConfiguration config) {
        var players = ServerRuntime.server.getPlayerList().getPlayers();
        players.forEach(player -> {
            ArmorHider.LOGGER.info("Sending config to players...");
            if (!player.getUUID().equals(playerId)) {
                PacketSender.sendToPlayer(player, config);
            }
        });
    }
}