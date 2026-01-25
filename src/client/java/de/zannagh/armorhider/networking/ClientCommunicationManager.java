package de.zannagh.armorhider.networking;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
//? if >= 1.20.5 {
import de.zannagh.armorhider.net.PayloadRegistry;
//?}
//? if < 1.20.5 {
/*import de.zannagh.armorhider.net.LegacyPacketHandler;
*///?}
import de.zannagh.armorhider.netPackets.PermissionPacket;
import de.zannagh.armorhider.resources.ServerConfiguration;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Client-side communication manager.
 * Handles packet registration and events without Fabric API.
 */
public final class ClientCommunicationManager {

    public static void initClient() {
        //? if >= 1.20.5 {
        PayloadRegistry.registerS2CHandler(ServerConfiguration.TYPE, ctx -> {
            if (!(ctx.payload() instanceof ServerConfiguration payload)) {
                return;
            }
            handleServerConfigReceived(payload);
        });

        PayloadRegistry.registerS2CHandler(PermissionPacket.TYPE, ctx -> {
            if (!(ctx.payload() instanceof PermissionPacket payload)) {
                return;
            }
            handlePermissionPacketReceived(payload);
        });
        //?}

        //? if < 1.20.5 {
        /*LegacyPacketHandler.registerS2CHandler(LegacyPacketHandler.getServerConfigChannel(), ctx -> {
            if (!(ctx.payload() instanceof ServerConfiguration payload)) {
                return;
            }
            handleServerConfigReceived(payload);
        });

        LegacyPacketHandler.registerS2CHandler(LegacyPacketHandler.getPermissionChannel(), ctx -> {
            if (!(ctx.payload() instanceof PermissionPacket payload)) {
                return;
            }
            handlePermissionPacketReceived(payload);
        });
        *///?}

        ClientConnectionEvents.registerJoin((handler, client) -> {
            assert client.player != null;
            var playerName = client.player.getName().getString();
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateName(playerName);
            //? if >= 1.21.9
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getLocalGameProfile().id());
            //? if < 1.21.9
            //ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getLocalGameProfile().getId());
            var currentConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();

            ServerData serverData = client.getCurrentServer();
            if (serverData != null) {
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

    private static void handleServerConfigReceived(ServerConfiguration payload) {
        ArmorHider.LOGGER.info("Armor Hider received configuration from server.");
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.setServerConfig(payload);
        ArmorHider.LOGGER.info("Armor Hider successfully set configuration from server.");
    }

    private static void handlePermissionPacketReceived(PermissionPacket payload) {
        if (payload.permissionLevel >= 3) {
            ArmorHiderClient.isCurrentPlayerSinglePlayerHostOrAdmin = true;
        }
    }
}
