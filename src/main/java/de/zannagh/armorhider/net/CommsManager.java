// Network.java
package de.zannagh.armorhider.net;

import de.zannagh.armorhider.*;
import de.zannagh.armorhider.netPackets.AdminSettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import de.zannagh.armorhider.resources.ServerConfiguration;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class CommsManager {
    
    public static void initServer() {
        
        PayloadTypeRegistry.playC2S().register(SettingsC2SPacket.IDENTIFIER, SettingsC2SPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(SettingsS2CPacket.IDENTIFIER, SettingsS2CPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(AdminSettingsC2SPacket.IDENTIFIER, AdminSettingsC2SPacket.PACKET_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ArmorHider.LOGGER.info("Player joined with ID {}. Sending current server config to client...", handler.player.getUuidAsString());
            var p = handler.player;
            var currentConfig = ServerRuntime.store.getConfig();
            
            sendToClient(p, currentConfig);
        });
        
        ServerPlayNetworking.registerGlobalReceiver(SettingsC2SPacket.IDENTIFIER, (payload, context) ->{
            ArmorHider.LOGGER.info("Server received settings packet from {}", payload.config().playerId.getValue().toString());

            var data = payload.config();

            try {
                ServerRuntime.put(data.playerId.getValue(), data);
                ServerRuntime.store.saveCurrent();

                var currentConfig = ServerRuntime.store.getConfig();

                sendToAllClientsButSender(data.playerId.getValue(), currentConfig);
            } catch(Exception e) {
                ArmorHider.LOGGER.error("Failed to store player data!", e);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(AdminSettingsC2SPacket.IDENTIFIER, (payload, context) ->{
            ArmorHider.LOGGER.info("Server received admin settings packet.");
            var player = context.player();
            var currentPlayerPermissionLevel = context.server().getPermissionLevel(player.getPlayerConfigEntry()).getLevel().getLevel();

            if (currentPlayerPermissionLevel < 3) {
                ArmorHider.LOGGER.info("Non-admin player {} attempted to disable combat detection. Ignoring.", player.getUuidAsString());
                return;
            }
            
            ArmorHider.LOGGER.info("Admin player {} is updating server-wide combat detection to: {}", player.getUuidAsString(), payload.enableCombatDetection());
            ServerRuntime.store.setServerCombatDetection(payload.enableCombatDetection());
            
            sendToAllClientsButSender(player.getUuid(), ServerRuntime.store.getConfig());
        });
    }

    private static void sendToClient(ServerPlayerEntity player, ServerConfiguration config) {
        ServerPlayNetworking.send(player, new SettingsS2CPacket(config.getPlayerConfigs(), config.enableCombatDetection.getValue()));
    }

    private static void sendToAllClientsButSender(UUID playerId, ServerConfiguration config) {
        var players = ServerRuntime.server.getPlayerManager().getPlayerList();
        players.forEach(player -> {
            ArmorHider.LOGGER.info("Sending config to players...");
            if (!player.getUuid().equals(playerId)) {
                ServerPlayNetworking.send(player, new SettingsS2CPacket(config.getPlayerConfigs(), config.enableCombatDetection.getValue()));
            }
        });
    }
}