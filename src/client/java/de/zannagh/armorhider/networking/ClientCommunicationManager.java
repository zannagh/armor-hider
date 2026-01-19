package de.zannagh.armorhider.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.net.PayloadRegistry;
import de.zannagh.armorhider.netPackets.PermissionPacket;
import de.zannagh.armorhider.resources.ServerConfiguration;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Client-side communication manager.
 * Handles packet registration and events without Fabric API.
 */
public final class ClientCommunicationManager {

    public static void initClient() {
        PayloadRegistry.registerS2CHandler(ServerConfiguration.TYPE, ctx -> {
            if (!(ctx.payload() instanceof ServerConfiguration payload)) {
                return;
            }

            ArmorHider.LOGGER.info("Armor Hider received configuration from server.");
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.setServerConfig(payload);
            ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
        });

        PayloadRegistry.registerS2CHandler(PermissionPacket.TYPE, ctx -> {
            if (!(ctx.payload() instanceof PermissionPacket payload)) {
                return;
            }

            if (payload.permissionLevel >= 3) {
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = true;
            }
        });

        ClientConnectionEvents.registerJoin((handler, client) -> {
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateName(playerName);
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getLocalGameProfile().id());
            var currentConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();

            if (client.getCurrentServer() instanceof ServerData serverData) {
                try {
                    boolean isLanServer = serverData.isLan();
                    boolean isSinglePlayer = client.isSingleplayer();
                    ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = isSinglePlayer || isLanServer;
                } catch (Exception ignored) {
                    ArmorHider.LOGGER.error("Failed to set permissions for player {}.", playerName);
                }
            }

            if (!ArmorHiderClient.isClientConnectedToServer()) {
                ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = client.isSingleplayer();
            }

            ClientPacketSender.sendToServer(currentConfig);
        });
    }
}
