package de.zannagh.armorhider.util;

/**
 * An exponential backoff helper following IEE802 standards.
 * Increases the delay exponentially with each attempt (shouldContinue() call) from a base of 52.1 microseconds.
 * If the calculated attempt count exceeds 16, it will be capped at 16 (effective delay of ~16.384 seconds).
 */
public class ExponentialBackoffHelper {
    private int attempts = 1;
    private static final double BASE_DELAY_MILLIS = 0.521; // 52.1 microseconds in ms
    private final int maxAttempts;
    private final long initialMillis;

    public ExponentialBackoffHelper(int maxDelayMillis) {
        double maxAttemptsDouble = Math.log((maxDelayMillis / BASE_DELAY_MILLIS) + 2) / Math.log(2);
        int maxAttemptsByDelay = Math.max(1, (int) Math.floor(maxAttemptsDouble));
        this.maxAttempts = Math.min(maxAttemptsByDelay, 16);
        initialMillis = System.currentTimeMillis();
    }

    private int getDelayMillis() {
        return Math.toIntExact(Math.round(BASE_DELAY_MILLIS * Math.pow(2, attempts)));
    }
    
    public boolean hasTimedOut;
    
    public int getElapsedMillisSinceFirstAttempt() {
        return (int) (System.currentTimeMillis() - initialMillis);
    }
    
    public boolean shouldContinue() {
        if (attempts >= maxAttempts) {
            hasTimedOut = true;
            return false;
        }
        var delayMillis = getDelayMillis();
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            hasTimedOut = true;
            return false;
        }
        attempts++;
        return true;
    }
}
