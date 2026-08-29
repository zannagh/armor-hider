package de.zannagh.armorhider.paper.net;

import com.google.gson.JsonObject;
import de.zannagh.eunomia.networking.packets.PacketType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Paper plugin's view of armor-hider's packet channels, expressed as eunomia {@link PacketType}s
 * over a schema-agnostic {@link JsonObject} payload.
 *
 * <p>These are the exact channel keys the mod declares in {@code AhPackets}, only with the payload
 * type erased to a raw {@link JsonObject}: the plugin never inspects a {@code PlayerConfig}, it stores
 * and relays it opaquely, so it does not need (and could not compile against) the mod's Minecraft-bound
 * payload classes. {@code PaperSchemaContractTest} pins {@link #channelKeys()} to
 * {@code AhPackets.*.channelKey()} so the two sides cannot drift.</p>
 *
 * <p>Every channel lives under the single namespace {@code de.zannagh.armorhider}. The historical
 * {@code armorhider}/{@code de.zannagh.armorhider} dual-namespace split - and the Paper dialect
 * machinery it required - is gone with the eunomia migration. The capability handshake is eunomia's
 * built-in {@code eunomia:hello}/{@code eunomia:hello_ack}, so there is no armor-hider handshake
 * channel here.</p>
 */
public final class ArmorHiderPaperPackets {

    /** Channel namespace for every armor-hider packet. Dots are legal in Bukkit channel names. */
    public static final String NAMESPACE = "de.zannagh.armorhider";

    /** C2S: a single player's {@code PlayerConfig}. */
    public static final PacketType<JsonObject> PLAYER_CONFIG =
            PacketType.serverbound(NAMESPACE, "settings_c2s_packet", JsonObject.class);

    /** S2C: the full {@code ServerConfiguration} snapshot. */
    public static final PacketType<JsonObject> SERVER_CONFIG =
            PacketType.clientbound(NAMESPACE, "settings_s2c_packet", JsonObject.class);

    /** C2S: an admin's {@code ServerWideSettings} update (level &gt;= 3 gated server-side). */
    public static final PacketType<JsonObject> SERVER_WIDE_SETTINGS =
            PacketType.serverbound(NAMESPACE, "server_wide_settings", JsonObject.class);

    /** S2C: the recipient's own permission level. */
    public static final PacketType<JsonObject> PERMISSION =
            PacketType.clientbound(NAMESPACE, "permissions_s2c_packet", JsonObject.class);

    /** C2S: a client-detected combat-log event. */
    public static final PacketType<JsonObject> COMBAT_EVENT =
            PacketType.serverbound(NAMESPACE, "combatlog_c2s_packet", JsonObject.class);

    /** S2C: a relayed combat-log notification (authenticated originator). */
    public static final PacketType<JsonObject> COMBAT_NOTIFICATION =
            PacketType.clientbound(NAMESPACE, "combatlog_s2c_packet", JsonObject.class);

    /** C2S: a client's shared render-rule state announcement. */
    public static final PacketType<JsonObject> SHARED_RULES =
            PacketType.serverbound(NAMESPACE, "shared_rules_c2s_packet", JsonObject.class);

    /** S2C: a relayed shared render-rule outcome (authoritative name + authenticated sender id). */
    public static final PacketType<JsonObject> SHARED_RULES_NOTIFICATION =
            PacketType.clientbound(NAMESPACE, "shared_rules_s2c_packet", JsonObject.class);

    /**
     * Every armor-hider channel, in declaration order. Does <em>not</em> include eunomia's built-in
     * handshake channels - those are registered separately via {@code enableServerHandshake()}.
     */
    public static final List<PacketType<JsonObject>> ALL = List.of(
            PLAYER_CONFIG,
            SERVER_CONFIG,
            SERVER_WIDE_SETTINGS,
            PERMISSION,
            COMBAT_EVENT,
            COMBAT_NOTIFICATION,
            SHARED_RULES,
            SHARED_RULES_NOTIFICATION);

    private ArmorHiderPaperPackets() {
    }

    /**
     * The {@code namespace:path} routing keys of every armor-hider channel (handshake excluded).
     *
     * <p>This is exactly the set {@code PaperSchemaContractTest} asserts equal to the mod's
     * {@code AhPackets.*.channelKey()} set, so the relay and the mod agree on the wire identity of
     * every packet.</p>
     */
    public static Set<String> channelKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (PacketType<JsonObject> type : ALL) {
            keys.add(type.channelKey());
        }
        return keys;
    }
}
