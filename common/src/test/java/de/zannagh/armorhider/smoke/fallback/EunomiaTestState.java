package de.zannagh.armorhider.smoke.fallback;

import de.zannagh.eunomia.networking.comms.CommunicationManager;

import java.lang.reflect.Field;

/**
 * Resets the eunomia static state that {@link CommunicationManager#resetForTesting()} does not reach.
 *
 * <p>{@code StoreSyncClient} binds the shared {@code eunomia:store_sync} handler onto the
 * {@link CommunicationManager} exactly once, guarded by a {@code static volatile boolean registered} and a
 * one-shot {@code ensureRegistered()}. {@code resetForTesting()} clears the manager's handler table but leaves
 * that flag set, so the next {@code ReplicatedClientStore.enableClient()} early-returns and never rebinds the
 * handler to the fresh manager. The socket then delivers {@code store_sync} frames that nothing is listening
 * for, and snapshots are dropped in silence - no error, just an empty store.
 *
 * <p>The symptom is order-dependent and therefore nasty: whichever fallback test runs first passes, and the
 * next one to rely on a snapshot fails on an assertion that looks like a relay bug. Both
 * {@link HttpFallbackE2ETest} and {@link LiveRelayContractTest} were seen to fail this way, each depending on
 * which ran first.
 *
 * <p>This is a eunomia-side leak of test state, not an armor-hider bug. The upstream fix is one line - have
 * {@code resetForTesting()} clear {@code StoreSyncClient.registered} - and this class should be deleted once
 * that lands. Until then, calling {@link #rebindStoreSync()} straight after every {@code resetForTesting()}
 * keeps the suite order-independent instead of passing by luck.
 */
final class EunomiaTestState {

    private EunomiaTestState() {
    }

    /**
     * Clears {@code StoreSyncClient.registered} so the next {@code enableClient()} rebinds the {@code store_sync}
     * handler onto the current {@link CommunicationManager}.
     *
     * <p>Deliberately silent when the field cannot be found: if a future eunomia release fixes the leak and drops
     * the flag, this becomes a no-op rather than a suite-wide failure. A genuine regression would still surface
     * as the snapshot assertion it protects.
     */
    static void rebindStoreSync() {
        try {
            Class<?> storeSync = Class.forName("de.zannagh.eunomia.keyed.StoreSyncClient");
            Field registered = storeSync.getDeclaredField("registered");
            registered.setAccessible(true);
            registered.setBoolean(null, false);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            // The field is gone or sealed off - either eunomia fixed this upstream, or the internals moved.
            // Neither is a reason to fail a test that has not run yet.
        }
    }
}
