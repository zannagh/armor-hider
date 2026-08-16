package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import de.zannagh.armorhider.net.packets.HandshakePacket;
import net.minecraft.network.chat.Component;
import de.zannagh.armorhider.combat.DefaultCombatEvent;
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

    /**
     * Whether the connected server has proven it runs Armor Hider (by sending a
     * {@link HandshakePacket}). Written on the netty/client thread from the handshake handler, the
     * join handler and the disconnect handler; read off-thread by {@link ClientPacketSender}'s waiter,
     * hence {@code volatile}. Reset to {@code false} on every disconnect.
     */
    public static volatile boolean SERVER_SUPPORTS_MOD;

    public static void initClient() {
        //? if >= 1.20.5 {
        PayloadRegistry.registerS2CHandler(ServerConfiguration.TYPE, ctx -> ClientCommunicationManager.handleServerConfigReceived(ctx.payload()));
        PayloadRegistry.registerS2CHandler(PermissionPacket.TYPE, ctx -> ClientCommunicationManager.handlePermissionPacketReceived(ctx.payload()));
        PayloadRegistry.registerS2CHandler(CombatLogNotificationPacket.TYPE, ctx -> ClientCommunicationManager.handleCombatLogNotificationReceived(ctx.payload()));
        PayloadRegistry.registerS2CHandler(HandshakePacket.TYPE, ctx -> ClientCommunicationManager.handleHandshakePacketReceived(ctx.payload()));
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

        LegacyPacketHandler.registerS2CHandler(LegacyPacketHandler.getHandshakeChannel(), ctx -> {
            if (!(ctx.payload() instanceof HandshakePacket payload)) {
                return;
            }
            handleHandshakePacketReceived(payload);
        });
        *///?}

        ClientConnectionEvents.registerJoin((handler, client) -> {
            if (client.player == null) {
                return;
            }
            // Start every connection assuming the server does NOT run the mod. Relying only on the
            // disconnect handler to clear this is unsafe: a stale true (from singleplayer or an
            // incomplete disconnect) would let the gate treat the next - possibly vanilla - server as
            // supported and send it custom payloads before any handshake, risking a kick. The handshake
            // handler (or the local-server shortcut below) re-sets it to true when appropriate.
            SERVER_SUPPORTS_MOD = false;
            ClientPacketSender.reset();
            var playerName = PlayerNameUtil.getPlayerName(client.player);
            if (playerName == null || playerName.isBlank()) {
                //? if >= 1.21.9
                playerName = client.player.getGameProfile().name();
                //? if < 1.21.9
                //playerName = client.player.getGameProfile().getName();
            }
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateLocalPlayerName(playerName, java.util.Optional.of(true));
            //? if >= 1.21.9
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateLocalPlayerUuid(handler.getLocalGameProfile().id(), java.util.Optional.of(true));
            //? if < 1.21.9
            //ArmorHiderClient.CLIENT_CONFIG_MANAGER.updateLocalPlayerUuid(handler.getLocalGameProfile().getId(), java.util.Optional.of(true));
            var currentConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getLocalPlayerConfig();

            ServerData serverData = client.getCurrentServer();
            if (serverData != null) {
                try {
                    //? if >= 26.2-1.pre
                    boolean isSinglePlayer = client.hasSingleplayerServer();
                    //? if < 26.2-1.pre
                    //boolean isSinglePlayer = client.isSingleplayer();
                    if (isSinglePlayer) {
                        ArmorHiderClient.permissionLevel = 4;
                    }
                } catch (Exception ignored) {
                    ArmorHider.LOGGER.error("Failed to set permissions for player {}.", playerName);
                }
            }

            if (!McClientUtils.isClientConnectedToServer()) {
                SERVER_SUPPORTS_MOD = true;
                ArmorHiderClient.permissionLevel = 4; // local -> admin
            }

            // A send failure must never abort the join. ClientPacketSender already swallows the
            // "server doesn't know this channel" case, but the encoder can still reject an oversized
            // payload and the connection can drop between the check and the write - neither is worth
            // taking the client down for, since the config is client-authoritative anyway.
            try {
                ClientPacketSender.sendToServer(currentConfig.forNetwork());
            } catch (Exception e) {
                ArmorHider.LOGGER.warn("Could not send the local config to the server on join.", e);
            }

            // Remind the viewer if Armor Hider is disabled by their saved setting, so a persisted "off" never
            // looks like the mod silently broke. Reads the persisted flag directly (the transient keybind
            // override is cleared on disconnect, so on a fresh join the effective state equals the saved one).
            if (currentConfig.disableArmorHider.getValue()) {
                McClientUtils.showChatMessage(Component.translatable("armorhider.notice.disabled_on_join"));
            }
            //? if >= 26.2-1.pre && < 26.3-0.snapshot.2 {
            // On the version family where partial-opacity armor can't be a true translucency under a
            // shaderpack (see ShaderDitheredArmorTextures), the mod substitutes a dithered opaque render.
            // If a shaderpack is loaded and dithering is enabled, tell the viewer once per join so the
            // stipple look doesn't read as a bug and they know where to change it.
            else if (de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes.isShaderPackActive()
                    && currentConfig.irisPartialTransparencyMode.getValue()
                        != de.zannagh.armorhider.configuration.IrisPartialTransparencyMode.NONE) {
                McClientUtils.showChatMessage(Component.translatable("armorhider.notice.dithering_on_join"));
            }
            //?}
        });
        ArmorHider.LOGGER.info("Registered client-side packet handlers.");

        ClientConnectionEvents.registerDisconnect(client -> {
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearServerConfig();
            // Drop the transient keybind override so the next connection starts from the persisted baseline.
            ArmorHiderClient.CLIENT_CONFIG_MANAGER.clearSessionDisableOverride();
            ArmorHiderClient.permissionLevel = 0;
            SERVER_SUPPORTS_MOD = false;
            ClientPacketSender.reset();
        });
    }

    private static void handleHandshakePacketReceived(de.zannagh.armorhider.net.packets.HandshakePacket payload) {
        ArmorHider.LOGGER.info("Received handshake packet from session: {}", payload.sessionId);
        SERVER_SUPPORTS_MOD = true;
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
        var serverConfig = ArmorHiderClient.CLIENT_CONFIG_MANAGER.getServerConfig();
        boolean serverForces = serverConfig != null
                && serverConfig.serverWideSettings.enableCombatDetection.getValue();
        if (!serverForces) {
            var config = ArmorHiderClient.CLIENT_CONFIG_MANAGER.resolveConfig(ctx.playerName);
            if (!config.enableCombatDetection.getValue()) {
                return;
            }
        }
        ArmorHiderApi.getInstance().getCombatManagement().registerCombatEvent(new DefaultCombatEvent(ctx.playerName, ctx.timestamp));
    }
}
