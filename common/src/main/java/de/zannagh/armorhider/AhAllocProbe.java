package de.zannagh.armorhider;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Test-only allocation counter for the render hot path, used by {@code HotPathAllocSmokeTest} to assert
 * that rendering a frame does not build configuration graphs.
 * <p>
 * The regression this guards against: {@code AhRenderStateImpl.getActiveScope(scope)} answered a MISS -
 * the normal case - with a freshly built {@code RenderScopeContext.empty(scope)}, which built a
 * {@code SlotModification.empty()} plus a {@code RenderModifications.empty()} (which built a second
 * {@code SlotModification.empty()}), each calling {@code new PlayerConfig()}: a ~37-allocation
 * constructor that also drew a {@code UUID.randomUUID()} from a synchronized {@code SecureRandom}. The
 * hand mixins {@code ModelPartMixin} and {@code ItemRendererMixin} ran that twice per model part
 * (recursively, per entity, per frame) and twice per baked quad. Frame time, unlike an allocation count,
 * is not assertable on CI's software GL - so the test asserts the count instead.
 * <p>
 * This lives in the common (non-client) source set because {@code PlayerConfig}, the class that reports
 * into it, does too; a client-only probe like {@code AhArmProbe} is not reachable from there. It
 * references nothing outside {@code java.*}, so it compiles and is inert on the Paper/server variants.
 * <p>
 * Disabled by default and effectively free when disabled: {@link #recordPlayerConfigAllocation()} reads
 * one {@code volatile boolean} and returns. Counting is additionally restricted to the single thread that
 * called {@link #enable()} (the client/render thread, since the smoke test arms it from
 * {@code runOnClient}), so background config I/O and netty threads can never colour the result.
 */
public final class AhAllocProbe {

    private static volatile boolean enabled = false;

    /**
     * The thread whose allocations are counted - captured by {@link #enable()}. Only reads happen on other
     * threads, and a stale read there can only mean "not my thread", i.e. no count, so no extra ordering
     * beyond the {@code volatile} {@link #enabled} flag is needed.
     */
    private static volatile Thread watchedThread = null;

    private static final AtomicLong PLAYER_CONFIG_ALLOCATIONS = new AtomicLong();

    /** Total {@code resolveConfig(name)} calls seen on the watched thread while armed. */
    private static final AtomicLong RESOLVE_CONFIG_CALLS = new AtomicLong();

    /**
     * {@code resolveConfig(name)} calls whose name was NOT the local player's - i.e. the calls that miss the
     * {@code return CURRENT} fast path and fall through to the allocating remote-player resolution.
     */
    private static final AtomicLong RESOLVE_CONFIG_REMOTE_CALLS = new AtomicLong();

    private AhAllocProbe() {
    }

    /**
     * Arms the probe and resets the counter, watching the calling thread. Call this from the render thread
     * (i.e. inside {@code ClientGameTestContext.runOnClient}).
     */
    public static void enable() {
        PLAYER_CONFIG_ALLOCATIONS.set(0);
        RESOLVE_CONFIG_CALLS.set(0);
        RESOLVE_CONFIG_REMOTE_CALLS.set(0);
        watchedThread = Thread.currentThread();
        enabled = true;
    }

    public static void disable() {
        enabled = false;
        watchedThread = null;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Reports one {@code new PlayerConfig()}. Called from the constructor itself, so it must stay cheap and
     * must never throw.
     */
    public static void recordPlayerConfigAllocation() {
        if (!enabled) {
            return;
        }
        if (Thread.currentThread() != watchedThread) {
            return;
        }
        PLAYER_CONFIG_ALLOCATIONS.incrementAndGet();
    }

    public static long playerConfigAllocationCount() {
        return PLAYER_CONFIG_ALLOCATIONS.get();
    }

    /**
     * Reports one {@code ArmorHiderPlayerConfigApi#resolveConfig(String)} entry.
     *
     * @param local whether the requested name resolved via the local-player fast path (shared {@code CURRENT}
     *              instance, no allocation).
     */
    public static void recordResolveConfig(boolean local) {
        if (!enabled) {
            return;
        }
        if (Thread.currentThread() != watchedThread) {
            return;
        }
        RESOLVE_CONFIG_CALLS.incrementAndGet();
        if (!local) {
            RESOLVE_CONFIG_REMOTE_CALLS.incrementAndGet();
        }
    }

    public static long resolveConfigCallCount() {
        return RESOLVE_CONFIG_CALLS.get();
    }

    public static long resolveConfigRemoteCallCount() {
        return RESOLVE_CONFIG_REMOTE_CALLS.get();
    }
}
