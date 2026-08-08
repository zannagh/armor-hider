package de.zannagh.armorhider.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Assertions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
 *   <li>{@code -Dsmoke.only=<variant>[,<variant>]} - run only the listed variants
 *       (e.g. {@code fabric-26.2}, {@code neoforge-1.21.10}).</li>
 *   <li>{@code -Dsmoke.exclude=<variant>[,<variant>]} - skip the listed variants.</li>
 *   <li>{@code -Dsmoke.compat=all|none|key1,key2} - override the compat set for every combo.
 *       Default runs each variant twice: once with {@code all}, once with {@code none}.</li>
 *   <li>{@code -Dsmoke.delay.ms=<n>} - boot window before the smoke timer exits cleanly.
 *       Default 15000. Raise on cold CI.</li>
 * </ul>
 * <p>
 * The client-plus-Paper end-to-end phase lives in {@link PaperE2ESmokeTest}
 * because it needs a server process alongside the forked client. The in-game FCGT scenarios are
 * batched here (one {@code runClientGametest} launch runs them all) for the FCGT variants EXCEPT
 * {@link #FCGT_PER_ID_VARIANT}, which {@link FcgtScenarioTest} covers one launch per scenario so
 * each shows as its own IDE node.
 */
@DisplayName("MC boot smoke matrix")
class SmokeMatrixTest {

    /** Hard wall-clock ceiling per test row regardless of phase. Belt-and-braces: a hung
     *  popup-type stall is what makes this matter - without a real wall-clock watchdog
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
    /** Hard ceiling - if we never see the marker, kill the JVM and call it a failure. */
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
     * Variants where the FCGT (entity-render) phase is wired through the build -
     * i.e. those that pin {@code fabricapi.semver} in {@code stonecutter.properties.toml}.
     * The {@code fcgt} stonecutter constant, the FCGT module dep, the
     * {@code fabric-client-gametest} entrypoint and the {@code runClientGametest} run task
     * all auto-activate from that single property pin.
     * <p>
     * fabric-1.21.1 is intentionally excluded: fabric-api 0.116.x predates the FCGT module
     * (it was introduced in 0.119.x for 1.21.4), so Phase 2 is unsupportable upstream.
     * 1.20.1 is also excluded for the same reason (older fabric-api releases).
     */
    static final List<String> FCGT_VARIANTS = List.of(
            "fabric-1.21.4", "fabric-1.21.8", "fabric-1.21.10", "fabric-1.21.11",
            "fabric-26.1.2", "fabric-26.2"
    );

    /**
     * The FCGT variant covered per-scenario by {@link FcgtScenarioTest} instead of by the batched
     * {@code runClientGametest} row here. {@code FcgtScenarioTest} forks one launch per FCGT id on
     * this variant, giving each scenario its own IDE-discoverable node - so a batched all-in-one
     * ENTITY_RENDER row for the same variant would only duplicate (coarser) coverage. The other
     * {@link #FCGT_VARIANTS} keep their single batched row for cross-version breadth.
     */
    static final String FCGT_PER_ID_VARIANT = "fabric-26.2";

    enum Phase {
        /** Phase 1 - boots client, verifies no startup crash within smoke.delay.ms. */
        BOOT("runClient"),
        /** Phase 2 - FCGT drives client into a singleplayer world, equips a player,
         *  switches to third-person, renders frames. Fabric variants in {@link #FCGT_VARIANTS}. */
        ENTITY_RENDER("runClientGametest");

        final String gradleTask;
        Phase(String gradleTask) { this.gradleTask = gradleTask; }
    }

    static Stream<Arguments> matrix() {
        String compatOverride = System.getProperty("smoke.compat", "").trim();
        String phaseFilter = System.getProperty("smoke.phase", "").trim().toLowerCase();

        List<String> onlySet = VariantFilter.only();
        List<String> excludeSet = VariantFilter.exclude();

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
            if (!onlySet.isEmpty() && !onlySet.contains(variant)) {
                continue;
            }
            if (excludeSet.contains(variant)) {
                continue;
            }
            String loader = variant.split("-")[0];
            for (String compat : compatSets) {
                if (wantBoot) {
                    rows.add(Arguments.of(loader, variant, compat, Phase.BOOT));
                }
                if (wantEntityRender && FCGT_VARIANTS.contains(variant)
                        && !variant.equals(FCGT_PER_ID_VARIANT)) {
                    rows.add(Arguments.of(loader, variant, compat, Phase.ENTITY_RENDER));
                }
            }
        }
        return rows.stream();
    }

    /**
     * Phase routes to a different completion strategy:
     * <ul>
     *   <li>{@link Phase#BOOT}: marker + silence - kill the subprocess once we've seen
     *       {@link #BOOT_READY_MARKER} and logs have been idle for
     *       {@link #BOOT_IDLE_THRESHOLD_MS}. Synthesises exit code 0 because gradle
     *       reports the killed JVM as a failure even though boot succeeded.</li>
     *   <li>{@link Phase#ENTITY_RENDER}: FCGT calls {@code System.exit} when
     *       {@code EntityRenderSmokeTest.runTest()} returns - wait for the natural exit
     *       and use the real exit code.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{3} {1} compat={2}")
    @MethodSource("matrix")
    void launches_without_crashing(String loader, String variant, String compat, Phase phase) throws Exception {
        Path repoRoot = GradleFork.repoRoot();

        List<String> cmd = new ArrayList<>();
        cmd.add(GradleFork.gradleScript());
        cmd.add(":" + loader + ":" + variant + ":" + phase.gradleTask);
        cmd.add("-Psmoke");
        cmd.add("-Pcompat=" + compat);
        cmd.add("-Psmoke.delay.ms=" + System.getProperty("smoke.delay.ms", "15000"));
        cmd.add("--console=plain");
        cmd.add("--no-daemon");

        GradleFork.Result r = phase == Phase.BOOT
                ? GradleFork.runUntilIdleAfterMarker(cmd, repoRoot.toFile(), BOOT_READY_MARKER,
                        BOOT_IDLE_THRESHOLD_MS, BOOT_HARD_CEILING_MS)
                : GradleFork.runToExit(cmd, repoRoot.toFile(), ROW_HARD_CEILING_MS);

        if (r.exitCode() != 0) {
            Assertions.fail(String.format(
                    "%s smoke of %s (compat=%s) exited with %d.%n"
                            + "Command: %s%n"
                            + "Last 80 lines of output:%n%s",
                    phase.name(), variant, compat, r.exitCode(), String.join(" ", cmd), r.tail(80)));
        }
    }
}
