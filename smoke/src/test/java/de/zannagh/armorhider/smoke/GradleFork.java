package de.zannagh.armorhider.smoke;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Forks {@code ./gradlew} as a child process, mirrors its combined output to this JVM's stdout and
 * returns the captured lines alongside an exit code.
 *
 * <p>Two completion strategies exist because the two smoke phases end differently:</p>
 * <ul>
 *   <li>{@link #runToExit(List, File, long)} - the child exits on its own (FCGT calls
 *       {@code System.exit} when the gametest returns), so the real exit code is meaningful.</li>
 *   <li>{@link #runUntilIdleAfterMarker(List, File, String, long, long)} - a plain
 *       {@code runClient} never exits, so we watch for a readiness marker plus a silence window and
 *       tear the child down ourselves.</li>
 * </ul>
 *
 * <p>The output reader always runs on its own thread so the caller can enforce a real wall-clock
 * ceiling: a blocking {@code readLine()} on the main thread never reaches {@code waitFor()} when a
 * fabric-loader popup hangs the child with stdout still open.</p>
 */
final class GradleFork {

    private static final String REPO_ROOT_PROPERTY = "armorhider.repo.root";
    private static final long POLL_INTERVAL_MS = 200;
    private static final long READER_JOIN_MS = 2_000;

    /** Exit code synthesised when we kill the child for exceeding its wall-clock ceiling. */
    static final int TIMEOUT_EXIT_CODE = 124;

    private GradleFork() {
    }

    /** Waits for the child to exit on its own, killing it once {@code ceilingMs} has elapsed. */
    static Result runToExit(List<String> command, File workingDirectory, long ceilingMs)
            throws IOException, InterruptedException {
        Capture capture = start(command, workingDirectory, null);
        long startedAt = System.currentTimeMillis();
        while (true) {
            Result finished = capture.finishedResult();
            if (finished != null) {
                return finished;
            }
            if (System.currentTimeMillis() - startedAt > ceilingMs) {
                return capture.kill(TIMEOUT_EXIT_CODE);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
    }

    /**
     * Kills the child - and synthesises exit code {@code 0} - once {@code marker} has been seen and
     * the output has then been silent for {@code idleThresholdMs}. Gradle would otherwise report the
     * killed JVM as a failure even though the boot itself succeeded.
     */
    static Result runUntilIdleAfterMarker(List<String> command, File workingDirectory, String marker,
                                          long idleThresholdMs, long ceilingMs)
            throws IOException, InterruptedException {
        Capture capture = start(command, workingDirectory, marker);
        long startedAt = System.currentTimeMillis();
        while (true) {
            Result finished = capture.finishedResult();
            if (finished != null) {
                return finished;
            }
            long now = System.currentTimeMillis();
            if (now - startedAt > ceilingMs) {
                return capture.kill(TIMEOUT_EXIT_CODE);
            }
            if (capture.sawMarker.get() && now - capture.lastLineAt.get() >= idleThresholdMs) {
                return capture.kill(0);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
    }

    /** The gradle wrapper invocation for this platform. */
    static String gradleScript() {
        return isWindows() ? "gradlew.bat" : "./gradlew";
    }

    /** Repository root, forwarded by the Gradle test task, or discovered by walking upwards. */
    static Path repoRoot() {
        String property = System.getProperty(REPO_ROOT_PROPERTY);
        if (property != null && !property.isBlank()) {
            return Paths.get(property);
        }
        Path here = Paths.get("").toAbsolutePath();
        for (Path candidate = here; candidate != null; candidate = candidate.getParent()) {
            if (candidate.resolve("gradlew").toFile().isFile()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not locate repo root: set -D" + REPO_ROOT_PROPERTY
                + " or run from inside the repo");
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static Capture start(List<String> command, File workingDirectory, String marker)
            throws IOException {
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(true)
                .start();
        Capture capture = new Capture(process, marker);
        capture.startReader();
        return capture;
    }

    /** A running child plus the state the two completion strategies poll. */
    private static final class Capture {

        private final Process process;
        private final String marker;
        private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong lastLineAt = new AtomicLong(System.currentTimeMillis());
        private final AtomicBoolean sawMarker = new AtomicBoolean(false);
        private Thread reader;

        private Capture(Process process, String marker) {
            this.process = process;
            this.marker = marker;
        }

        private void startReader() {
            reader = new Thread(this::pump, "smoke-stdout-reader");
            reader.setDaemon(true);
            reader.start();
        }

        private void pump() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    lines.add(line);
                    System.out.println(line);
                    lastLineAt.set(System.currentTimeMillis());
                    if (marker != null && line.contains(marker)) {
                        sawMarker.set(true);
                    }
                }
            } catch (IOException ignored) {
                // Pipe closed - normally because we just destroyed the child below.
            }
        }

        private Result finishedResult() throws InterruptedException {
            if (process.isAlive()) {
                return null;
            }
            reader.join(READER_JOIN_MS);
            return new Result(process.exitValue(), lines);
        }

        private Result kill(int exitCode) throws InterruptedException {
            process.destroyForcibly();
            reader.join(READER_JOIN_MS);
            return new Result(exitCode, lines);
        }
    }

    /** Exit code plus every captured output line of one gradle invocation. */
    record Result(int exitCode, List<String> lines) {
        String tail(int count) {
            int from = Math.max(0, lines.size() - count);
            return String.join("\n", lines.subList(from, lines.size()));
        }
    }
}
