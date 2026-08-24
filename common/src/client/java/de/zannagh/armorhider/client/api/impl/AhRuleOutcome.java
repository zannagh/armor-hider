package de.zannagh.armorhider.client.api.impl;

import org.jetbrains.annotations.ApiStatus;

/**
 * Result of evaluating every registered rule for one target: the opacity that should be used and
 * whether the glint has to be suppressed. {@code changed} is {@code false} when no rule matched,
 * which lets the caller keep its own values untouched.
 */
@ApiStatus.Internal
public record AhRuleOutcome(double opacity, boolean disableGlint, boolean changed) {

    private static final AhRuleOutcome UNCHANGED = new AhRuleOutcome(1.0, false, false);

    /** Shared "no rule matched" instance. */
    public static AhRuleOutcome unchanged() {
        return UNCHANGED;
    }
}
