package de.zannagh.armorhider.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Time-limited debug logger for diagnosing rendering pipeline issues.
 * Writes to a dedicated timestamped file under {@code logs/armorHiderLog/}
 * (next to the normal game log) instead of polluting the general game log.
 * <p>
 * Callers should always guard expensive string formatting behind
 * {@link #isEnabled()} — the check is a single volatile read.
 */
public final class DebugLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("armor-hider-debug");
    private static final String PREFIX = "[ArmorHider DEBUG] ";
    private static final long DURATION_MS = 5L * 60L * 1000L;

    private static final Path LOG_DIR = Path.of("logs", "armorHiderLog");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter LINE_TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final AtomicLong enabledUntilMillis = new AtomicLong(0);
    private static final AtomicReference<BufferedWriter> activeWriter = new AtomicReference<>(null);

    private DebugLogger() {}

    public static void enable() {
        long until = System.currentTimeMillis() + DURATION_MS;
        enabledUntilMillis.set(until);

        BufferedWriter writer = openLogFile();
        BufferedWriter prev = activeWriter.getAndSet(writer);
        closeQuietly(prev);

        LOGGER.info(PREFIX + "Debug logging ENABLED for 5 minutes — writing to logs/armorHiderLog/");
    }

    public static void disable() {
        enabledUntilMillis.set(0);
        BufferedWriter writer = activeWriter.getAndSet(null);
        closeQuietly(writer);
        LOGGER.info(PREFIX + "Debug logging DISABLED");
    }

    public static boolean isEnabled() {
        long until = enabledUntilMillis.get();
        if (until > 0 && System.currentTimeMillis() < until) {
            return true;
        }
        if (until > 0) {
            // Timer expired — clean up
            enabledUntilMillis.set(0);
            BufferedWriter writer = activeWriter.getAndSet(null);
            closeQuietly(writer);
        }
        return false;
    }

    public static long remainingSeconds() {
        long until = enabledUntilMillis.get();
        if (until <= 0) return 0;
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public static void log(String msg) {
        if (isEnabled()) {
            writeLine(msg);
        }
    }

    public static void log(String msg, Object arg) {
        if (isEnabled()) {
            writeLine(MessageFormatter.format(msg, arg).getMessage());
        }
    }

    public static void log(String msg, Object arg1, Object arg2) {
        if (isEnabled()) {
            writeLine(MessageFormatter.format(msg, arg1, arg2).getMessage());
        }
    }

    public static void log(String msg, Object... args) {
        if (isEnabled()) {
            writeLine(MessageFormatter.arrayFormat(msg, args).getMessage());
        }
    }

    private static void writeLine(String message) {
        BufferedWriter writer = activeWriter.get();
        if (writer == null) return;
        try {
            String timestamp = LocalDateTime.now().format(LINE_TIMESTAMP);
            writer.write(timestamp);
            writer.write(" ");
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOGGER.warn(PREFIX + "Failed to write debug log line", e);
        }
    }

    private static BufferedWriter openLogFile() {
        try {
            Files.createDirectories(LOG_DIR);
            String filename = LocalDateTime.now().format(FILE_TIMESTAMP) + ".log";
            Path file = LOG_DIR.resolve(filename);
            return Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to create debug log file", e);
            return null;
        }
    }

    private static void closeQuietly(BufferedWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {}
        }
    }
}
