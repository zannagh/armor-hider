package de.zannagh.armorhider.paper;

import com.google.gson.JsonObject;
import de.zannagh.armorhider.paper.config.ServerConfigStorage;
import de.zannagh.armorhider.paper.config.ServerConfigurationState;
import de.zannagh.armorhider.paper.config.ServerWideSettingsDefaults;
import de.zannagh.armorhider.paper.net.ArmorHiderPaperPackets;
import de.zannagh.armorhider.paper.perm.PermissionResolver;
import de.zannagh.armorhider.paper.util.Schedulers;
import de.zannagh.eunomia.networking.comms.CommunicationManager;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server half of the protocol, mirroring the mod's {@code CommsManager}.
 *
 * <p>The server understands almost nothing of the schema: it stores and relays player configs
 * opaquely as raw {@link JsonObject}s and only ever reads {@code playerId}, {@code playerName} and the
 * four server-wide booleans. Sends go through the eunomia {@link CommunicationManager}, which routes
 * them to the installed {@link de.zannagh.armorhider.paper.net.PaperServerTransport}.</p>
 *
 * <p>There is no {@code sendHandshake} here any more: eunomia's built-in
 * {@code eunomia:hello}/{@code eunomia:hello_ack} capability handshake replaces the armor-hider one,
 * and {@code enableServerHandshake()} answers a client's probe automatically.</p>
 */
public final class ArmorHiderService {

    private final Logger logger;
    private final ServerConfigurationState state;
    private final ServerConfigStorage storage;
    private final PermissionResolver permissions;
    private final Schedulers schedulers;

    public ArmorHiderService(Logger logger,
                             ServerConfigurationState state,
                             ServerConfigStorage storage,
                             PermissionResolver permissions,
                             Schedulers schedulers) {
        this.logger = logger;
        this.state = state;
        this.storage = storage;
        this.permissions = permissions;
        this.schedulers = schedulers;
    }

    /** Sends the full {@code ServerConfiguration} snapshot to a single client. */
    public void sendServerConfiguration(Player player) {
        CommunicationManager.sendToPlayer(player.getUniqueId(),
                ArmorHiderPaperPackets.SERVER_CONFIG, state.toJson());
    }

    /** Sends the recipient's own permission level. */
    public void sendPermissions(Player player) {
        JsonObject packet = new JsonObject();
        packet.addProperty("permissionLevel", permissions.getPermissionLevel(player));
        CommunicationManager.sendToPlayer(player.getUniqueId(),
                ArmorHiderPaperPackets.PERMISSION, packet);
    }

    /**
     * Stores an incoming player config and re-broadcasts the resulting snapshot.
     *
     * <p>The config is keyed by the <em>authenticated</em> sender UUID rather than the
     * client-supplied {@code playerId}, so a client cannot overwrite somebody else's entry.</p>
     */
    public void handlePlayerConfig(Player from, JsonObject config) {
        UUID senderId = from.getUniqueId();
        logger.info("Server received settings packet from " + senderId);
        try {
            state.put(senderId, config);
            saveAsync();
            JsonObject snapshot = state.toJson();
            CommunicationManager.broadcastExcept(senderId,
                    ArmorHiderPaperPackets.SERVER_CONFIG, snapshot);
            sendPermissions(from);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Failed to store player data!", e);
        }
    }

    /**
     * Applies an admin's server-wide settings update.
     *
     * <p>Requires permission level &gt;= 3. Mirrors the mod exactly, including the fact that only
     * {@code enableCombatDetection} and {@code forceArmorHiderOff} take part in change detection
     * and mutation.</p>
     */
    public void handleServerWideSettings(Player from, JsonObject payload) {
        logger.info("Server received admin settings packet.");
        int level = permissions.getPermissionLevel(from);
        if (level < 3) {
            logger.info("Non-admin player " + from.getUniqueId()
                    + " attempted to change server settings. Ignoring.");
            return;
        }
        sendPermissions(from);

        JsonObject current = state.getServerWideSettings();
        boolean combatDetection = ServerWideSettingsDefaults.readBoolean(payload,
                ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION);
        boolean forceOff = ServerWideSettingsDefaults.readBoolean(payload,
                ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF);

        boolean unchanged = ServerWideSettingsDefaults.readBoolean(current,
                ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION) == combatDetection
                && ServerWideSettingsDefaults.readBoolean(current,
                ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF) == forceOff;
        if (unchanged) {
            return;
        }

        JsonObject updated = current.deepCopy();
        updated.addProperty(ServerWideSettingsDefaults.ENABLE_COMBAT_DETECTION, combatDetection);
        updated.addProperty(ServerWideSettingsDefaults.FORCE_ARMOR_HIDER_OFF, forceOff);
        state.setServerWideSettings(updated);
        saveAsync();
        CommunicationManager.broadcastExcept(from.getUniqueId(),
                ArmorHiderPaperPackets.SERVER_CONFIG, state.toJson());
    }

    /**
     * Relays a combat-log event to everyone but the sender.
     *
     * <p>The client-supplied {@code originator} is discarded and replaced with the authenticated
     * sender UUID - otherwise any client could forge a combat event attributed to anyone.</p>
     */
    public void handleCombatLogEvent(Player from, JsonObject payload) {
        // Logged like the other two inbound handlers, and relied on by PaperE2ESmokeTest: with a
        // single connected player the relay below reaches nobody, so this line is the only
        // observable evidence that the combat-log C2S channel works against a real Paper server.
        logger.info("Server received combat log packet from " + from.getUniqueId());
        JsonObject notification = new JsonObject();
        if (payload.has("playerName") && payload.get("playerName").isJsonPrimitive()) {
            notification.addProperty("playerName", payload.get("playerName").getAsString());
        }
        notification.addProperty("originator", from.getUniqueId().toString());
        long timestamp = payload.has("timestamp") && payload.get("timestamp").isJsonPrimitive()
                ? payload.get("timestamp").getAsLong()
                : System.currentTimeMillis();
        notification.addProperty("timestamp", timestamp);

        CommunicationManager.broadcastExcept(from.getUniqueId(),
                ArmorHiderPaperPackets.COMBAT_NOTIFICATION, notification);
    }

    /** Persists the current state. Called on shutdown, on the calling thread. */
    public void saveNow() {
        storage.save(state);
    }

    private void saveAsync() {
        schedulers.runAsync(() -> storage.save(state));
    }
}
