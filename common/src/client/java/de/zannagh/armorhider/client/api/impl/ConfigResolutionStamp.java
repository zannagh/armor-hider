package de.zannagh.armorhider.client.api.impl;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of everything that can change the outcome of
 * {@code AhPlayerConfigApiImpl.resolveUnknownPlayerConfig(...)} / {@code getGlobalConfigOverride()}.
 * <p>
 * {@link ResolvedConfigCache} holds one of these alongside its entries and drops the whole cache the moment
 * a freshly read stamp stops matching. It is deliberately ONE value rather than a handful of ad-hoc checks,
 * so a new invalidation source is added in exactly one place.
 * <p>
 * The components are:
 * <ul>
 *   <li>{@code generation} - the monotonic config-change counter from
 *       {@code AhPlayerConfigApiImpl}'s GLOBAL config-change counter - a sibling of the public
 *       {@code ArmorHiderPlayerConfigApi#getConfigGeneration()} that advances only on changes which are not
 *       provably scoped to one remote player (local save, server config set/cleared, session toggle, and
 *       the in-place mutators that do not persist). A notification naming a single remote player evicts that
 *       one entry directly instead of moving this counter.</li>
 *   <li>{@code localConfigIdentity} - the identity of the {@code CURRENT} instance, so swapping the whole
 *       local config (preset load, settings screen rebuild) invalidates even if a generation bump were
 *       missed.</li>
 *   <li>{@code serverKey} - the current server identity, so joining a different server (or going to
 *       singleplayer) never reuses the previous session's resolutions.</li>
 *   <li>{@code flags} - the packed branch toggles that decide WHICH source a remote player resolves from
 *       (see {@link #pack}).</li>
 *   <li>{@code serverConfigIdentity} - the identity of the server-transmitted configuration, or {@code 0}
 *       when there is none.</li>
 * </ul>
 *
 * @param generation          the config-change counter at the time of the snapshot.
 * @param localConfigIdentity {@code System.identityHashCode} of the local {@code CURRENT} config.
 * @param serverKey           the server key the snapshot was taken on.
 * @param flags               the packed branch toggles, see {@link #pack}.
 * @param serverConfigIdentity {@code System.identityHashCode} of the server config, or {@code 0} for none.
 */
record ConfigResolutionStamp(
        long generation,
        int localConfigIdentity,
        @Nullable String serverKey,
        int flags,
        int serverConfigIdentity) {

    /**
     * Packs the three resolution-affecting toggles into one int so the hot path can compare them without
     * allocating anything.
     * <p>
     * There is deliberately no separate bit for {@code areOtherPlayerConfigsAllowed()}: it is
     * {@code return areIndividualConfigsAllowedByServer();} verbatim
     * ({@code ArmorHiderPlayerConfigApi.java:175-177}), so a fourth bit would always equal the third and
     * present four independent toggles where there are three. The only builder of this stamp is
     * {@code AhPlayerConfigApiImpl}, which does not override either method. Should the two ever diverge, add
     * the bit back here and at the single {@code pack(...)} call site.
     *
     * @param useGlobalOverrideForAllPlayers   Row C: the global override applies to everybody.
     * @param usePlayerSettingsWhenUndeterminable Row B: unknown players inherit the viewer's own settings.
     * @param individualConfigsAllowedByServer whether the server permits per-player overrides (and, being
     *                                         the same value, whether client-side other-player
     *                                         configuration applies at all).
     * @return the packed flag bits.
     */
    static int pack(
            boolean useGlobalOverrideForAllPlayers,
            boolean usePlayerSettingsWhenUndeterminable,
            boolean individualConfigsAllowedByServer) {
        int packed = 0;
        if (useGlobalOverrideForAllPlayers) {
            packed |= 1;
        }
        if (usePlayerSettingsWhenUndeterminable) {
            packed |= 1 << 1;
        }
        if (individualConfigsAllowedByServer) {
            packed |= 1 << 2;
        }
        return packed;
    }

    /**
     * Component-wise comparison against a freshly read set of values. Deliberately NOT
     * {@code equals(new ConfigResolutionStamp(...))}: the hot path must not allocate a stamp per frame, so it
     * compares the components it just read and only builds a new stamp on the (rare) mismatch.
     *
     * @return {@code true} when nothing that could change a resolution has changed.
     */
    boolean matches(
            long otherGeneration,
            int otherLocalConfigIdentity,
            @Nullable String otherServerKey,
            int otherFlags,
            int otherServerConfigIdentity) {
        if (generation != otherGeneration
                || localConfigIdentity != otherLocalConfigIdentity
                || flags != otherFlags
                || serverConfigIdentity != otherServerConfigIdentity) {
            return false;
        }
        return serverKey == null ? otherServerKey == null : serverKey.equals(otherServerKey);
    }
}
