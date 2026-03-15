package de.zannagh.armorhider.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Time-limited debug logger for diagnosing rendering pipeline issues.
 * Uses a separate SLF4J logger ({@code armor-hider-debug}) and prefixes all
 * messages with {@code [ArmorHider DEBUG]} so they are easy to filter in the
 * Minecraft log file.
 * <p>
 * Callers should always guard expensive string formatting behind
 * {@link #isEnabled()} — the check is a single volatile read.
 */
public final class DebugLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("armor-hider-debug");
    private static final String PREFIX = "[ArmorHider DEBUG] ";
    private static final long DURATION_MS = 5L * 60L * 1000L;

    private static final AtomicLong enabledUntilMillis = new AtomicLong(0);

    private DebugLogger() {}

    public static void enable() {
        long until = System.currentTimeMillis() + DURATION_MS;
        enabledUntilMillis.set(until);
        LOGGER.info(PREFIX + "Debug logging ENABLED for 5 minutes (until {})", until);
    }

    public static void disable() {
        enabledUntilMillis.set(0);
        LOGGER.info(PREFIX + "Debug logging DISABLED");
    }

    public static boolean isEnabled() {
        long until = enabledUntilMillis.get();
        return until > 0 && System.currentTimeMillis() < until;
    }

    public static long remainingSeconds() {
        long until = enabledUntilMillis.get();
        if (until <= 0) return 0;
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public static void log(String msg) {
        if (isEnabled()) {
            LOGGER.info(formatMessage(msg), msg);
        }
    }

    public static void log(String msg, Object arg) {
        if (isEnabled()) {
            LOGGER.info(formatMessage(msg), arg);
        }
    }

    public static void log(String msg, Object arg1, Object arg2) {
        if (isEnabled()) {
            LOGGER.info(formatMessage(msg), arg1, arg2);
        }
    }

    public static void log(String msg, Object... args) {
        if (isEnabled()) {
            LOGGER.info(formatMessage(msg), args);
        }
    }

    private static String formatMessage(String message) {
        return PREFIX + message;
    }
}
