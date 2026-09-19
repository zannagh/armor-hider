package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.net.packets.PlayerConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Memoises the two config resolutions that ALLOCATE.
 * <p>
 * {@code AhPlayerConfigApiImpl.resolveConfig(name)} returns the shared {@code CURRENT} instance for the local
 * player and existing instances for the server-transmitted / individual-override branches - those are already
 * free. The remaining two branches built a whole new {@code PlayerConfig} graph on <b>every call</b>:
 * {@code CURRENT.deepCopy(name, id)} for an unknown player, and the {@code freshGlobalOverride()} fallback of
 * {@code getGlobalConfigOverride()}. Measured on fabric-1.21.4, the render path asked for a remote player's
 * config ~5000 times per 60-tick window and allocated one config per ask, 1:1; with four remote players the
 * client rendered ~41% fewer frames. This class makes that once-per-player instead of once-per-ask.
 * <p>
 * <b>Instance identity is preserved.</b> {@code SlotModification.shouldUseVanilla} identifies the local player
 * by comparing the resolved config against {@code getLocalPlayerConfig()} by reference
 * ({@code SlotModification.java:76}). Everything stored here is a deep copy or a fresh default, never
 * {@code CURRENT} itself, so a cached remote config can never be mistaken for the viewer's own.
 * <p>
 * <b>Thread safety.</b> Entries live in a {@link ConcurrentHashMap} and the stamp in an
 * {@link AtomicReference}; the read path takes no lock and no {@code synchronized} block. This is NOT merely
 * a hot-path nicety: mod packet handlers run on the netty I/O thread (see the
 * {@code ClientPacketListenerMixin} {@code @At("HEAD")} injection into {@code handleCustomPayload}, which is
 * before vanilla's {@code ensureRunningOnSameThread}), so a config change and a render-thread resolution
 * genuinely run concurrently.
 * <p>
 * Because building a value is not atomic with storing it, a writer captures the stamp it built under
 * ({@link #invalidateIfStale} returns it) and {@link #put} both pre- and post-checks that the stamp is still
 * the very same instance, dropping the entry when it moved. {@link #dropEntries} publishes {@code null} into
 * the stamp BEFORE clearing the map, which is what makes the post-check sufficient: if a writer's post-check
 * still reads its own stamp, the invalidator had not yet nulled it and therefore had not yet cleared the map,
 * so the clear is still to come and will remove the entry. The guarantee is therefore: <b>a stale entry never
 * survives the invalidation that obsoleted it</b>; a racing invalidation may cost one wasted rebuild, and a
 * value built under a stamp that has since moved is returned to its one caller but not cached.
 * <p>
 * <b>No behavioural delta - including item discovery.</b> Memoisation would otherwise have had one visible
 * side effect: the values stored here used to be per-call throwaways, so the
 * {@code SlotModification.addItemInformation} -> {@code ExclusionItemConfiguration.discoverItem} write on a
 * remote player's config was discarded with the copy on the next line. Retained instead, it would accumulate
 * for the session, unread and unbounded - nothing reads it ({@code ItemExclusionScreen.java:42}, the only UI
 * over the exclusion list, reads {@code getLocalPlayerConfig().getExclusionItems()} unconditionally, and the
 * only other client reader, {@code GenderPhysicsRelaxation.java:66}, queries user-set ignore flags rather
 * than discovered entries, which are written with {@code shouldIgnore == false}) and nothing prunes it
 * ({@code discoverItem} at {@code ExclusionItemConfiguration.java:217}/{@code :231} is a
 * containsKey-then-setItem with no cap; the {@code MAX_DISCOVERED_ITEMS_PER_SLOT} cap lives only in
 * {@code prune()} ({@code :157}), whose sole production caller is {@code PlayerConfig.heal()}
 * ({@code PlayerConfig.java:502}) on deserialization, which these instances never see).
 * <p>
 * That write is therefore <b>suppressed</b>, restoring the pre-memoisation discard exactly: both values
 * stored here are tagged {@code PlayerConfig#markAsDerivedResolution()} at construction, and
 * {@code addItemInformation} skips only the {@code discoverItem} call (not the {@code shouldArmorHiderIgnore}
 * lookup, which reads the user's own flags carried over by the deep copy) for a tagged config. The
 * discriminator is "was this instance synthesized per call", NOT "is this the local config": a configured
 * {@code CURRENT.globalPlayerOverride}, a server-transmitted config and an individual override were all
 * shared, persisted instances before this cache existed, are returned uncached, are untagged, and still
 * discover. This class is consequently a pure performance change.
 */
final class ResolvedConfigCache {

    /**
     * Hard cap on cached remote players. A render frame can only resolve players that are actually being
     * drawn, so this is far above any realistic simultaneous-entity count; it exists purely so a session that
     * churns through thousands of distinct names (a busy hub server over hours) cannot grow the map forever.
     * On overflow the whole map is dropped rather than evicting one entry: resolutions are cheap to rebuild
     * and a full drop needs no access ordering, hence no lock on the render path.
     */
    private static final int MAX_ENTRIES = 256;

    private final Map<String, PlayerConfig> entries = new ConcurrentHashMap<>();

    /** The stamp every cached entry was built under; {@code null} means "nothing cached yet". */
    private final AtomicReference<@Nullable ConfigResolutionStamp> stamp = new AtomicReference<>();

    /**
     * The memoised {@code freshGlobalOverride()} fallback:
     * {@code PlayerConfig.defaults(DEFAULT_PLAYER_ID, DEFAULT_PLAYER_NAME)}.
     * <p>
     * Deliberately NOT covered by the stamp: its value depends on nothing the stamp tracks, so a generation
     * bump cannot change it and clearing it on one would only re-allocate an identical graph. It is dropped
     * only by {@link #clear} (disconnect/join).
     * <p>
     * It is shared more widely than "Row C": {@code getGlobalConfigOverride()} serves the Row C branch, the
     * Row B fallback of {@code resolveUnknownPlayerConfig}, and {@code resolveConfig(null)} for players the
     * render path could not identify at all ({@code AhPlayerConfigApiImpl.java:315-319}). All of those share
     * one instance on purpose - see {@link #putGlobalOverrideFallback}.
     */
    private final AtomicReference<@Nullable PlayerConfig> globalOverrideFallback = new AtomicReference<>();

    /**
     * Drops every entry when any component of the resolution stamp has changed since the cached entries were
     * built. Must be called before {@link #get}, and the stamp it returns must be handed back to {@link #put}
     * so a value built under an already-obsolete stamp is not cached.
     *
     * @return the stamp the cache is now on; pass it to {@link #put}.
     */
    ConfigResolutionStamp invalidateIfStale(
            long generation,
            int localConfigIdentity,
            @Nullable String serverKey,
            int flags,
            int serverConfigIdentity) {
        ConfigResolutionStamp current = stamp.get();
        if (current != null
                && current.matches(generation, localConfigIdentity, serverKey, flags, serverConfigIdentity)) {
            return current;
        }
        dropEntries();
        ConfigResolutionStamp fresh = new ConfigResolutionStamp(
                generation, localConfigIdentity, serverKey, flags, serverConfigIdentity);
        stamp.set(fresh);
        return fresh;
    }

    /**
     * @param playerName the resolved player's name.
     * @param playerId   the id the caller would build the copy with; a cached entry built for a different id
     *                   (e.g. the {@code DEFAULT_PLAYER_ID} placeholder before the player list knew the uuid)
     *                   is treated as a miss so the better-identified copy replaces it.
     * @return the memoised config, or {@code null} on a miss.
     */
    @Nullable PlayerConfig get(String playerName, UUID playerId) {
        PlayerConfig cached = entries.get(playerName);
        if (cached == null) {
            return null;
        }
        return playerId.equals(cached.playerId.getValue()) ? cached : null;
    }

    /**
     * Stores a freshly built resolution and returns it, so call sites read
     * {@code return cache.put(name, id, CURRENT.deepCopy(name, id), stamp);}.
     * <p>
     * The value is always returned, cached or not: the caller asked for a resolution and gets a correct one
     * either way. Caching is conditional on {@code builtUnder} still being the live stamp INSTANCE, checked
     * both before and after the map write - see the thread-safety note on the class.
     *
     * @param builtUnder the stamp returned by {@link #invalidateIfStale} BEFORE the value was built.
     */
    PlayerConfig put(String playerName, UUID playerId, PlayerConfig config, ConfigResolutionStamp builtUnder) {
        if (stamp.get() != builtUnder) {
            // The world moved while the deep copy was being built; the copy is of a config that no longer
            // resolves this way. Hand it back for this one call, but do not let it become the cached answer.
            return config;
        }
        if (entries.size() >= MAX_ENTRIES) {
            entries.clear();
        }
        entries.put(playerName, config);
        if (stamp.get() != builtUnder) {
            // Re-check AFTER the write. dropEntries() nulls the stamp before clearing the map, so reading a
            // moved stamp here means the clear either already happened (harmless, we just re-added and now
            // remove) or is still to come (it will remove it). Value-conditional remove so a newer entry
            // written by another thread for the same name is left alone.
            entries.remove(playerName, config);
        }
        return config;
    }

    /**
     * Evicts one player's memoised resolution, leaving every other entry and the stamp intact.
     * <p>
     * Used for configuration changes that provably affect exactly one remote player, so ordinary recurring
     * network traffic does not return every rendered player to the pre-memoisation allocation rate on the
     * next frame. Anything that could change how OTHER players resolve - the local config in particular,
     * since every entry here is a deep copy of it - must go through the stamp instead.
     */
    void evict(String playerName) {
        entries.remove(playerName);
    }

    /** @return the memoised fresh global-override fallback, or {@code null} if it has not been built yet. */
    @Nullable PlayerConfig getGlobalOverrideFallback() {
        return globalOverrideFallback.get();
    }

    /**
     * Stores the fresh global-override fallback. Uses compare-and-set so two threads racing the first build
     * converge on ONE shared instance: the fallback stands in for a single "how I see strangers" config, so
     * every caller should see the same object rather than a per-thread duplicate.
     * <p>
     * No stamp check is needed here (unlike {@link #put}): the memoised value depends on nothing the stamp
     * tracks, so there is no interleaving in which it becomes stale.
     */
    PlayerConfig putGlobalOverrideFallback(PlayerConfig config) {
        PlayerConfig existing = globalOverrideFallback.compareAndExchange(null, config);
        return existing != null ? existing : config;
    }

    /**
     * Drops the per-player entries and the stamp, keeping the stamp-independent global-override fallback.
     * <p>
     * Order is load-bearing: the stamp is nulled FIRST so a writer that re-reads it after its own map write
     * can never conclude "my stamp is still live" while the map clear that would have removed its entry is
     * already behind it. See the thread-safety note on the class.
     */
    private void dropEntries() {
        stamp.set(null);
        entries.clear();
    }

    /** Drops everything, fallback included - called on disconnect and world change. */
    void clear() {
        dropEntries();
        globalOverrideFallback.set(null);
    }
}
