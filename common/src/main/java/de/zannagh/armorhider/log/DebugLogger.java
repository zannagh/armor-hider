package de.zannagh.armorhider.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

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

    private static final int FLUSH_INTERVAL = 50;

    private static final AtomicLong enabledUntilMillis = new AtomicLong(0);

    // Guarded by LOCK
    private static BufferedWriter activeWriter;
    private static int linesSinceFlush;

    private static final Object LOCK = new Object();

    private DebugLogger() {}

    public static void enable() {
        synchronized (LOCK) {
            BufferedWriter writer = openLogFile();
            if (writer == null) {
                LOGGER.error(PREFIX + "Failed to open debug log file — debug logging NOT enabled");
                return;
            }
            closeQuietly(activeWriter);
            activeWriter = writer;
            linesSinceFlush = 0;
            enabledUntilMillis.set(System.currentTimeMillis() + DURATION_MS);
        }
        LOGGER.info(PREFIX + "Debug logging ENABLED for 5 minutes — writing to logs/armorHiderLog/");
    }

    public static void disable() {
        enabledUntilMillis.set(0);
        synchronized (LOCK) {
            closeQuietly(activeWriter);
            activeWriter = null;
        }
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
            writeLine(msg, null);
        }
    }

    public static void log(String msg, Object arg) {
        if (isEnabled()) {
            FormattingTuple ft = MessageFormatter.format(msg, arg);
            writeLine(ft.getMessage(), ft.getThrowable());
        }
    }

    public static void log(String msg, Object arg1, Object arg2) {
        if (isEnabled()) {
            FormattingTuple ft = MessageFormatter.format(msg, arg1, arg2);
            writeLine(ft.getMessage(), ft.getThrowable());
        }
    }

    public static void log(String msg, Object... args) {
        if (isEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(msg, args);
            writeLine(ft.getMessage(), ft.getThrowable());
        }
    }

    private static void writeLine(String message, Throwable throwable) {
        synchronized (LOCK) {
            if (activeWriter == null) return;

            // Check expiry while we hold the lock
            if (!isEnabled()) {
                enabledUntilMillis.set(0);
                closeQuietly(activeWriter);
                activeWriter = null;
                return;
            }

            try {
                String timestamp = LocalDateTime.now().format(LINE_TIMESTAMP);
                activeWriter.write(timestamp);
                activeWriter.write(" ");
                activeWriter.write(message);
                activeWriter.newLine();
                if (throwable != null) {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    activeWriter.write(sw.toString());
                }
                linesSinceFlush++;
                if (linesSinceFlush >= FLUSH_INTERVAL) {
                    activeWriter.flush();
                    linesSinceFlush = 0;
                }
            } catch (IOException e) {
                LOGGER.warn(PREFIX + "Failed to write debug log line", e);
            }
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
                writer.flush();
                writer.close();
            } catch (IOException ignored) {}
        }
    }
}
