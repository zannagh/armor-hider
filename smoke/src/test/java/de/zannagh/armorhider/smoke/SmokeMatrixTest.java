package de.zannagh.armorhider.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Assertions;

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
import java.util.stream.Stream;

/**
 * Forks {@code ./gradlew :<loader>:<variant>:runClient -Psmoke -Pcompat=<set>} once per
 * (loader, variant, compat-set) combination and asserts the child gradle process exits 0.
 * <p>
 * Each test entry shows in the IDE test runner (IntelliJ groups them by display name). On
 * failure, the last 80 lines of stderr from the gradle invocation are attached to the assertion
 * so you can see the actual crash without digging through {@code run/crash-reports/}.
 * <p>
 * Filtering knobs (set via JVM system properties on the gradle test invocation, e.g.
 * {@code ./gradlew :smoke:test -Dsmoke.only=fabric-26.2}):
 * <ul>
 *   <li>{@code -Dsmoke.only=<variant>[,<variant>]} — run only the listed variants
 *       (e.g. {@code fabric-26.2}, {@code neoforge-1.21.10}).</li>
 *   <li>{@code -Dsmoke.exclude=<variant>[,<variant>]} — skip the listed variants.</li>
 *   <li>{@code -Dsmoke.compat=all|none|key1,key2} — override the compat set for every combo.
 *       Default runs each variant twice: once with {@code all}, once with {@code none}.</li>
 *   <li>{@code -Dsmoke.delay.ms=<n>} — boot window before the smoke timer exits cleanly.
 *       Default 15000. Raise on cold CI.</li>
 * </ul>
 */
@DisplayName("MC boot smoke matrix")
class SmokeMatrixTest {

    private static final String REPO_ROOT_PROP = "armorhider.repo.root";
    /** Hard wall-clock ceiling per test row regardless of phase. Belt-and-braces: a hung
     *  popup-type stall is what makes this matter — without a real wall-clock watchdog
     *  the stdout reader blocks indefinitely and the test row eats matrix budget. */
    private static final long ROW_HARD_CEILING_MS = 8 * 60 * 1000;
    /**
     * Marker line emitted by {@code SmokeMode.maybeArm()} once {@code ArmorHider.init()}
     * has run cleanly. Empirically this lands ~4 s into a Fabric boot and is followed by
     * a few seconds of texture/atlas logging before the JVM idles until the smoke timer
     * fires. We use this marker + a silence window to short-circuit the timer.
     * <p>
     * Tied to our own log output rather than a Mojang-controlled string so it doesn't
     * drift with MC releases.
     */
    private static final String BOOT_READY_MARKER = "[smoke] Armed: will exit JVM";
    /** Cut the boot row short once we've gone this many ms without a new log line. */
    private static final long BOOT_IDLE_THRESHOLD_MS = 3_000;
    /** Hard ceiling — if we never see the marker, kill the JVM and call it a failure. */
    private static final long BOOT_HARD_CEILING_MS = 90_000;

    private static final List<String> FABRIC_VARIANTS = List.of(
            "fabric-1.20.1", "fabric-1.21.1", "fabric-1.21.4", "fabric-1.21.8",
            "fabric-1.21.10", "fabric-1.21.11", "fabric-26.1.2", "fabric-26.2"
    );
    private static final List<String> NEOFORGE_VARIANTS = List.of(
            "neoforge-1.21.1", "neoforge-1.21.4", "neoforge-1.21.8",
            "neoforge-1.21.10", "neoforge-1.21.11", "neoforge-26.1.2", "neoforge-26.2"
    );
    /**
     * Variants where the FCGT (entity-render) phase is wired through the build —
     * i.e. those that pin {@code fabricapi.semver} in {@code stonecutter.properties.toml}.
     * The {@code fcgt} stonecutter constant, the FCGT module dep, the
     * {@code fabric-client-gametest} entrypoint and the {@code runClientGametest} run task
     * all auto-activate from that single property pin.
     * <p>
     * fabric-1.21.1 is intentionally excluded: fabric-api 0.116.x predates the FCGT module
     * (it was introduced in 0.119.x for 1.21.4), so Phase 2 is unsupportable upstream.
     * 1.20.1 is also excluded for the same reason (older fabric-api releases).
     */
    private static final List<String> FCGT_VARIANTS = List.of(
            "fabric-1.21.4", "fabric-1.21.8", "fabric-1.21.10", "fabric-1.21.11",
            "fabric-26.1.2", "fabric-26.2"
    );

