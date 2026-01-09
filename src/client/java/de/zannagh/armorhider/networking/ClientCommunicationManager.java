package de.zannagh.armorhider.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ServerConfiguration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

public final class ClientCommunicationManager {
    public static void initClient() {

        ClientPlayNetworking.registerGlobalReceiver(ServerConfiguration.PACKET_ID, (client, handler, buf, responseSender) -> {
            ServerConfiguration payload = ServerConfiguration.read(buf);
            client.execute(() -> {
                ArmorHider.LOGGER.info("Armor Hider received configuration from server.");
                ArmorHiderClient.CLIENT_CONFIG_MANAGER.setServerConfig(payload);
                ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateName(playerName);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getProfile().getId());
            var currentConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();

            if (client.getServer() != null) {
                try {
                   ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = client.getServer().getPermissionLevel(client.player.getGameProfile()) >= 3;
                }
                catch (Exception ignored) {
                    ArmorHider.LOGGER.error("Failed to set permissions for player {}.", playerName);
                }
            }

            if (!ArmorHiderClient.isClientConnectedToServer()) {
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = true;
            }

            PacketByteBuf buf = PacketByteBufs.create();
            currentConfig.write(buf);
            ClientPlayNetworking.send(currentConfig.getPacketId(), buf);
        });
    }
}
