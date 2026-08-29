package de.zannagh.armorhider.net.packets;

import org.jspecify.annotations.Nullable;

/**
 * The equipment a shared render rule can address, as it travels over the wire.
 *
 * <p>Deliberately separate from the client-side {@code AhRuleTarget}, which lives in the client
 * source set and is therefore invisible to the networking and server code that has to move these
 * values around. The two enums carry the same constants and are mapped <b>by name</b>, so neither
 * side can be broken by a reordering, and an unknown name decodes to {@code null} rather than
 * throwing - a newer client may well know a target this side does not.</p>
 *
 * @since 0.13.0
 */
public enum SharedRuleTarget {
    HEAD,
    CHEST,
    LEGS,
    FEET,
    OFFHAND,
    ELYTRA;

    /**
     * @return the constant with this name, or {@code null} if there is none. Used on the receiving
     *         end, where the value is attacker-controlled and a future version may add targets.
     */
    public static @Nullable SharedRuleTarget byName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (SharedRuleTarget target : values()) {
            if (target.name().equals(name)) {
                return target;
            }
        }
        return null;
    }
}
