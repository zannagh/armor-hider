package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.net.packets.CombatLogNotificationPacket;
import de.zannagh.armorhider.net.packets.PermissionPacket;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.server.ServerConnectionEvents;
import de.zannagh.armorhider.server.ServerRuntime;
import de.zannagh.armorhider.util.ServerUtil;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import de.zannagh.eunomia.networking.packets.ServerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-side networking for armor-hider, expressed against eunomia's {@link CommunicationManager}.
 * <p>
 * Registers the serverbound handlers ({@link AhPackets#PLAYER_CONFIG},
 * {@link AhPackets#SERVER_WIDE_SETTINGS}, {@link AhPackets#COMBAT_EVENT}), enables eunomia's built-in
 * capability handshake, and pushes the current server config + the player's permission level on join.
 * There is no armor-hider handshake any more - eunomia's {@code eunomia:hello}/{@code hello_ack}
 * exchange is what tells a client the server runs the mod.
 * <p>
 * Player/server resolution goes through {@link ServerRuntime} (eunomia-core exposes only the
 * authenticated {@code senderId()}), so permission checks still run through
 * {@link ServerUtil#getPermissionLevelForPlayer}.
 */
public final class ArmorHiderServerNet {

    private ArmorHiderServerNet() {
    }

    public static void init() {
        // Capability handshake: answer client HELLO probes so a client learns this server runs the mod.
        CommunicationManager.enableServerHandshake();

        // Push the current config snapshot + this player's permission level on join. No armor-hider
        // handshake is sent: eunomia's handshake already signals capability.
        ServerConnectionEvents.registerJoin(ArmorHiderServerNet::pushOnJoin);

        CommunicationManager.onServerReceive(AhPackets.PLAYER_CONFIG,
                (payload, ctx) -> handlePlayerConfigReceived(payload, ctx));
        CommunicationManager.onServerReceive(AhPackets.SERVER_WIDE_SETTINGS,
                (payload, ctx) -> handleServerWideSettingsReceived(payload, ctx));
        CommunicationManager.onServerReceive(AhPackets.COMBAT_EVENT,
                (payload, ctx) -> handleCombatLogEventReceived(payload, ctx));
    }

    private static void pushOnJoin(ServerPlayer player, MinecraftServer server) {
        ArmorHider.LOGGER.info("Player joined with ID {}. Sending current server config to client...",
                player.getStringUUID());
        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot send config to player");
            return;
        }
        var currentConfig = runtime.getStore().getConfig();
        UUID id = player.getUUID();
        CommunicationManager.sendToPlayer(id, AhPackets.SERVER_CONFIG, currentConfig);
        CommunicationManager.sendToPlayer(id, AhPackets.PERMISSION,
                new PermissionPacket(ServerUtil.getPermissionLevelForPlayer(player, server)));
    }

    private static void handleCombatLogEventReceived(CombatLogEventPacket eventPacket, ServerContext ctx) {
        if (eventPacket == null || ctx == null) {
            return;
        }
        try {
            // Re-broadcast to everyone but the authenticated sender, as a (clientbound) notification.
            var notification = new CombatLogNotificationPacket(
                    eventPacket.playerName, eventPacket.originator, eventPacket.timestamp);
            CommunicationManager.broadcastExcept(ctx.senderId(), AhPackets.COMBAT_NOTIFICATION, notification);
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to broadcast combat log event for player {}!", eventPacket.playerName, e);
        }
    }

    private static void handlePlayerConfigReceived(PlayerConfig config, ServerContext ctx) {
        ArmorHider.LOGGER.info("Server received settings packet from {}", ctx.senderId());

        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot handle player config");
            return;
        }

        try {
            runtime.put(config.playerId.getValue(), config);
            var currentConfig = runtime.getStore().getConfig();
            CommunicationManager.broadcastExcept(config.playerId.getValue(), AhPackets.SERVER_CONFIG, currentConfig);
            ServerPlayer player = resolvePlayer(ctx.senderId());
            if (player != null) {
                var permissionLevel = ServerUtil.getPermissionLevelForPlayer(player, runtime.getServer());
                CommunicationManager.sendToPlayer(ctx.senderId(), AhPackets.PERMISSION,
                        new PermissionPacket(permissionLevel));
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to store player data!", e);
        }
    }

    private static void handleServerWideSettingsReceived(ServerWideSettings payload, ServerContext ctx) {
        ArmorHider.LOGGER.info("Server received admin settings packet.");

        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot handle server settings");
            return;
        }
        ServerPlayer player = resolvePlayer(ctx.senderId());
        if (player == null) {
            ArmorHider.LOGGER.warn("Could not resolve player {} for server-wide settings update.", ctx.senderId());
            return;
        }
        MinecraftServer server = runtime.getServer();
        var currentPlayerPermissionLevel = ServerUtil.getPermissionLevelForPlayer(player, server);

        if (currentPlayerPermissionLevel < 3) {
            ArmorHider.LOGGER.info("Non-admin player {} attempted to change server settings. Ignoring.",
                    player.getStringUUID());
            return;
        }

        CommunicationManager.sendToPlayer(player.getUUID(), AhPackets.PERMISSION,
                new PermissionPacket(currentPlayerPermissionLevel));

        if (runtime.getStore().getConfig().serverWideSettings.enableCombatDetection.getValue() == payload.enableCombatDetection.getValue()
                && runtime.getStore().getConfig().serverWideSettings.forceArmorHiderOff.getValue() == payload.forceArmorHiderOff.getValue()) {
            ArmorHider.LOGGER.debug(
                    "Admin player {} attempted to update server-wide settings (combatDetection={}, forceArmorHiderOff={}), but no change detected.",
                    player.getStringUUID(),
                    payload.enableCombatDetection.getValue(),
                    payload.forceArmorHiderOff.getValue()
            );
            return;
        }

        ArmorHider.LOGGER.debug("Admin player {} is updating server-wide settings (combatDetection={}, forceArmorHiderOff={}).",
                player.getStringUUID(),
                payload.enableCombatDetection.getValue(),
                payload.forceArmorHiderOff.getValue()
        );
        runtime.getStore().setServerCombatDetection(payload.enableCombatDetection.getValue());
        runtime.getStore().setGlobalOverride(payload.forceArmorHiderOff.getValue());
        runtime.getStore().saveCurrent();
        CommunicationManager.broadcastExcept(player.getUUID(), AhPackets.SERVER_CONFIG, runtime.getStore().getConfig());
    }

    private static ServerPlayer resolvePlayer(UUID id) {
        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null || id == null) {
            return null;
        }
        return runtime.getServer().getPlayerList().getPlayer(id);
    }
}
