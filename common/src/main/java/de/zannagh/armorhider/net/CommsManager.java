package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.packets.*;
import de.zannagh.armorhider.server.ServerConnectionEvents;
import de.zannagh.armorhider.server.ServerPayloadContext;
import de.zannagh.armorhider.server.ServerRuntime;
import de.zannagh.armorhider.server.ServerConfiguration;
import de.zannagh.armorhider.util.ServerUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class CommsManager {

    public static void initServer() {
        // Register player join handler
        ServerConnectionEvents.registerJoin((player, server) -> {
            ArmorHider.LOGGER.info("Player joined with ID {}. Sending current server config to client...", player.getStringUUID());
            ServerRuntime runtime = ArmorHider.getRuntime();
            if (runtime == null) {
                ArmorHider.LOGGER.warn("Runtime not initialized, cannot send config to player");
                return;
            }
            var currentConfig = runtime.getStore().getConfig();
            sendToClient(player, currentConfig);
        });

        ServerConnectionEvents.registerJoin((player, server) -> {
            int permissionLevel;
            //? if >= 1.21.11
            permissionLevel = server.getProfilePermissions(player.nameAndId()).level().id();
            //? if >= 1.21.9 && < 1.21.11
            //permissionLevel = server.getProfilePermissions(player.nameAndId());
            //? if < 1.21.9
            //permissionLevel = server.getProfilePermissions(player.getGameProfile());
            sendToClient(player, new PermissionPacket(permissionLevel));
        });

        // Register PlayerConfig handler (C2S)
        //? if >= 1.20.5 {
        PayloadRegistry.registerC2SHandler(PlayerConfig.TYPE, ctx -> {
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handlePlayerConfigReceived(ctx.payload(), serverCtx);
        });
        //?}
        //? if < 1.20.5 {
        /*LegacyPacketHandler.registerC2SHandler(LegacyPacketHandler.getPlayerConfigChannel(), ctx -> {
            if (!(ctx.payload() instanceof PlayerConfig config)) {
                return;
            }
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handlePlayerConfigReceived(config, serverCtx);
        });
        *///?}

        // Register ServerWideSettings handler (C2S)
        //? if >= 1.20.5 {
        PayloadRegistry.registerC2SHandler(ServerWideSettings.TYPE, ctx -> {
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handleServerWideSettingsReceived(ctx.payload(), serverCtx.player(), serverCtx.server());
        });
        //?}
        //? if < 1.20.5 {
        /*LegacyPacketHandler.registerC2SHandler(LegacyPacketHandler.getServerWideSettingsChannel(), ctx -> {
            if (!(ctx.payload() instanceof ServerWideSettings payload)) {
                return;
            }
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handleServerWideSettingsReceived(payload, serverCtx.player(), serverCtx.server());
        });
        *///?}
        
        // Register combat log event packet (C2S)
        //? if >= 1.20.5 {
        PayloadRegistry.registerC2SHandler(CombatLogEventPacket.TYPE, ctx -> {
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handleCombatLogEventReceived(ctx.payload(), serverCtx);
        });
        //?}
        //? if < 1.20.5 {
        /*LegacyPacketHandler.registerC2SHandler(LegacyPacketHandler.getCombatLogEventChannel(), ctx -> {
            if (!(ctx.payload() instanceof CombatLogEventPacket payload)) {
                return;
            }
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handleCombatLogEventReceived(payload, serverCtx);
        });
        *///?}
    }
    
    private static void handleCombatLogEventReceived(CombatLogEventPacket eventPacket, ServerPayloadContext ctx) {
        if (eventPacket == null) {
            return;
        }
        if (ctx == null) {
            return;
        }
        try {
            sendPackageToAllClientsButSender(eventPacket.originator, eventPacket);
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to store player data!", e);
        }
    }

    private static void handlePlayerConfigReceived(PlayerConfig config, ServerPayloadContext serverCtx) {
        ArmorHider.LOGGER.info("Server received settings packet from {}", serverCtx.player().getStringUUID());

        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot handle player config");
            return;
        }

        try {
            runtime.put(config.playerId.getValue(), config);
            var currentConfig = runtime.getStore().getConfig();
            sendToAllClientsButSender(config.playerId.getValue(), currentConfig);
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to store player data!", e);
        }
    }

    private static void handleServerWideSettingsReceived(ServerWideSettings payload, ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        ArmorHider.LOGGER.info("Server received admin settings packet.");
        var currentPlayerPermissionLevel = ServerUtil.getPermissionLevelForPlayer(player, server);

        if (currentPlayerPermissionLevel < 3) {
            ArmorHider.LOGGER.info("Non-admin player {} attempted to change server settings. Ignoring.", player.getStringUUID());
            return;
        }

        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot handle server settings");
            return;
        }

        if (runtime.getStore().getConfig().serverWideSettings.enableCombatDetection.getValue() == payload.enableCombatDetection.getValue()
                && runtime.getStore().getConfig().serverWideSettings.forceArmorHiderOff.getValue() == payload.forceArmorHiderOff.getValue()) {
            return;
        }

        ArmorHider.LOGGER.info("Admin player {} is updating server-wide combat detection to: {}", player.getStringUUID(), payload.enableCombatDetection.getValue());
        runtime.getStore().setServerCombatDetection(payload.enableCombatDetection.getValue());
        runtime.getStore().setGlobalOverride(payload.forceArmorHiderOff.getValue());
        sendToAllClientsButSender(player.getUUID(), runtime.getStore().getConfig());
    }

    private static void sendToClient(ServerPlayer player, PermissionPacket permissions) {
        PacketSender.sendToPlayer(player, permissions);
    }

    private static void sendToClient(ServerPlayer player, ServerConfiguration config) {
        PacketSender.sendToPlayer(player, config);
    }

    private static void sendToAllClientsButSender(UUID playerId, ServerConfiguration config) {
        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot broadcast config");
            return;
        }
        var players = runtime.getServer().getPlayerList().getPlayers();
        players.forEach(player -> {
            ArmorHider.LOGGER.info("Sending config to players...");
            if (!player.getUUID().equals(playerId)) {
                PacketSender.sendToPlayer(player, config);
            }
        });
    }
    
    private static void sendPackageToAllClientsButSender(UUID playerId, CombatLogEventPacket eventPacket) {
        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot broadcast config");
            return;
        }
        var players = runtime.getServer().getPlayerList().getPlayers();
        var notificationPacket = new CombatLogNotificationPacket(eventPacket.playerName, eventPacket.originator, eventPacket.timestamp);
        players.forEach(player -> {
            if (!player.getUUID().equals(playerId)) {
                PacketSender.sendToPlayer(player, notificationPacket);
            }
        });
    }
}
