package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.packets.*;
import de.zannagh.armorhider.server.ServerConnectionEvents;
import de.zannagh.armorhider.server.ServerPayloadContext;
import de.zannagh.armorhider.server.ServerRuntime;
import de.zannagh.armorhider.server.ServerConfiguration;
import de.zannagh.armorhider.util.PlayerNameUtil;
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
            sendToClient(player, new PermissionPacket(ServerUtil.getPermissionLevelForPlayer(player, server)));
            // Shared render-rule state, both directions, strictly BEFORE the handshake: the client
            // suppresses all outgoing traffic until the handshake arrives, so doing the clearing part
            // first makes it impossible for the joiner's own fresh announcement to be wiped by it.
            handleSharedRulesOnJoin(runtime, player, server);
            sendToClient(player, new HandshakePacket());
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

        // Register shared render-rule state (C2S)
        //? if >= 1.20.5 {
        PayloadRegistry.registerC2SHandler(SharedRuleStatePacket.TYPE, ctx -> {
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handleSharedRuleStateReceived(ctx.payload(), serverCtx);
        });
        //?}
        //? if < 1.20.5 {
        /*LegacyPacketHandler.registerC2SHandler(LegacyPacketHandler.getSharedRuleStateChannel(), ctx -> {
            if (!(ctx.payload() instanceof SharedRuleStatePacket payload)) {
                return;
            }
            if (!(ctx.context() instanceof ServerPayloadContext serverCtx)) {
                return;
            }
            handleSharedRuleStateReceived(payload, serverCtx);
        });
        *///?}
    }

    /**
     * Relays one player's shared render-rule outcome to everyone else.
     *
     * <p>The sender's own {@code playerName} is discarded and replaced with the server's authoritative
     * display name, and the id with the authenticated sender UUID. Without that, any client could hide
     * another player's armor for the whole server by announcing state under their name.</p>
     */
    private static void handleSharedRuleStateReceived(SharedRuleStatePacket packet, ServerPayloadContext ctx) {
        if (packet == null || ctx == null) {
            return;
        }
        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot handle shared render rules");
            return;
        }

        ServerPlayer sender = ctx.player();
        String authoritativeName = authoritativeNameOf(sender);
        long timestamp = packet.timestamp > 0 ? packet.timestamp : System.currentTimeMillis();

        try {
            var stored = runtime.getSharedRules().put(sender.getUUID(), authoritativeName, packet.overrides, timestamp);
            if (stored == null) {
                // Identical to what this player already announced. Dropping it here is what keeps a
                // per-tick-true predicate from turning into a broadcast per tick if a client ever stops
                // diffing on its side.
                return;
            }
            // Relays what was stored, not what arrived: clamped, with malformed and no-op entries
            // already dropped, so every client applies exactly the state the server holds.
            var notification = new SharedRuleNotificationPacket(
                    authoritativeName, sender.getUUID(), stored, timestamp);
            broadcastSharedRules(runtime, sender.getUUID(), notification);
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to relay shared render rules for player {}!", authoritativeName, e);
        }
    }

    /**
     * Join-time shared-rule bookkeeping, in both directions.
     *
     * <p>The joiner's own entry is dropped and the drop announced: it belongs to their previous session,
     * possibly to a game they have since restarted without the mod that created it. Then the joiner is
     * told about everyone else, since those announcements happened before they were connected.</p>
     */
    private static void handleSharedRulesOnJoin(ServerRuntime runtime, ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        try {
            var sharedRules = runtime.getSharedRules();
            sharedRules.retainOnline(server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList());

            if (sharedRules.remove(player.getUUID())) {
                broadcastSharedRules(runtime, player.getUUID(), new SharedRuleNotificationPacket(
                        authoritativeNameOf(player), player.getUUID(), java.util.List.of(), System.currentTimeMillis()));
            }

            for (var notification : sharedRules.snapshotExcept(player.getUUID())) {
                PacketSender.sendToPlayer(player, notification);
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to synchronise shared render rules for joining player {}!",
                    player.getStringUUID(), e);
        }
    }

    /**
     * The name every other client will key this player's shared state under. Goes through
     * {@link PlayerNameUtil} like every other name resolution in the mod, with the profile name as a
     * last resort so an entry can never be stored under a blank key.
     */
    private static String authoritativeNameOf(ServerPlayer player) {
        String name = PlayerNameUtil.getPlayerName(player);
        if (name != null && !name.isBlank()) {
            return name;
        }
        //? if >= 1.21.9
        return player.getGameProfile().name();
        //? if < 1.21.9
        //return player.getGameProfile().getName();
    }

    private static void broadcastSharedRules(ServerRuntime runtime, UUID senderId, SharedRuleNotificationPacket notification) {
        for (var player : runtime.getServer().getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(senderId)) {
                PacketSender.sendToPlayer(player, notification);
            }
        }
    }

    private static void handleCombatLogEventReceived(CombatLogEventPacket eventPacket, ServerPayloadContext ctx) {
        if (eventPacket == null || ctx == null) {
            return;
        }
        try {
            // Use the authenticated sender UUID from the server context, not the client-provided one
            sendPackageToAllClientsButSender(ctx.player().getUUID(), eventPacket);
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to broadcast combat log event for player {}!", eventPacket.playerName, e);
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
            var permissionLevel = ServerUtil.getPermissionLevelForPlayer(serverCtx.player(), serverCtx.server());
            sendToClient(serverCtx.player(), new PermissionPacket(permissionLevel));
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
        sendToClient(player, new PermissionPacket(currentPlayerPermissionLevel));

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
        sendToAllClientsButSender(player.getUUID(), runtime.getStore().getConfig());
    }

    private static void sendToClient(ServerPlayer player, PermissionPacket permissions) {
        PacketSender.sendToPlayer(player, permissions);
    }

    private static void sendToClient(ServerPlayer player, ServerConfiguration config) {
        PacketSender.sendToPlayer(player, config);
    }

    private static void sendToClient(ServerPlayer player,  HandshakePacket handshakePacket) {
        PacketSender.sendToPlayer(player, handshakePacket);
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
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot broadcast combat log event");
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
