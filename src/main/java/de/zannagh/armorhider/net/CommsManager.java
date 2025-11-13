// Network.java
package de.zannagh.armorhider.net;

import de.zannagh.armorhider.*;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import de.zannagh.armorhider.resources.PlayerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public final class CommsManager {
    public static void initServer() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ArmorHider.LOGGER.info("Player joined with ID {}. Sending current server config to client...", handler.player.getUuidAsString());
            var p = handler.player;
            var currentConfig = ServerRuntime.store.getConfig();
            sendToClient(p, currentConfig);
        });
        
        ServerPlayNetworking.registerGlobalReceiver(SettingsC2SPacket.IDENTIFIER, (payload, context) ->{
            ArmorHider.LOGGER.info("Server received settings packet from {}", payload.config().playerId.toString());
            
            var data = payload.config();
            
            try {
                ServerRuntime.put(data.playerId, data);
                ServerRuntime.store.save();
                sendToAllClientsButSender(data.playerId, ServerRuntime.store.getConfig());
            } catch(Exception e) {
                ArmorHider.LOGGER.error("Failed to store player data!", e);
            }
        });
    }

    private static void sendToClient(ServerPlayerEntity player, List<PlayerConfig> cfg) {
        ServerPlayNetworking.send(player, new SettingsS2CPacket(cfg));
    }

    private static void sendToAllClientsButSender(UUID playerId, List<PlayerConfig> cfg) {
        var players = ServerRuntime.server.getPlayerManager().getPlayerList();
        players.forEach(player -> {
            ArmorHider.LOGGER.info("Sending config to players...");
            if (!player.getUuid().equals(playerId)) {
                ServerPlayNetworking.send(player, new SettingsS2CPacket(cfg));
            }
        });
    }
}