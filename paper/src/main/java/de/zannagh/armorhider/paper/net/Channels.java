package de.zannagh.armorhider.paper.net;

import java.util.List;

/**
 * Every plugin-message channel the mod may use, across every supported game version.
 *
 * <p>The mod switched its namespace from {@code armorhider} to {@code de.zannagh.armorhider} at
 * 1.21.11 for three of the six payloads, while the other three hardcode the long namespace from
 * 1.20.5 onwards and the short one below that. The client announces nothing - it never sends
 * {@code minecraft:register} at all - so there is no advertisement to read the dialect from.
 * The plugin therefore registers the union of both namespaces and {@link ChannelSubscriber}
 * force-subscribes every connection to all of it, which guarantees delivery whatever the client
 * speaks. {@link ClientDialects} then narrows outbound sends back down to a single alias once the
 * client's first inbound message reveals which namespace it uses.</p>
 */
public final class Channels {

    /** Legacy namespace, used by every payload pre-1.20.5 and by some up to 1.21.10. */
    public static final String LEGACY_NAMESPACE = "armorhider";

    /** Current namespace. Dots are legal in Bukkit channel names. */
    public static final String CURRENT_NAMESPACE = "de.zannagh.armorhider";

    /** Serverbound: a single player's {@code PlayerConfig}. */
    public static final List<String> PLAYER_CONFIG_C2S = aliases("settings_c2s_packet");

    /** Serverbound: an admin's {@code ServerWideSettings} update. */
    public static final List<String> SERVER_WIDE_SETTINGS_C2S = aliases("server_wide_settings");

    /** Serverbound: a {@code CombatLogEventPacket}. */
    public static final List<String> COMBAT_LOG_C2S = aliases("combatlog_c2s_packet");

    /** Clientbound: the full {@code ServerConfiguration} snapshot. */
    public static final List<String> SERVER_CONFIGURATION_S2C = aliases("settings_s2c_packet");

    /** Clientbound: the recipient's {@code PermissionPacket}. */
    public static final List<String> PERMISSIONS_S2C = aliases("permissions_s2c_packet");

    /** Clientbound: a relayed {@code CombatLogNotificationPacket}. */
    public static final List<String> COMBAT_LOG_S2C = aliases("combatlog_s2c_packet");

    /**
     * Clientbound: the {@code HandshakePacket} sent on join. The client suppresses all outgoing
     * traffic until it receives this, so the plugin - being a "dumb" relay that would otherwise never
     * announce the mod - must send it, or clients would lock themselves out of a working server.
     */
    public static final List<String> HANDSHAKE_S2C = aliases("handshake_s2c_packet");

    /**
     * The only inbound channels whose namespace reveals the client's era.
     *
     * <p>{@code settings_c2s_packet} and {@code server_wide_settings} switched namespace at 1.21.11,
     * so hearing one tells us which dialect the client speaks. {@code combatlog_c2s_packet} did
     * <em>not</em> - it hardcodes {@link #CURRENT_NAMESPACE} on every version >= 1.20.5 - so it
     * carries no era information at all and must never be used as evidence. Treating it as evidence
     * made a 1.21.4-1.21.10 client look like a 1.21.11+ client the moment it sent a combat event,
     * after which its {@code ServerConfiguration} broadcasts were aimed at a channel it had never
     * registered and were dropped silently. See {@code ClientDialectsTest}.</p>
     */
    public static final List<String> DIALECT_BEARING_C2S = concat(
            PLAYER_CONFIG_C2S,
            SERVER_WIDE_SETTINGS_C2S);

    /** All channels, in both namespaces. Registered as both incoming and outgoing. */
    public static final List<String> ALL = concat(
            PLAYER_CONFIG_C2S,
            SERVER_WIDE_SETTINGS_C2S,
            COMBAT_LOG_C2S,
            SERVER_CONFIGURATION_S2C,
            PERMISSIONS_S2C,
            COMBAT_LOG_S2C,
            HANDSHAKE_S2C);

    private Channels() {
    }

    private static List<String> aliases(String path) {
        return List.of(LEGACY_NAMESPACE + ":" + path, CURRENT_NAMESPACE + ":" + path);
    }

    @SafeVarargs
    private static List<String> concat(List<String>... groups) {
        return java.util.Arrays.stream(groups)
                .flatMap(List::stream)
                .distinct()
                .toList();
    }
}