    enum Phase {
        /** Phase 1 — boots client, verifies no startup crash within smoke.delay.ms. */
        BOOT("runClient"),
        /** Phase 2 — FCGT drives client into a singleplayer world, equips a player,
         *  switches to third-person, renders frames. Fabric variants in {@link #FCGT_VARIANTS}. */
        ENTITY_RENDER("runClientGametest");

        final String gradleTask;
        Phase(String gradleTask) { this.gradleTask = gradleTask; }
    }

    static Stream<Arguments> matrix() {
        String only = System.getProperty("smoke.only", "").trim();
        String exclude = System.getProperty("smoke.exclude", "").trim();
        String compatOverride = System.getProperty("smoke.compat", "").trim();
        String phaseFilter = System.getProperty("smoke.phase", "").trim().toLowerCase();

        List<String> onlySet = only.isEmpty() ? List.of()
                : List.of(only.split(",")).stream().map(String::trim).toList();
        List<String> excludeSet = exclude.isEmpty() ? List.of()
                : List.of(exclude.split(",")).stream().map(String::trim).toList();

        List<String> compatSets = compatOverride.isEmpty()
                ? List.of("all", "none")            // default: bare + full-stack
                : List.of(compatOverride);

        boolean wantBoot = phaseFilter.isEmpty() || phaseFilter.equals("boot");
        boolean wantEntityRender = phaseFilter.isEmpty() || phaseFilter.equals("entity-render")
                || phaseFilter.equals("entityrender") || phaseFilter.equals("render");

        List<String> variants = new ArrayList<>();
        variants.addAll(FABRIC_VARIANTS);
        variants.addAll(NEOFORGE_VARIANTS);

        List<Arguments> rows = new ArrayList<>();
        for (String variant : variants) {
            if (!onlySet.isEmpty() && !onlySet.contains(variant)) continue;
            if (excludeSet.contains(variant)) continue;
            String loader = variant.split("-")[0];
            for (String compat : compatSets) {
                if (wantBoot) {
                    rows.add(Arguments.of(loader, variant, compat, Phase.BOOT));
                }
                if (wantEntityRender && FCGT_VARIANTS.contains(variant)) {
                    rows.add(Arguments.of(loader, variant, compat, Phase.ENTITY_RENDER));
                }
            }
        }
        return rows.stream();
    }

    @ParameterizedTest(name = "{3} {1} compat={2}")
    @MethodSource("matrix")
    void launches_without_crashing(String loader, String variant, String compat, Phase phase) throws Exception {
        Path repoRoot = repoRoot();
        String gradleScript = isWindows() ? "gradlew.bat" : "./gradlew";

        List<String> cmd = new ArrayList<>();
        cmd.add(gradleScript);
        cmd.add(":" + loader + ":" + variant + ":" + phase.gradleTask);
        cmd.add("-Psmoke");
        cmd.add("-Pcompat=" + compat);
        cmd.add("-Psmoke.delay.ms=" + System.getProperty("smoke.delay.ms", "15000"));
        cmd.add("--console=plain");
        cmd.add("--no-daemon");

        Result r = run(cmd, repoRoot.toFile(), phase);

        if (r.exitCode != 0) {
            Assertions.fail(String.format(
                    "%s smoke of %s (compat=%s) exited with %d.%n"
                            + "Command: %s%n"
                            + "Last 80 lines of output:%n%s",
                    phase.name(), variant, compat, r.exitCode, String.join(" ", cmd), r.tail(80)));
        }
    }

