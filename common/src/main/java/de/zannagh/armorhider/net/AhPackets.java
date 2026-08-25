package de.zannagh.armorhider.net;

import de.zannagh.armorhider.net.packets.AhReplicatedPlayerConfig;
import de.zannagh.armorhider.net.packets.CombatLogEventPacket;
import de.zannagh.armorhider.net.packets.CombatLogNotificationPacket;
import de.zannagh.armorhider.net.packets.PermissionPacket;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.net.packets.ServerWideSettings;
import de.zannagh.armorhider.server.ServerConfiguration;
import de.zannagh.eunomia.networking.packets.KeyedPacket;
import de.zannagh.eunomia.networking.packets.PacketType;

/**
 * The single, Minecraft-free source of truth for armor-hider's packet channels, expressed as eunomia
 * {@link PacketType}s. The mod (loader), the Paper plugin and the smoke tests all route on these exact
 * {@link PacketType#channelKey() channel keys}, so there is no second place a channel id or direction
 * can drift.
 *
 * <p>All channels use the single namespace {@code de.zannagh.armorhider}: the eunomia migration is a
 * clean protocol bump, so the historical {@code armorhider}/{@code de.zannagh.armorhider} dual-namespace
 * split (and the Paper dialect machinery it required) is gone. The capability handshake is eunomia's
 * built-in {@code eunomia:hello}/{@code eunomia:hello_ack}, so there is no armor-hider handshake channel.
 */
public final class AhPackets {

    /** Channel namespace for every armor-hider packet. */
    public static final String NAMESPACE = "de.zannagh.armorhider";

    private AhPackets() {
    }

    /** C2S: a client pushes its own {@link PlayerConfig} (stripped via {@code forNetwork()}). */
    public static final PacketType<PlayerConfig> PLAYER_CONFIG =
            PacketType.serverbound(NAMESPACE, "settings_c2s_packet", PlayerConfig.class);

    /**
     * Bidirectional, keyed+replicated per-player config for the HTTP/WebSocket relay fallback. When the joined
     * MC server does not run the mod, eunomia routes this to the external relay, which stores it by the sender's
     * UUID, relays each update to the other clients and dumps the whole set to newcomers on join - so per-player
     * config still propagates with no armor-hider server present. Carries {@link AhReplicatedPlayerConfig}, a thin
     * wrapper over {@link PlayerConfig} (see it for why it is not {@code PlayerConfig} directly). On a normal
     * armor-hider MC server this channel is simply unhandled and ignored; that path uses {@link #PLAYER_CONFIG} +
     * {@link #SERVER_CONFIG} as before.
     */
    public static final KeyedPacket<AhReplicatedPlayerConfig> PLAYER_CONFIG_REPLICATED =
            KeyedPacket.keyedBidirectional(NAMESPACE, "player_config_replicated", AhReplicatedPlayerConfig.class);

    /** S2C: the server's full config snapshot (server-wide settings + every stored player config). */
    public static final PacketType<ServerConfiguration> SERVER_CONFIG =
            PacketType.clientbound(NAMESPACE, "settings_s2c_packet", ServerConfiguration.class);

    /** C2S: an admin pushes updated server-wide settings (level >= 3 gated server-side). */
    public static final PacketType<ServerWideSettings> SERVER_WIDE_SETTINGS =
            PacketType.serverbound(NAMESPACE, "server_wide_settings", ServerWideSettings.class);

    /** S2C: the receiving player's op/permission level (0-4). */
    public static final PacketType<PermissionPacket> PERMISSION =
            PacketType.clientbound(NAMESPACE, "permissions_s2c_packet", PermissionPacket.class);

    /** C2S: a client reports a combat-log event it detected locally. */
    public static final PacketType<CombatLogEventPacket> COMBAT_EVENT =
            PacketType.serverbound(NAMESPACE, "combatlog_c2s_packet", CombatLogEventPacket.class);

    /** S2C: the server re-broadcasts a combat-log event as a notification (authenticated originator). */
    public static final PacketType<CombatLogNotificationPacket> COMBAT_NOTIFICATION =
            PacketType.clientbound(NAMESPACE, "combatlog_s2c_packet", CombatLogNotificationPacket.class);
}
