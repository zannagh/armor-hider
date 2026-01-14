// Network.java
package de.zannagh.armorhider.net;

import de.zannagh.armorhider.*;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CommsManager {
    
    public static void initServer() {
        

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ArmorHider.LOGGER.info("Player joined with ID {}. Sending current server config to client...", handler.player.getStringUUID());
            var p = handler.player;
            var currentConfig = ServerRuntime.store.getConfig();
            
            sendToClient(p, currentConfig);
        });

        assert PlayerConfig.PACKET_IDENTIFIER != null;
        ServerPlayNetworking.registerGlobalReceiver(
            PlayerConfig.empty().type(), 
            (payload, context) -> {
                if (!(payload instanceof PlayerConfig config)) {
                    return;
                }
            ArmorHider.LOGGER.info("Server received settings packet from {}", context.player().getStringUUID());

            try {
                ServerRuntime.put(config.playerId.getValue(), config);
                ServerRuntime.store.saveCurrent();

                var currentConfig = ServerRuntime.store.getConfig();

                sendToAllClientsButSender(config.playerId.getValue(), currentConfig);
            } catch(Exception e) {
                ArmorHider.LOGGER.error("Failed to store player data!", e);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ServerWideSettings.TYPE, 
                (payload, context) ->{
            ArmorHider.LOGGER.info("Server received admin settings packet.");
            var player = context.player();
            var currentPlayerPermissionLevel = context.server().getProfilePermissions(player.nameAndId()).level().id();

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
        ServerPlayNetworking.send(player, config);
    }

    private static void sendToAllClientsButSender(UUID playerId, ServerConfiguration config) {
        var players = ServerRuntime.server.getPlayerList().getPlayers();
        players.forEach(player -> {
            ArmorHider.LOGGER.info("Sending config to players...");
            if (!player.getUUID().equals(playerId)) {
                ServerPlayNetworking.send(player, config);
            }
        });
    }
}