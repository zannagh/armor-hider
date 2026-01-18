package de.zannagh.armorhider.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.ServerConfiguration;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.RegistryAccess;

import java.rmi.registry.Registry;


public final class ClientCommunicationManager {
    public static void initClient() {
        
        ClientPlayNetworking.registerGlobalReceiver(ServerConfiguration.PACKET_IDENTIFIER, (payload, context) -> {
            ArmorHider.LOGGER.info("Armor Hider received configuration from server.");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setServerConfig(payload);
            ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
        });

        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateName(playerName);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getLocalGameProfile().id());
            var currentConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();

            if (client.getCurrentServer() instanceof ServerData serverData) {
                try {
                    var currentPlayerPermissionLevel = 2;
                    ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = serverData.isLan(); // TODO Figure this out
                }
                catch (Exception ignored) {
                    ArmorHider.LOGGER.error("Failed to set permissions for player {}.", playerName);
                }
            }

            if (!ArmorHiderClient.isClientConnectedToServer()) {
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = true;
            }
            ClientPlayNetworking.send(currentConfig);
        });
    }
}
