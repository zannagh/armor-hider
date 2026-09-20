package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.util.PlayerNameUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Name to {@link Player} resolution for the {@code Predicate<Player>} rule variants.
 * <p>
 * Rules are evaluated once per player, per slot, per frame, so a naive scan over
 * {@code level.players()} would run hundreds of times a frame on a busy server. The whole level
 * roster is therefore snapshotted into a name-keyed map and reused for a short window (~1 tick).
 * The snapshot is held per-thread so the render thread never contends with an off-thread caller -
 * Minecraft's entity lists are not safe to iterate concurrently anyway.
 * <p>
 * Entries are {@link WeakReference}s. The render thread outlives every {@code ClientLevel}, so a
 * strong map here would pin the last snapshot's players - and transitively their level - for the
 * rest of the process after a disconnect. {@link #invalidate()} is additionally wired to the
 * client-disconnect event, which covers the common case eagerly rather than waiting for a GC.
 * <p>
 * <b>Known limitation:</b> the map is keyed by display name, so two players sharing a display name
 * (a nick plugin, or a rank prefix collapsing two names to the same string) resolve to whichever
 * one was iterated last. Armor Hider identifies players by display name throughout - config
 * look-ups and combat keys have the same ambiguity - so this cannot be fixed here without changing
 * that identity model. Rules that must disambiguate should use the {@code *Matching} variants and
 * compare something stronger off {@link de.zannagh.armorhider.client.api.AhHideContext}.
 */
@ApiStatus.Internal
public final class AhPlayerLookupCache {

    /** Roughly one tick. Long enough to cover every frame of a tick, short enough to self-heal. */
    private static final long TTL_NANOS = 50_000_000L;

    private static final ThreadLocal<Map<String, WeakReference<Player>>> SNAPSHOT =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * {@code [0]} is the {@link System#nanoTime()} reading of the last refresh, {@code [1]} is a
     * primed flag: {@code 0} until this thread has refreshed at least once.
     * <p>
     * The flag exists instead of a sentinel timestamp because {@code nanoTime()}'s origin is
     * arbitrary. Seeding {@code [0]} with {@link Long#MIN_VALUE} and testing
     * {@code now - stamp[0] > TTL_NANOS} overflows for any positive {@code now}, wrapping to a
     * large negative value, so the test reads false forever and the snapshot is never built - which
     * silently made every {@code Predicate<Player>} rule non-matching. Only ever subtract two real
     * {@code nanoTime()} readings from each other.
     */
    private static final ThreadLocal<long[]> STAMP =
            ThreadLocal.withInitial(() -> new long[]{0L, 0L});

    private AhPlayerLookupCache() {}

    /**
     * @return the loaded client-side player with this display name, or {@code null} when it is not
     * currently in the level (out of render distance, removed, or no level loaded).
     */
    public static @Nullable Player resolve(@Nullable String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        var snapshot = SNAPSHOT.get();
        long[] stamp = STAMP.get();
        long now = System.nanoTime();
        if (stamp[1] == 0L || now - stamp[0] > TTL_NANOS) {
            refresh(snapshot);
            stamp[0] = now;
            stamp[1] = 1L;
        }
        var reference = snapshot.get(playerName);
        return reference == null ? null : reference.get();
    }

    /** Drops the cached roster; the next {@link #resolve(String)} rebuilds it. */
    public static void invalidate() {
        SNAPSHOT.get().clear();
        // Clear the primed flag rather than re-seeding the timestamp - see the note on STAMP.
        STAMP.get()[1] = 0L;
    }

    private static void refresh(Map<String, WeakReference<Player>> snapshot) {
        snapshot.clear();
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (Player player : level.players()) {
            // Same naming strategy the render path and the config lookups use, so a rule keyed off
            // AhHideContext#playerName() resolves to the entity Armor Hider is actually rendering.
            String name = PlayerNameUtil.getPlayerName(player);
            if (name != null) {
                snapshot.put(name, new WeakReference<>(player));
            }
        }
    }
}
