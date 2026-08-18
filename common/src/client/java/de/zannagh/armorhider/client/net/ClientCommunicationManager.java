package de.zannagh.armorhider.client.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.utils.McClientUtils;
import net.minecraft.network.chat.Component;
import de.zannagh.armorhider.combat.DefaultCombatEvent;
import de.zannagh.armorhider.log.DebugLogger;
import de.zannagh.armorhider.net.AhPackets;
import de.zannagh.armorhider.net.packets.CombatLogNotificationPacket;
import de.zannagh.armorhider.net.packets.PermissionPacket;
import de.zannagh.armorhider.server.ServerConfiguration;
import de.zannagh.armorhider.util.PlayerNameUtil;
import de.zannagh.eunomia.networking.CommunicationManager;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Client-side communication manager, wired against eunomia's {@link CommunicationManager}.
 * <p>
 * The historical "server supports the mod" signal is now eunomia's capability handshake: whether the
 * server runs Armor Hider is {@code CommunicationManager.serverCapabilities().isPresent()}. Outgoing
 * C2S traffic is gated by eunomia's own send path: {@link CommunicationManager#sendToServer} defaults
 * to {@link de.zannagh.eunomia.networking.SendOptions#AFTER_SUCCESSFUL_HANDSHAKE}, so a packet is held
 * until the probe resolves and dropped if the server does not run eunomia - the gating Armor Hider's
 * bespoke {@code ClientSendGate} used to provide before it was folded into eunomia.
 */
public final class ClientCommunicationManager {

    public static void initClient() {
        CommunicationManager.onClientReceive(AhPackets.SERVER_CONFIG,
                (payload, ctx) -> handleServerConfigReceived(payload));
        CommunicationManager.onClientReceive(AhPackets.PERMISSION,
                (payload, ctx) -> handlePermissionPacketReceived(payload));
        CommunicationManager.onClientReceive(AhPackets.COMBAT_NOTIFICATION,
                (payload, ctx) -> handleCombatLogNotificationReceived(payload));

        // Consume eunomia's server-capability probe results. Outgoing C2S traffic now flows through
        // eunomia's own send path: CommunicationManager.sendToServer defaults to AFTER_SUCCESSFUL_HANDSHAKE,
        // which queues a packet until the capability probe resolves and drops it if the server does not run
        // eunomia - exactly the gating Armor Hider's bespoke ClientSendGate used to provide.
        CommunicationManager.enableClientHandshake();

        ClientConnectionEvents.registerJoin((handler, client) -> {
            if (client.player == null) {
                return;
            }
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
                ArmorHiderClient.permissionLevel = 4; // local -> admin
            }

            // Push our local config to the server. eunomia's sendToServer holds it until the capability
            // probe resolves, then delivers it if the server runs the mod and drops it otherwise, so this
            // is safe to fire on join. A send failure must never abort the join: the encoder can reject an
            // oversized payload and the connection can drop between here and the write, and neither is worth
            // taking the client down for since the config is client-authoritative.
            try {
                CommunicationManager.sendToServer(AhPackets.PLAYER_CONFIG, currentConfig.forNetwork());
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
            // eunomia's own disconnect wiring (onClientDisconnect) resets its capability probe and send
            // gate, so there is nothing connection-scoped left for Armor Hider to clear here.
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
