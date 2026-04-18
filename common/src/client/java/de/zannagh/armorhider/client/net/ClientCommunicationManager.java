package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.combat.CombatManager;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.packets.CombatLogNotificationPacket;
import de.zannagh.armorhider.net.packets.PermissionPacket;
import de.zannagh.armorhider.server.ServerConfiguration;
import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.multiplayer.ServerData;

//? if >= 1.20.5 
import de.zannagh.armorhider.net.PayloadRegistry;
//? if < 1.20.5 
//import de.zannagh.armorhider.net.LegacyPacketHandler;

/**
 * Client-side communication manager.
 * Handles packet registration and events without Fabric API.
 */
public final class ClientCommunicationManager {

    public static void initClient() {
        //? if >= 1.20.5 {
        PayloadRegistry.registerS2CHandler(ServerConfiguration.TYPE, ctx -> ClientCommunicationManager.handleServerConfigReceived(ctx.payload()));
        PayloadRegistry.registerS2CHandler(PermissionPacket.TYPE, ctx -> ClientCommunicationManager.handlePermissionPacketReceived(ctx.payload()));
        PayloadRegistry.registerS2CHandler(CombatLogNotificationPacket.TYPE, ctx -> ClientCommunicationManager.handleCombatLogNotificationReceived(ctx.payload()));
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
        
        LegacyPacketHandler.registerS2CHandler(LegacyPacketHandler.getCombatLogNotificationChannel(), ctx -> {
            if (!(ctx.payload() instanceof CombatLogNotificationPacket payload)) {
                return;
            }
            handleCombatLogNotificationReceived(payload);
        });
        *///?}

        ClientConnectionEvents.registerJoin((handler, client) -> {
            assert client.player != null;
            var playerName = PlayerNameUtil.getPlayerName(client.player);
            if (playerName == null || playerName.isBlank()) {
                //? if >= 1.21.9
                playerName = client.player.getGameProfile().name();
                //? if < 1.21.9
                /*playerName = client.player.getGameProfile().getName();*/
            }
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateName(playerName);
            //? if >= 1.21.9
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getLocalGameProfile().id());
            //? if < 1.21.9
            //ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateId(handler.getLocalGameProfile().getId());
            var currentConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getValue();

            ServerData serverData = client.getCurrentServer();
            if (serverData != null) {
                try {
                    boolean isSinglePlayer = client.isSingleplayer();
                    if (isSinglePlayer) {
                        ArmorHiderClient.permissionLevel = 4;
                    } 
                } catch (Exception ignored) {
                    ArmorHider.LOGGER.error("Failed to set permissions for player {}.", playerName);
                }
            }

            if (!ArmorHiderClient.isClientConnectedToServer()) {
                ArmorHiderClient.permissionLevel = 4; // local -> admin
            }

            ClientPacketSender.sendToServer(currentConfig);
        });
    }

    private static void handleServerConfigReceived(ServerConfiguration ctx) {
        DebugLogger.log("Armor Hider received configuration from server.");
        ArmorHiderClient.CLIENT_CONFIG_MANAGER.setServerConfig(ctx);
        DebugLogger.log("Armor Hider successfully set configuration from server.");
    }

    private static void handlePermissionPacketReceived(PermissionPacket ctx) {
        DebugLogger.log("Received permission packet from server: {}", ctx.permissionLevel);
        ArmorHiderClient.permissionLevel = ctx.permissionLevel;
        
    }

    private static void handleCombatLogNotificationReceived(CombatLogNotificationPacket ctx) {
        CombatManager.logCombat(ctx.playerName, ctx.timestamp);
    }
}
