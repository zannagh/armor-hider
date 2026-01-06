package de.zannagh.armorhider.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.resources.PlayerConfig;
import de.zannagh.armorhider.resources.ServerConfiguration;
import de.zannagh.armorhider.resources.ServerWideSettings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

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
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getProfile().id());
            var currentConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();

            if (client.getServer() != null) {
                try {
                    var currentPlayerPermissionLevel = client.getServer().getPermissionLevel(client.player.getPlayerConfigEntry()).getLevel().getLevel();
                   ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = currentPlayerPermissionLevel >= 3;
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
