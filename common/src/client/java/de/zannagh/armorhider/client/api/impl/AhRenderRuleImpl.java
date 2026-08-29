package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.api.AhHideContext;
import de.zannagh.armorhider.client.api.AhRenderRule;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * One registered rule. A rule either carries an opacity override, suppresses the glint, or both -
 * see {@link #affectsOpacity()} / {@link #disableGlint()}.
 */
@ApiStatus.Internal
public final class AhRenderRuleImpl implements AhRenderRule {

    /**
     * How many <em>consecutive</em> throws a predicate is allowed before the rule is dropped. A
     * transient failure - the classic being an unguarded {@code ctx.player()} on a frame where the
     * entity is out of render distance - must not kill a consumer's rule for the session, but a
     * predicate that throws every single frame is broken and would otherwise spin forever.
     */
    public static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final AhRuleTarget target;
    private final int priority;
    private final boolean affectsOpacity;
    private final double opacity;
    private final boolean disableGlint;
    private final Predicate<AhHideContext> condition;
    private final @Nullable Object owner;
    /** Whether this rule's outcome for the local player is broadcast to the other clients. */
    private final boolean shared;

    /**
     * Re-log interval. A rule that throws on only <em>some</em> evaluations never reaches
     * {@link #MAX_CONSECUTIVE_FAILURES} and so is never dropped; with a one-shot log it would emit a
     * single line early in the session and then misbehave invisibly forever. Re-logging on an
     * interval keeps it diagnosable without spamming a frame-rate loop.
     */
    private static final long LOG_INTERVAL_NANOS = 60_000_000_000L;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong lastLogNanos = new AtomicLong();
    private volatile boolean everLogged;
    private volatile boolean registered = true;

    public AhRenderRuleImpl(AhRuleTarget target,
                            int priority,
                            boolean affectsOpacity,
                            double opacity,
                            boolean disableGlint,
                            Predicate<AhHideContext> condition,
                            @Nullable Object owner,
                            boolean shared) {
        this.target = target;
        this.priority = priority;
        this.affectsOpacity = affectsOpacity;
        this.opacity = opacity;
        this.disableGlint = disableGlint;
        this.condition = condition;
        this.owner = owner;
        this.shared = shared;
    }

    public AhRuleTarget target() {
        return target;
    }

    @Override
    public int priority() {
        return priority;
    }

    public boolean affectsOpacity() {
        return affectsOpacity;
    }

    public double opacity() {
        return opacity;
    }

    public boolean disableGlint() {
        return disableGlint;
    }

    public Predicate<AhHideContext> condition() {
        return condition;
    }

    public @Nullable Object owner() {
        return owner;
    }

    /** @see de.zannagh.armorhider.client.api.AhRenderRuleBuilder#shared() */
    public boolean shared() {
        return shared;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    /** Clears the consecutive-failure streak; a rule that works again is forgiven entirely. */
    public void recordSuccess() {
        if (consecutiveFailures.get() != 0) {
            consecutiveFailures.set(0);
        }
    }

    /**
     * Logs the first failure of this rule and counts the streak.
     *
     * @return {@code true} when the rule has now thrown {@link #MAX_CONSECUTIVE_FAILURES} times in a
     * row and should be dropped.
     */
    public boolean recordFailureAndShouldDisable(Throwable throwable) {
        int failures = consecutiveFailures.incrementAndGet();
        // Only ever compares two real nanoTime() readings: lastLogNanos is read solely once
        // everLogged is true, by which point it holds an actual reading rather than a sentinel.
        long now = System.nanoTime();
        if (!everLogged || now - lastLogNanos.get() > LOG_INTERVAL_NANOS) {
            everLogged = true;
            lastLogNanos.set(now);
            ArmorHider.LOGGER.error(
                    "Armor Hider render rule for {} (priority {}) threw. It is treated as non-matching; "
                            + "it will be unregistered only if it throws {} times in a row. Repeats of this "
                            + "message are rate-limited to one per minute.",
                    target, priority, MAX_CONSECUTIVE_FAILURES, throwable);
        }
        if (failures < MAX_CONSECUTIVE_FAILURES) {
            return false;
        }
        ArmorHider.LOGGER.error("Armor Hider render rule for {} (priority {}) threw {} times in a row "
                + "and has been unregistered.", target, priority, failures);
        return true;
    }

    void markUnregistered() {
        registered = false;
    }

    @Override
    public void unregister() {
        AhRenderRuleRegistryImpl.unregister(this);
    }
}
