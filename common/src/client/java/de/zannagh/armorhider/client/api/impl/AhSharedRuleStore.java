package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.net.packets.SharedRuleOverride;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the <em>other</em> players' shared rules currently resolve to, as relayed by the server.
 * <p>
 * Written from the network thread when a {@code SharedRuleNotificationPacket} arrives, read from the
 * render thread by {@link AhRenderRuleRegistryImpl#evaluate}. Entries are immutable
 * {@link EnumMap}s swapped in wholesale, so a reader can never observe a half-applied update, and the
 * hot path is one volatile read ({@link #isEmpty()}) followed - only when something is actually
 * shared anywhere - by a single map lookup.
 * <p>
 * Keyed by display name, because that is how every other render decision identifies a player. The
 * name is the server's own, never the sender's claim. The owner's UUID is kept alongside so entries
 * can be pruned when a player leaves and replaced when their display name changes.
 * <p>
 * The local player never appears here: the server relays to everyone but the sender, so the viewer's
 * own shared rules only ever evaluate locally.
 */
@ApiStatus.Internal
public final class AhSharedRuleStore {

    private record Entry(UUID playerId, Map<AhRuleTarget, SharedRuleOverride> overrides) {
    }

    private static final Map<String, Entry> BY_NAME = new ConcurrentHashMap<>();

    /**
     * Mirrors {@code BY_NAME.isEmpty()} for the render path. A {@code ConcurrentHashMap.isEmpty()} is
     * cheap but not free, and this is read once per player, per slot, per frame; a plain volatile
     * boolean keeps the zero-consumer case down to a single read.
     */
    private static volatile boolean empty = true;

    private AhSharedRuleStore() {
    }

    /** @return whether nothing at all is currently shared by anyone. */
    public static boolean isEmpty() {
        return empty;
    }

    /**
     * Replaces everything known about one player. An empty or entirely meaningless list of overrides
     * removes the entry, which is how a player says "nothing of mine is rule-hidden any more".
     *
     * @param playerName the server's authoritative display name for the owner.
     * @param playerId   the owner's authenticated UUID, or {@code null} if the server did not send one.
     * @param overrides  the owner's current per-target outcomes.
     * @return whether anything actually changed - the caller only has to invalidate render caches then.
     */
    public static boolean put(@Nullable String playerName, @Nullable UUID playerId,
                              @Nullable List<SharedRuleOverride> overrides) {
        if (playerName == null || playerName.isBlank()) {
            return false;
        }

        Map<AhRuleTarget, SharedRuleOverride> parsed = new EnumMap<>(AhRuleTarget.class);
        if (overrides != null) {
            for (SharedRuleOverride override : overrides) {
                if (override == null || !override.isMeaningful()) {
                    continue;
                }
                AhRuleTarget target = AhRuleTarget.fromWire(override.target);
                if (target == null) {
                    // A target this version does not know - a newer client on the same server. Skipping
                    // it is the right failure mode: the rest of that player's state still applies.
                    continue;
                }
                parsed.put(target, override.sanitized());
            }
        }

        // Renaming is handled by dropping every other entry that carries the same id. Without it a
        // display-name change (rank prefix, nick plugin) would leave the old name hidden forever, since
        // the owner only ever sends under their current name.
        boolean changed = false;
        if (playerId != null) {
            changed = BY_NAME.entrySet().removeIf(entry ->
                    playerId.equals(entry.getValue().playerId()) && !entry.getKey().equals(playerName));
        }

        if (parsed.isEmpty()) {
            changed |= BY_NAME.remove(playerName) != null;
        } else {
            Entry previous = BY_NAME.put(playerName, new Entry(playerId, Map.copyOf(parsed)));
            changed |= previous == null || !previous.overrides().equals(parsed);
        }
        empty = BY_NAME.isEmpty();
        return changed;
    }

    /** @return what {@code playerName} shares for {@code target}, or {@code null} if nothing. */
    public static @Nullable SharedRuleOverride get(@Nullable String playerName, @Nullable AhRuleTarget target) {
        if (playerName == null || target == null) {
            return null;
        }
        Entry entry = BY_NAME.get(playerName);
        return entry == null ? null : entry.overrides().get(target);
    }

    /** Drops everything. Called on disconnect - shared state belongs to one connection. */
    public static void clear() {
        BY_NAME.clear();
        empty = true;
    }

    /**
     * Drops entries for players who are no longer connected.
     *
     * <p>This is the only cleanup for a player who leaves: the mod has no server-side disconnect hook,
     * and adding one would mean a mixin whose target signature has changed several times across the
     * supported version range. The tab list already tells every client exactly who is online, so the
     * pruning happens where the information is free.</p>
     *
     * <p>Matched on UUID, never on name: the tab list is authoritative about ids, while the display
     * name it shows is not necessarily the one the entry was stored under. Pruning on a name mismatch
     * would silently delete live state. An entry whose owner id is unknown is kept for the same
     * reason - it cannot be proven stale.</p>
     *
     * @param onlineIds the UUIDs currently in the player list.
     * @return whether anything was removed.
     */
    public static boolean retainOnline(Collection<UUID> onlineIds) {
        if (BY_NAME.isEmpty()) {
            return false;
        }
        Set<UUID> keep = Set.copyOf(onlineIds);
        boolean removed = BY_NAME.values().removeIf(entry ->
                entry.playerId() != null && !keep.contains(entry.playerId()));
        if (removed) {
            empty = BY_NAME.isEmpty();
        }
        return removed;
    }

    /** @return the names currently carrying shared state. For the prune pass and for tests. */
    public static Set<String> knownPlayers() {
        return Set.copyOf(BY_NAME.keySet());
    }
}
