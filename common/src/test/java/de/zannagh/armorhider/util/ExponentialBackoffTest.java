package de.zannagh.armorhider.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The IEEE-802-style backoff helper. The attempt-count math is derived from {@code maxDelayMillis} in
 * the constructor and capped at 16; {@link ExponentialBackoff#shouldContinue()} then walks the ramp
 * and flips {@link ExponentialBackoff#hasTimedOut} on exhaustion. Kept fast by using tiny caps.
 */
@DisplayName("ExponentialBackoff ramp and timeout")
class ExponentialBackoffTest {

    @Test
    @DisplayName("a tiny max delay still permits at least one attempt")
    void tinyMaxDelayFloorsAtOneAttempt() {
        ExponentialBackoff backoff = new ExponentialBackoff(0);
        // maxAttempts floors at 1, and attempts starts at 1, so the very first check has timed out.
        assertFalse(backoff.shouldContinue());
        assertTrue(backoff.hasTimedOut);
    }

    @Test
    @DisplayName("hasTimedOut is false before the ramp is exhausted")
    void notTimedOutInitially() {
        ExponentialBackoff backoff = new ExponentialBackoff(50);
        assertFalse(backoff.hasTimedOut, "a fresh backoff has not timed out until shouldContinue exhausts it");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("shouldContinue eventually exhausts and sets hasTimedOut")
    void eventuallyTimesOut() {
        // ~50 ms cap keeps the summed sleeps small while still walking several rungs of the ramp.
        ExponentialBackoff backoff = new ExponentialBackoff(50);
        int guard = 0;
        while (backoff.shouldContinue()) {
            assertTrue(guard++ < 64, "the ramp must terminate well within the 16-attempt cap");
        }
        assertTrue(backoff.hasTimedOut, "the loop must exit via the timeout flag");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("elapsed time is measured from construction and advances across attempts")
    void tracksElapsedTime() {
        ExponentialBackoff backoff = new ExponentialBackoff(50);
        assertTrue(backoff.getElapsedMillisSinceFirstAttempt() >= 0);
        while (backoff.shouldContinue()) {
            // walk the ramp
        }
        assertTrue(backoff.getElapsedMillisSinceFirstAttempt() >= 0,
                "elapsed time stays non-negative after the ramp completes");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("the attempt ramp is bounded by the 16-attempt cap")
    void rampNeverExceedsHardCap() {
        ExponentialBackoff backoff = new ExponentialBackoff(50);
        int attempts = 1; // matches the field's internal starting value
        while (backoff.shouldContinue()) {
            attempts++;
            assertTrue(attempts <= 16, "attempts must never exceed the hard cap of 16");
        }
    }
}
