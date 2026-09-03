package de.zannagh.armorhider.net;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.net.packets.CombatLogNotificationPacket;
import de.zannagh.armorhider.net.packets.PermissionPacket;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.net.packets.SharedRuleNotificationPacket;
import de.zannagh.armorhider.net.packets.SharedRuleOverride;
import de.zannagh.armorhider.net.packets.SharedRuleStatePacket;
import de.zannagh.armorhider.server.ServerConnectionEvents;
import de.zannagh.armorhider.server.ServerRuntime;
import de.zannagh.armorhider.util.PlayerNameUtil;
import de.zannagh.armorhider.util.ServerUtil;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import de.zannagh.eunomia.networking.packets.ServerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
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
        CommunicationManager.onServerReceive(AhPackets.SHARED_RULES,
                (payload, ctx) -> handleSharedRuleStateReceived(payload, ctx));
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
        syncSharedRulesOnJoin(runtime, player, server);
    }

    /**
     * Join-time shared-rule bookkeeping, in both directions.
     *
     * <p>The joiner's own entry is dropped and the drop announced: it belongs to their previous
     * session, possibly to a game they have since restarted without the mod that created it. Then the
     * joiner is told about everyone else, since those announcements happened before they were
     * connected. eunomia handles the capability handshake, so the clear of everyone else's view of the
     * joiner and the joiner's own eventual re-announcement (held client-side until the handshake
     * resolves) are independent - the stale entry is dropped on the other clients here regardless.</p>
     */
    private static void syncSharedRulesOnJoin(ServerRuntime runtime, ServerPlayer player, MinecraftServer server) {
        try {
            var sharedRules = runtime.getSharedRules();
            sharedRules.retainOnline(
                    server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList());

            if (sharedRules.remove(player.getUUID())) {
                broadcastSharedRules(runtime, player.getUUID(), new SharedRuleNotificationPacket(
                        authoritativeNameOf(player), player.getUUID(), List.of(), System.currentTimeMillis()));
            }

            for (var notification : sharedRules.snapshotExcept(player.getUUID())) {
                CommunicationManager.sendToPlayer(player.getUUID(), AhPackets.SHARED_RULES_NOTIFICATION, notification);
            }
        } catch (Exception e) {
            ArmorHider.LOGGER.error("Failed to synchronise shared render rules for joining player {}!",
                    player.getStringUUID(), e);
        }
    }

    /**
     * Relays one player's shared render-rule outcome to everyone else.
     *
     * <p>The sender's own {@code playerName} is discarded and replaced with the server's authoritative
     * display name, and the id with the authenticated sender UUID. Without that, any client could hide
     * another player's armor for the whole server by announcing state under their name.</p>
     */
    private static void handleSharedRuleStateReceived(SharedRuleStatePacket packet, ServerContext ctx) {
        if (packet == null || ctx == null) {
            return;
        }
        ServerRuntime runtime = ArmorHider.getRuntime();
        if (runtime == null) {
            ArmorHider.LOGGER.warn("Runtime not initialized, cannot handle shared render rules");
            return;
        }
        ServerPlayer sender = resolvePlayer(ctx.senderId());
        if (sender == null) {
            ArmorHider.LOGGER.warn("Could not resolve player {} for shared render rules.", ctx.senderId());
            return;
        }

        String authoritativeName = authoritativeNameOf(sender);
        long timestamp = packet.timestamp > 0 ? packet.timestamp : System.currentTimeMillis();

        try {
            List<SharedRuleOverride> stored =
                    runtime.getSharedRules().put(sender.getUUID(), authoritativeName, packet.overrides, timestamp);
            if (stored == null) {
                // Identical to what this player already announced. Dropping it here keeps a
                // per-tick-true predicate from turning into a broadcast per tick if a client ever
                // stops diffing on its side.
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

    private static void broadcastSharedRules(ServerRuntime runtime, UUID senderId,
                                             SharedRuleNotificationPacket notification) {
        CommunicationManager.broadcastExcept(senderId, AhPackets.SHARED_RULES_NOTIFICATION, notification);
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
