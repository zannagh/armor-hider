// Network.java
package de.zannagh.armorhider.net;

import de.zannagh.armorhider.*;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class CommsManager {

    public static void initServer() {


        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ArmorHider.LOGGER.info("Player joined with ID {}. Sending current server config to client...", handler.player.getUuidAsString());
            var p = handler.player;
            var currentConfig = ServerRuntime.store.getConfig();

            sendToClient(p, currentConfig);
        });

        ServerPlayNetworking.registerGlobalReceiver(PlayerConfig.PACKET_ID, (server, player, handler, buf, responseSender) -> {
            PlayerConfig payload = PlayerConfig.read(buf);
            ArmorHider.LOGGER.info("Server received settings packet from {}", payload.playerId.getValue().toString());

            try {
                ServerRuntime.put(payload.playerId.getValue(), payload);
                ServerRuntime.store.saveCurrent();

                var currentConfig = ServerRuntime.store.getConfig();

                sendToAllClientsButSender(payload.playerId.getValue(), currentConfig);
            } catch(Exception e) {
                ArmorHider.LOGGER.error("Failed to store player data!", e);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ServerWideSettings.PACKET_ID, (server, player, handler, buf, responseSender) -> {
            ServerWideSettings payload = ServerWideSettings.read(buf);
            ArmorHider.LOGGER.info("Server received admin settings packet.");

            if (!player.hasPermissionLevel(3)) {
                ArmorHider.LOGGER.info("Non-admin player {} attempted to disable combat detection. Ignoring.", player.getUuidAsString());
                return;
            }

            ArmorHider.LOGGER.info("Admin player {} is updating server-wide combat detection to: {}", player.getUuidAsString(), payload.enableCombatDetection.getValue());
            ServerRuntime.store.setServerCombatDetection(payload.enableCombatDetection.getValue());

            sendToAllClientsButSender(player.getUuid(), ServerRuntime.store.getConfig());
        });
    }

    private static void sendToClient(ServerPlayerEntity player, ServerConfiguration config) {
        PacketByteBuf buf = PacketByteBufs.create();
        config.write(buf);
        ServerPlayNetworking.send(player, config.getPacketId(), buf);
    }

    private static void sendToAllClientsButSender(UUID playerId, ServerConfiguration config) {
        var players = ServerRuntime.server.getPlayerManager().getPlayerList();
        players.forEach(player -> {
            ArmorHider.LOGGER.info("Sending config to players...");
            if (!player.getUuid().equals(playerId)) {
                PacketByteBuf buf = PacketByteBufs.create();
                config.write(buf);
                ServerPlayNetworking.send(player, config.getPacketId(), buf);
            }
        });
    }
}