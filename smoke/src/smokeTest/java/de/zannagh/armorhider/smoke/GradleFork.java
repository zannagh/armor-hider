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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private static final long TREE_EXIT_WAIT_MS = 5_000;

    /** Exit code synthesised when we kill the child for exceeding its wall-clock ceiling. */
    static final int TIMEOUT_EXIT_CODE = 124;

    /**
     * Every forked {@code ./gradlew} still running, so a shutdown hook can reap their whole process
     * trees if THIS JVM (the {@code :smoke:test} runner) is itself interrupted (Ctrl-C / SIGTERM)
     * mid-row - otherwise the in-flight Minecraft JVM would orphan alongside its gradle parent.
     */
    private static final Set<Process> LIVE_FORKS = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process fork : LIVE_FORKS) {
                if (fork.isAlive()) {
                    destroyTree(fork);
                }
            }
        }, "smoke-fork-reaper"));
    }

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
     * Exit code synthesised when a crash signature appears anywhere in the child output - deliberately
     * NOT gated on the marker, so a crash BEFORE the boot arms fails fast instead of waiting out the
     * ceiling, and a crash after the marker is never masked by the grace window.
     */
    static final int CRASH_EXIT_CODE = 1;

    /**
     * Kills the child - synthesising exit code {@code 0} - once {@code marker} has been seen and the
     * boot has settled. "Settled" is EITHER the output going silent for {@code idleThresholdMs} OR
     * {@code graceAfterMarkerMs} having elapsed since the marker. The grace path exists because the
     * marker fires at DIFFERENT boot stages per loader: on Fabric it lands late (a good "booted"
     * proxy, and the client then idles), but on NeoForge {@code ArmorHider.init()} runs during mod
     * loading, so the marker is premature and the client keeps logging its way to the title screen -
     * it never goes idle, and the run task doesn't cleanly hand back, so idle-only detection ran every
     * NeoForge boot to the ceiling and reported a false failure. Grace turns "armed, then survived
     * {@code graceAfterMarkerMs} without crashing" into a pass. A crash signature in the meantime
     * (see {@link #CRASH_SIGNATURES}) still fails - so a boot that actually dies is not masked.
     */
    static Result runUntilIdleAfterMarker(List<String> command, File workingDirectory, String marker,
                                          long idleThresholdMs, long graceAfterMarkerMs, long ceilingMs)
            throws IOException, InterruptedException {
        Capture capture = start(command, workingDirectory, marker);
        long startedAt = System.currentTimeMillis();
        while (true) {
            Result finished = capture.finishedResult();
            if (finished != null) {
                return finished;
            }
            long now = System.currentTimeMillis();
            if (capture.sawCrash.get()) {
                return capture.kill(CRASH_EXIT_CODE);
            }
            if (now - startedAt > ceilingMs) {
                return capture.kill(TIMEOUT_EXIT_CODE);
            }
            if (capture.sawMarker.get()) {
                boolean idle = now - capture.lastLineAt.get() >= idleThresholdMs;
                boolean gracePassed = now - capture.markerAt.get() >= graceAfterMarkerMs;
                if (idle || gracePassed) {
                    return capture.kill(0);
                }
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
        LIVE_FORKS.add(process);
        Capture capture = new Capture(process, marker);
        capture.startReader();
        return capture;
    }

    /**
     * Forcibly destroys {@code process} AND every descendant. The heavy Minecraft JVM is a descendant
     * of the forked {@code ./gradlew} (gradle launcher -&gt; forked game JVM), so destroying only the
     * top process leaves it orphaned - the exact multi-GB stray this reaps. Descendants are snapshotted
     * BEFORE the root is killed, because once it exits its children are reparented to init and drop off
     * {@link Process#descendants()}.
     */
    static void destroyTree(Process process) {
        List<ProcessHandle> tree = new ArrayList<>();
        process.descendants().forEach(tree::add);
        tree.add(process.toHandle());
        for (ProcessHandle handle : tree) {
            handle.destroyForcibly();
        }
        for (ProcessHandle handle : tree) {
            try {
                handle.onExit().get(TREE_EXIT_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
                // Best-effort: a handle we cannot await (already gone, or not awaitable on this
                // platform) is fine - the destroyForcibly signal was already delivered above.
            }
        }
    }

    /**
     * Unambiguous fatal-crash lines. A normal smoke shutdown (SmokeMode's {@code System.exit}) logs
     * "Stopping server"/"Saving worlds" and the like - deliberately NOT matched here - so grace only
     * masks a hung hand-back, never an actual crash.
     */
    private static final List<String> CRASH_SIGNATURES = List.of(
            "Minecraft has crashed!",
            "---- Minecraft Crash Report ----",
            "Exception in thread \"main\"",
            "A fatal error has been detected by the Java Runtime Environment");

    /** A running child plus the state the two completion strategies poll. */
    private static final class Capture {

        private final Process process;
        private final String marker;
        private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong lastLineAt = new AtomicLong(System.currentTimeMillis());
        private final AtomicBoolean sawMarker = new AtomicBoolean(false);
        /** When the marker was first seen (used for the grace window); 0 until then. */
        private final AtomicLong markerAt = new AtomicLong(0);
        /** Set once a {@link #CRASH_SIGNATURES} line appears - a real crash, so fail rather than pass. */
        private final AtomicBoolean sawCrash = new AtomicBoolean(false);
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
                    long at = System.currentTimeMillis();
                    lastLineAt.set(at);
                    if (marker != null && line.contains(marker) && sawMarker.compareAndSet(false, true)) {
                        markerAt.set(at);
                    }
                    for (String crash : CRASH_SIGNATURES) {
                        if (line.contains(crash)) {
                            sawCrash.set(true);
                            break;
                        }
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
            LIVE_FORKS.remove(process);
            reader.join(READER_JOIN_MS);
            return new Result(process.exitValue(), lines);
        }

        private Result kill(int exitCode) throws InterruptedException {
            destroyTree(process);
            LIVE_FORKS.remove(process);
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
