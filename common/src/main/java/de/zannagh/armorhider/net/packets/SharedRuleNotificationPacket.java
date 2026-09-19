package de.zannagh.armorhider.net.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clientbound relay of one player's {@link SharedRuleStatePacket}.
 *
 * <p>{@link #playerName} and {@link #playerId} are filled in by the server from the authenticated
 * connection, never from the sender's payload. Receivers key the state by name, because that is how
 * every other render decision identifies a player; the id travels along so the entry can be replaced
 * and pruned reliably when a display name changes or is shared.</p>
 *
 * <p>An empty {@link #overrides} list clears the named player's entry.</p>
 *
 * <p>A plain POJO carried on eunomia's {@code de.zannagh.armorhider:shared_rules_s2c_packet}
 * channel; eunomia serializes from the class.</p>
 *
 * @since 0.13.0
 */
public class SharedRuleNotificationPacket {

    public String playerName;

    public UUID playerId;

    public List<SharedRuleOverride> overrides = new ArrayList<>();

    public long timestamp;

    /** Gson. */
    public SharedRuleNotificationPacket() {
    }

    public SharedRuleNotificationPacket(String playerName, UUID playerId, List<SharedRuleOverride> overrides, long timestamp) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.overrides = overrides != null ? new ArrayList<>(overrides) : new ArrayList<>();
        this.timestamp = timestamp;
    }
}
