package de.zannagh.armorhider.server;

import de.zannagh.armorhider.net.packets.SharedRuleNotificationPacket;
import de.zannagh.armorhider.net.packets.SharedRuleOverride;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What each connected player's shared render rules currently resolve to, so a client joining later
 * can be told about the players who announced something before it arrived.
 *
 * <p>Deliberately <b>transient</b>: it is never written to disk and does not survive a restart. A
 * shared rule's outcome is a live statement about a player's current state ("my helmet is off while I
 * am sleeping"), produced by a mod that may not even be installed the next time they connect.
 * Persisting it would resurrect a stale claim.</p>
 *
 * <p>Keyed by the authenticated sender UUID. The display name is stored alongside so the relay can
 * carry the server's own name rather than the sender's claim.</p>
 */
public final class SharedRuleStore {

    private record Entry(String playerName, List<SharedRuleOverride> overrides, long timestamp) {
    }

    private final Map<UUID, Entry> byPlayer = new ConcurrentHashMap<>();

    /**
     * Records a player's current state, dropping malformed and no-op entries and clamping opacities.
     *
     * @return {@code null} when this changed nothing - an unchanged state is not relayed, so a client
     *         whose predicate is true every tick cannot turn into a broadcast source. Otherwise the
     *         sanitized state to relay, which is empty when the player just stopped sharing. Callers
     *         must relay <em>this</em> list rather than what arrived, so what the other clients apply
     *         is exactly what the server stored.
     */
    public @Nullable List<SharedRuleOverride> put(UUID playerId, String playerName,
                                                  @Nullable List<SharedRuleOverride> overrides, long timestamp) {
        List<SharedRuleOverride> sanitized = new ArrayList<>();
        if (overrides != null) {
            for (SharedRuleOverride override : overrides) {
                if (override != null && override.isMeaningful()) {
                    sanitized.add(override.sanitized());
                }
            }
        }

        if (sanitized.isEmpty()) {
            return byPlayer.remove(playerId) != null ? List.of() : null;
        }

        List<SharedRuleOverride> stored = List.copyOf(sanitized);
        Entry previous = byPlayer.put(playerId, new Entry(playerName, stored, timestamp));
        boolean changed = previous == null
                || !previous.overrides().equals(stored)
                || !previous.playerName().equals(playerName);
        return changed ? stored : null;
    }

    /** Forgets a player entirely. @return whether there was anything to forget. */
    public boolean remove(UUID playerId) {
        return byPlayer.remove(playerId) != null;
    }

    /** Drops everyone who is no longer connected, so a long-running server cannot accumulate entries. */
    public void retainOnline(Collection<UUID> onlineIds) {
        Set<UUID> keep = Set.copyOf(onlineIds);
        byPlayer.keySet().removeIf(id -> !keep.contains(id));
    }

    /**
     * @return one notification per player currently sharing something, excluding {@code exclude}
     *         (the recipient - a client never needs its own state back, and the receiving store treats
     *         "not present" as "evaluate my own rules locally").
     */
    public List<SharedRuleNotificationPacket> snapshotExcept(@Nullable UUID exclude) {
        List<SharedRuleNotificationPacket> snapshot = new ArrayList<>();
        byPlayer.forEach((id, entry) -> {
            if (id.equals(exclude)) {
                return;
            }
            snapshot.add(new SharedRuleNotificationPacket(entry.playerName(), id, entry.overrides(), entry.timestamp()));
        });
        return snapshot;
    }
}
