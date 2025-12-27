package de.zannagh.armorhider.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.config.ClientConfigManager;
import de.zannagh.armorhider.netPackets.SettingsC2SPacket;
import de.zannagh.armorhider.netPackets.SettingsS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.chunk.PaletteType;

public final class ClientCommunicationManager {
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(SettingsS2CPacket.IDENTIFIER, (payload, context) -> {
            ArmorHider.LOGGER.info("Armor Hider received configuration from server.");

            var serverConfig = payload.getConfig();

            ClientConfigManager.setServerConfig(serverConfig);
            ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
        });

        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ClientConfigManager.updateName(playerName);
            ClientConfigManager.updateId(handler.getProfile().id());
            var currentConfig = ClientConfigManager.get();

            if (client.getServer() != null) {
                try {
                   ArmorHiderClient.IsCurrentPlayerSinglePlayerHostOrAdmin = client.getServer().getPermissionLevel(client.player.getPlayerConfigEntry()) >= 3;
                }
                catch (Exception ignored) {
                    ArmorHider.LOGGER.error("Failed to set permissions for player {}.", playerName);
                }
            }

            if (!ArmorHiderClient.IsClientConnectedToServer) {
                ArmorHiderClient.IsCurrentPlayerSinglePlayerHostOrAdmin = true;
            }
            ClientPlayNetworking.send(new SettingsC2SPacket(currentConfig));
        });
    }
}
