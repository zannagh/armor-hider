package de.zannagh.armorhider.net.packets;

import java.util.ArrayList;
import java.util.List;

/**
 * Serverbound: what the sender's <em>shared</em> render rules currently resolve to for their own
 * equipment. Sent only when the outcome changes (and once per join, so a rejoin cannot leave a stale
 * snapshot standing on the other clients), and relayed on as a {@link SharedRuleNotificationPacket}.
 *
 * <p>An empty {@link #overrides} list is meaningful: it says "nothing of mine is rule-hidden any
 * more" and clears the sender's entry everywhere.</p>
 *
 * <p>{@link #playerName} is a diagnostic only. The server discards it and relays its own
 * authoritative name and the authenticated sender UUID, so a client cannot hide somebody else's
 * armor by claiming their name.</p>
 *
 * <p>A plain POJO carried on eunomia's {@code de.zannagh.armorhider:shared_rules_c2s_packet}
 * channel; eunomia serializes from the class.</p>
 *
 * @since 0.13.0
 */
public class SharedRuleStatePacket {

    public String playerName;

    public List<SharedRuleOverride> overrides = new ArrayList<>();

    public long timestamp;

    /** Gson. */
    public SharedRuleStatePacket() {
    }

    public SharedRuleStatePacket(String playerName, List<SharedRuleOverride> overrides) {
        this.playerName = playerName;
        this.overrides = overrides != null ? new ArrayList<>(overrides) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
}
