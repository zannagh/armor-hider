package de.zannagh.armorhider.paper.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What each connected player's shared render rules currently resolve to, mirroring the mod's
 * {@code SharedRuleStore}.
 *
 * <p>Transient by design: it is never persisted. A shared rule's outcome is a live statement about a
 * player's current state, produced by a client-side mod that may not even be installed the next time
 * they connect - a persisted copy would resurrect a stale claim.</p>
 *
 * <p>Schema-agnostic like the rest of the plugin: the {@code overrides} array is stored and relayed
 * as raw JSON, so a client shipping a newer shape round-trips unchanged. Only the notification
 * envelope ({@code playerName}, {@code playerId}, {@code timestamp}) is ever written here, and always
 * from the authenticated connection rather than from the sender's payload.</p>
 */
public final class SharedRuleRelayState {

    /** Key of the array of per-target outcomes inside both the C2S and the S2C payload. */
    public static final String OVERRIDES = "overrides";

    public static final String PLAYER_ID = "playerId";

    public static final String PLAYER_NAME = "playerName";

    public static final String TIMESTAMP = "timestamp";

    private record Entry(String playerName, JsonArray overrides, long timestamp) {
    }

    private final Map<UUID, Entry> byPlayer = new ConcurrentHashMap<>();

    /**
     * Records a player's state and produces the notification to relay.
     *
     * @param overrides the raw array from the sender's payload; {@code null} or empty means "nothing of
     *                  mine is rule-hidden any more" and drops the entry.
     * @return the notification to broadcast, or {@code null} when nothing changed - an unchanged state
     *         is not relayed, so a client whose predicate is true every tick cannot become a broadcast
     *         source.
     */
    public JsonObject put(UUID playerId, String playerName, JsonArray overrides, long timestamp) {
        JsonArray sanitized = overrides == null ? new JsonArray() : overrides;

        if (sanitized.isEmpty()) {
            if (byPlayer.remove(playerId) == null) {
                return null;
            }
            return notification(playerId, playerName, new JsonArray(), timestamp);
        }

        Entry previous = byPlayer.put(playerId, new Entry(playerName, sanitized, timestamp));
        boolean changed = previous == null
                || !previous.overrides().equals(sanitized)
                || !Objects.equals(previous.playerName(), playerName);
        return changed ? notification(playerId, playerName, sanitized, timestamp) : null;
    }

    /**
     * Forgets a player.
     *
     * @return the clearing notification to broadcast, or {@code null} if they had nothing stored.
     */
    public JsonObject remove(UUID playerId, String playerName, long timestamp) {
        Entry previous = byPlayer.remove(playerId);
        return previous == null ? null : notification(playerId, playerName, new JsonArray(), timestamp);
    }

    /** Drops everyone no longer connected, so a long-running server cannot accumulate entries. */
    public void retainOnline(Collection<UUID> onlineIds) {
        Set<UUID> keep = Set.copyOf(onlineIds);
        byPlayer.keySet().removeIf(id -> !keep.contains(id));
    }

    /**
     * @return one notification per player currently sharing something, excluding {@code exclude} - a
     *         client never needs its own state back.
     */
    public List<JsonObject> snapshotExcept(UUID exclude) {
        List<JsonObject> snapshot = new ArrayList<>();
        byPlayer.forEach((id, entry) -> {
            if (id.equals(exclude)) {
                return;
            }
            snapshot.add(notification(id, entry.playerName(), entry.overrides(), entry.timestamp()));
        });
        return snapshot;
    }

    private static JsonObject notification(UUID playerId, String playerName, JsonArray overrides, long timestamp) {
        JsonObject packet = new JsonObject();
        packet.addProperty(PLAYER_NAME, playerName);
        packet.addProperty(PLAYER_ID, playerId.toString());
        packet.add(OVERRIDES, overrides);
        packet.addProperty(TIMESTAMP, timestamp);
        return packet;
    }
}
