package de.zannagh.armorhider.smoke.paper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * One Paper JVM: launch, stream its stdout, wait for a marker, SIGTERM it.
 *
 * <p>stdin is redirected from the platform null device and never written to. Once Paper's console
 * reader sees EOF it stops reading forever while the server keeps running, with no diagnostic - so
 * the only supported way to stop a server here is a signal.</p>
 */
final class PaperProcess implements AutoCloseable {

    private final StringBuffer log = new StringBuffer();
    private final Process process;
    private final Thread logPump;
    private Integer exitCode;

    PaperProcess(List<String> command, Path workingDirectory, Path logFile) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        builder.redirectInput(ProcessBuilder.Redirect.from(nullDevice()));
        process = builder.start();
        logPump = startLogPump(logFile);
    }

    /**
     * The platform's null device. {@code ./gradlew smokeTest} is a supported Windows dev flow, and
     * {@code /dev/null} does not exist there - {@code ProcessBuilder} would fail the launch outright
     * rather than degrade.
     */
    private static File nullDevice() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        return new File(windows ? "NUL" : "/dev/null");
    }

    /** Everything the process has written to stdout/stderr so far. */
    String getLog() {
        return log.toString();
    }

    /** Exit code once stopped, or {@code null} while still running. */
    Integer getExitCode() {
        return exitCode;
    }

    /**
     * Blocks until {@code marker} appears in the output.
     *
     * @throws IllegalStateException if a failure marker appears, the process dies, or time runs out
     */
    void awaitMarker(String marker, List<String> failureMarkers, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            String text = getLog();
            if (text.contains(marker)) {
                return;
            }
            for (String failure : failureMarkers) {
                if (text.contains(failure)) {
                    throw new IllegalStateException("Paper reported a fatal condition: " + failure
                            + tail(text));
                }
            }
            if (!process.isAlive()) {
                throw new IllegalStateException("Paper exited (code " + process.exitValue()
                        + ") before reaching '" + marker + "'." + tail(text));
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Paper did not reach '" + marker + "' within " + timeout
                + tail(getLog()));
    }

    /** SIGTERM, then SIGKILL if the graceful path stalls. */
    void stop(Duration timeout) {
        if (!process.isAlive() && exitCode != null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(15, TimeUnit.SECONDS);
            }
            exitCode = process.exitValue();
            logPump.join(TimeUnit.SECONDS.toMillis(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    @Override
    public void close() {
        stop(Duration.ofSeconds(45));
    }

    /**
     * Mirrors the stdout pipe into memory and a file. The pipe is read rather than
     * {@code logs/latest.log} because the file appender is async and lags, and its line format
     * differs - so only message bodies are ever matched.
     */
    private Thread startLogPump(Path logFile) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter writer = Files.newBufferedWriter(logFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append(line).append('\n');
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, "paper-stdout-pump");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static String tail(String text) {
        List<String> lines = text.lines().toList();
        int from = Math.max(0, lines.size() - 40);
        return "\n--- last " + (lines.size() - from) + " log lines ---\n"
                + String.join("\n", lines.subList(from, lines.size()));
    }
}