    /**
     * Phase routes to a different completion strategy:
     * <ul>
     *   <li>{@link Phase#BOOT}: marker + silence — kill the subprocess once we've seen
     *       {@link #BOOT_READY_MARKER} and logs have been idle for
     *       {@link #BOOT_IDLE_THRESHOLD_MS}. Synthesises exit code 0 because gradle
     *       reports the killed JVM as a failure even though boot succeeded.</li>
     *   <li>{@link Phase#ENTITY_RENDER}: FCGT calls {@code System.exit} when
     *       {@code EntityRenderSmokeTest.runTest()} returns — wait for the natural exit
     *       and use the real exit code.</li>
     * </ul>
     */
    private static Result run(List<String> cmd, File cwd, Phase phase) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(cwd).redirectErrorStream(true);
        Process p = pb.start();

        return phase == Phase.BOOT
                ? runBootWithIdleDetection(p)
                : runWaitForExit(p);
    }

    private static Result runBootWithIdleDetection(Process p) throws InterruptedException {
        final long startedAt = System.currentTimeMillis();
        final AtomicLong lastLineAt = new AtomicLong(startedAt);
        final AtomicBoolean sawMarker = new AtomicBoolean(false);
        final List<String> lines = Collections.synchronizedList(new ArrayList<>());

        Thread reader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    lines.add(line);
                    System.out.println(line);
                    lastLineAt.set(System.currentTimeMillis());
                    if (line.contains(BOOT_READY_MARKER)) {
                        sawMarker.set(true);
                    }
                }
            } catch (IOException ignored) {
                // Subprocess pipe closed — usual reason is we just destroyed it below.
            }
        }, "smoke-stdout-reader");
        reader.setDaemon(true);
        reader.start();

        while (true) {
            if (!p.isAlive()) {
                // Subprocess exited on its own — use the real gradle exit code.
                reader.join(2_000);
                return new Result(p.exitValue(), lines);
            }
            long now = System.currentTimeMillis();
            long idleMs = now - lastLineAt.get();
            long elapsedMs = now - startedAt;

            if (elapsedMs > BOOT_HARD_CEILING_MS) {
                p.destroyForcibly();
                reader.join(2_000);
                return new Result(124, lines); // timeout — return non-zero so the test fails
            }
            if (sawMarker.get() && idleMs >= BOOT_IDLE_THRESHOLD_MS) {
                // Boot completed cleanly. Tear down the JVM ourselves and synthesise success.
                p.destroyForcibly();
                reader.join(2_000);
                return new Result(0, lines);
            }
            Thread.sleep(200);
        }
    }

    private static Result runWaitForExit(Process p) throws InterruptedException {
        // Spawn the reader in a background thread so the main thread can enforce a real
        // wall-clock ceiling. A blocking readLine() in the main thread doesn't get to a
        // waitFor() until the child closes stdout, and a hung fabric-loader popup window
        // never does — that's how the 22-minute row earlier slipped past the timeout.
        final long startedAt = System.currentTimeMillis();
        final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        Thread reader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    lines.add(line);
                    System.out.println(line);
                }
            } catch (IOException ignored) { /* pipe closed by destroyForcibly() */ }
        }, "smoke-stdout-reader");
        reader.setDaemon(true);
        reader.start();

        while (true) {
            if (!p.isAlive()) {
                reader.join(2_000);
                return new Result(p.exitValue(), lines);
            }
            if (System.currentTimeMillis() - startedAt > ROW_HARD_CEILING_MS) {
                p.destroyForcibly();
                reader.join(2_000);
                return new Result(124, lines);
            }
            Thread.sleep(200);
        }
    }

    private static Path repoRoot() {
        String prop = System.getProperty(REPO_ROOT_PROP);
        if (prop != null && !prop.isBlank()) return Paths.get(prop);
        // Fallback: walk up from the working dir until we see gradlew.
        Path here = Paths.get("").toAbsolutePath();
        for (Path p = here; p != null; p = p.getParent()) {
            if (p.resolve("gradlew").toFile().isFile()) return p;
        }
        throw new IllegalStateException(
                "Could not locate repo root: set -D" + REPO_ROOT_PROP + " or run from inside the repo");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private record Result(int exitCode, List<String> lines) {
        String tail(int n) {
            int from = Math.max(0, lines.size() - n);
            return String.join("\n", lines.subList(from, lines.size()));
        }
    }
}
