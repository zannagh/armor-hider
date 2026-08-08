package de.zannagh.armorhider.smoke;

import de.zannagh.armorhider.smoke.paper.PaperEnvironment;
import de.zannagh.armorhider.smoke.paper.PaperServer;
import de.zannagh.armorhider.smoke.paper.PaperServerDownloader;
import de.zannagh.armorhider.smoke.paper.PaperServerSeed;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Phase 3 — the only row in the suite that runs a real client against a real <em>server</em>.
 *
 * <p>Boots a PaperMC server carrying the Armor Hider plugin, then forks
 * {@code ./gradlew :fabric:<variant>:runClientGametest -Psmoke.paper.port=<port>} so
 * {@code PaperHandshakeSmokeTest} connects to it. That covers both directions:</p>
 * <ul>
 *   <li><b>S2C</b> — asserted client-side: the received {@code ServerConfiguration} carries the four
 *       deliberately inverted seeded values, and {@code ArmorHiderClient.permissionLevel} is 4.</li>
 *   <li><b>C2S</b> — asserted here, after the client has exited and the plugin has saved on
 *       shutdown: the connecting player exists under both {@code playerConfigs} (by UUID) and
 *       {@code playerNameConfigs} (by name).</li>
 * </ul>
 *
 * <p><b>The Paper version must equal the client version</b> — a mismatched protocol fails the
 * handshake before a single payload moves — so it is derived from the variant name rather than from
 * {@code smoke.paper.mc}.</p>
 *
 * <p>Opt-in via {@code -Dsmoke.paper.e2e} (or {@code -Dsmoke.phase=paper-e2e}) and, by default,
 * limited to {@value #DEFAULT_VARIANT}: a six-variant matrix means six Paper downloads and six cold
 * client boots. Widen with the usual {@code -Dsmoke.only} / {@code -Dsmoke.exclude}. Everything
 * environment-dependent (no network, no suitable JDK) degrades to a JUnit skip.</p>
 *
 * <p>Rows span Paper builds with different Java requirements (21 for 1.21.x, 25 for 26.x). The
 * Gradle test task provisions one launcher per feature release and forwards them as
 * {@code smoke.paper.java.<n>}; {@link PaperEnvironment#resolveJavaOrSkip} picks the exact match for
 * whatever the Paper API declares, and skips the row - never fails it - when that JDK is not
 * installed. Pass {@code -Dsmoke.paper.java.<n>=<path>} to steer one explicitly.</p>
 *
 * <p>Each row registers only the {@code paper-handshake} FCGT entrypoint (see {@link #FCGT_TEST_ID}),
 * so it cannot be reddened by an unrelated sibling gametest sharing the same client launch.</p>
 */
@ResourceLock("smoke-server")
@DisplayName("Paper end-to-end smoke (client <-> Paper server)")
// Overrides the suite-wide 10 m default: a Paper boot plus a cold client gametest exceeds it. The
// real watchdogs are PAPER_BOOT_TIMEOUT and CLIENT_CEILING_MS, which produce diagnosable failures.
@Timeout(value = 25, unit = TimeUnit.MINUTES)
class PaperE2ESmokeTest {

    /** Only this variant runs unless {@code -Dsmoke.only} widens the set. */
    private static final String DEFAULT_VARIANT = "fabric-26.2";

    /** Paper's first boot generates a world; 180 s is the same budget PaperServerSmokeTest uses. */
    private static final Duration PAPER_BOOT_TIMEOUT = Duration.ofSeconds(180);

    /** A cold {@code runClientGametest} downloads assets and remaps — minutes, not seconds. */
    private static final long CLIENT_CEILING_MS = 12 * 60 * 1000;

    /** {@code onDisable} runs on the graceful path; the final save lands there. */
    private static final Duration SHUTDOWN_LOG_TIMEOUT = Duration.ofSeconds(20);

    /** Lines of Paper log attached to a failing assertion. */
    private static final int PAPER_LOG_TAIL_LINES = 60;

    /**
     * FCGT entrypoint id registered for these rows, matching the catalog in
     * {@code multiloader-loom.gradle.kts}. An unknown id fails the fork loudly rather than
     * registering nothing and passing vacuously.
     */
    private static final String FCGT_TEST_ID = "paper-handshake";

    /** Plugin log line proving the combat-log C2S packet reached the server. */
    private static final String COMBAT_LOG_RECEIVED = "Server received combat log packet from";

    /** Markers PaperHandshakeSmokeTest logs, used to attribute a red gametest run. */
    private static final String HANDSHAKE_START = "Paper handshake smoke starting";
    private static final String HANDSHAKE_PASS = "Paper handshake smoke passed";

    static Stream<String> variants() {
        List<String> only = VariantFilter.only();
        List<String> exclude = VariantFilter.exclude();
        List<String> candidates = only.isEmpty() ? List.of(DEFAULT_VARIANT) : only;

        List<String> rows = new ArrayList<>();
        for (String variant : candidates) {
            if (exclude.contains(variant) || !SmokeMatrixTest.FCGT_VARIANTS.contains(variant)) {
                continue;
            }
            rows.add(variant);
        }
        // An empty @MethodSource is a JUnit error, so keep one row that will simply skip itself.
        return rows.isEmpty() ? Stream.of(DEFAULT_VARIANT) : rows.stream();
    }

    @ParameterizedTest(name = "PAPER_E2E {0}")
    @MethodSource("variants")
    @DisplayName("client handshakes with a real Paper server in both directions")
    void clientHandshakesWithPaper(String variant) throws Exception {
        Assumptions.assumeTrue(optedIn(),
                "PAPER_E2E is opt-in: pass -Dsmoke.paper.e2e (or -Dsmoke.phase=paper-e2e)");
        Assumptions.assumeTrue(SmokeMatrixTest.FCGT_VARIANTS.contains(variant),
                variant + " has no runClientGametest task");

        String minecraftVersion = paperVersionFor(variant);
        Path pluginJar = PaperEnvironment.locatePluginJar();
        PaperServerDownloader.PaperBuild build =
                PaperEnvironment.resolveOrSkip(serverProject(), minecraftVersion);
        Path java = PaperEnvironment.resolveJavaOrSkip(build.javaVersion());
        Path serverDirectory = Files.createTempDirectory("armor-hider-paper-e2e");

        System.out.println("[paper-e2e] variant=" + variant + " -> " + build.project() + " "
                + minecraftVersion + " build " + build.build() + " (" + build.channel()
                + ", java " + build.javaVersion() + ")");
        System.out.println("[paper-e2e] plugin=" + pluginJar);
        System.out.println("[paper-e2e] serverDir=" + serverDirectory);

        PaperServer server = new PaperServer(build, pluginJar, serverDirectory, java);
        try {
            bootServer(server);
            runClientAgainst(variant, server);
            assertClientToServerDirection(server);
        } finally {
            server.close();
            System.out.println("[paper-e2e] paper exit code " + server.getExitCode()
                    + ", log kept at " + server.getLogFile());
        }
    }

    private static void bootServer(PaperServer server) throws Exception {
        long startedAt = System.nanoTime();
        server.start();
        server.awaitReady(PAPER_BOOT_TIMEOUT);
        System.out.println("[paper-e2e] ready on port " + server.getPort() + " after "
                + (System.nanoTime() - startedAt) / 1_000_000 + " ms");
        System.out.println("[paper-e2e] ready line: " + readyLine(server.getLogText()));
    }

    /**
     * Forks the gametest and uses the real exit code — FCGT calls {@code System.exit} once
     * {@code runTest()} returns, so unlike the BOOT phase there is nothing to scrape or kill.
     */
    private static void runClientAgainst(String variant, PaperServer server) throws Exception {
        String loader = variant.split("-")[0];
        List<String> command = List.of(
                GradleFork.gradleScript(),
                ":" + loader + ":" + variant + ":runClientGametest",
                "-Psmoke.paper.port=" + server.getPort(),
                // One client launch runs every registered FCGT entrypoint, so without this the row
                // inherits the health of every sibling test - WaterTransparencySmokeTest is red on
                // 26.2 today for reasons that have nothing to do with Paper. Registering only the
                // handshake makes each row report on its own merits, and skips the world build and
                // screenshot passes the other tests need.
                "-Psmoke.fcgt.only=" + FCGT_TEST_ID,
                "--console=plain",
                "--no-daemon");
        System.out.println("[paper-e2e] " + String.join(" ", command));

        GradleFork.Result result =
                GradleFork.runToExit(command, GradleFork.repoRoot().toFile(), CLIENT_CEILING_MS);
        if (result.exitCode() != 0) {
            Assertions.fail(failureMessage(variant, command, result, server));
        }
        // A zero exit code is NOT sufficient evidence the handshake ran. If FCGT's module is missing
        // from the runtime classpath the client boots vanilla, idles at the title screen and exits
        // 0 - the gametest task "succeeds" having executed no test at all. That is exactly how this
        // matrix looked green on fabric-26.2 (which had a stale FCGT jar in run/mods) while doing
        // nothing on every other variant. Demand positive proof from the test's own log marker.
        if (result.lines().stream().noneMatch(line -> line.contains(HANDSHAKE_PASS))) {
            Assertions.fail(String.format(
                    "PAPER_E2E client gametest for %s exited 0 but never logged \"%s\", so the"
                            + " handshake test did not actually run.%n"
                            + "The usual cause is FCGT not being on the runtime classpath (no"
                            + " \"(fabric-client-gametest-api-v1)\" lines below): the client then boots"
                            + " vanilla, idles and exits zero.%n"
                            + "Command: %s%nLast 80 lines of gradle output:%n%s",
                    variant, HANDSHAKE_PASS, String.join(" ", command), result.tail(80)));
        }
        System.out.println("[paper-e2e] client gametest passed for " + variant);
    }

    /**
     * {@code runClientGametest} runs <em>every</em> registered FCGT entrypoint in one client launch,
     * so a red exit code does not necessarily mean the handshake failed. Say up front which it was,
     * otherwise a failure in an unrelated sibling gametest reads as a Paper problem.
     */
    private static String failureMessage(String variant, List<String> command,
                                         GradleFork.Result result, PaperServer server) {
        boolean handshakeRan = result.lines().stream().anyMatch(line -> line.contains(HANDSHAKE_START));
        boolean handshakePassed = result.lines().stream().anyMatch(line -> line.contains(HANDSHAKE_PASS));
        String verdict;
        if (handshakePassed) {
            verdict = "The Paper handshake itself PASSED — the failure is in another FCGT test in the"
                    + " same client run. Check whether that test is also red without a Paper server.";
        } else if (handshakeRan) {
            verdict = "The Paper handshake started but did not pass — this is a genuine E2E failure.";
        } else {
            verdict = "The Paper handshake never started — the client died before reaching it.";
        }
        return String.format(
                "PAPER_E2E client gametest for %s exited with %d%s.%n%s%n"
                        + "Command: %s%n"
                        + "Last 80 lines of gradle output:%n%s%n"
                        + "Last %d lines of the Paper log:%n%s",
                variant, result.exitCode(),
                result.exitCode() == GradleFork.TIMEOUT_EXIT_CODE ? " (wall-clock timeout)" : "",
                verdict, String.join(" ", command), result.tail(80),
                PAPER_LOG_TAIL_LINES, lastLines(server.getCombinedLogText(), PAPER_LOG_TAIL_LINES));
    }

    /**
     * The client-side half already asserted S2C. This is the other direction: the plugin must have
     * received the joining player's config packet, stored it under both indexes and persisted it.
     * The state only reaches disk on the {@code onDisable} save, so shut the server down first.
     */
    private static void assertClientToServerDirection(PaperServer server) throws Exception {
        server.close();
        Assertions.assertTrue(
                server.awaitLatestLogContains("Disabling ArmorHider", SHUTDOWN_LOG_TIMEOUT),
                () -> "Plugin was never disabled — onDisable (and its final save) did not run.\n"
                        + lastLines(server.getCombinedLogText(), PAPER_LOG_TAIL_LINES));

        // The client drove real /damage and its combat hook fired (asserted client-side). This is the
        // other half: the resulting CombatLogEventPacket actually reached the plugin. It travels on
        // `de.zannagh.armorhider:combatlog_c2s_packet` on every version >= 1.20.5, unlike the settings
        // channel, so it is the only coverage of the second namespace in the C2S direction.
        String paperLog = server.getCombinedLogText();
        if (!paperLog.contains(COMBAT_LOG_RECEIVED)) {
            Assertions.fail("Plugin never logged \"" + COMBAT_LOG_RECEIVED + "\" - the combat-log C2S"
                    + " packet did not reach the server even though the client entered combat.\n"
                    + lastLines(paperLog, PAPER_LOG_TAIL_LINES));
        }
        System.out.println("[paper-e2e] combat-log C2S verified: plugin received the event");

        Path levelDirectory = server.getLevelDirectory();
        System.out.println("[paper-e2e] configs: " + PaperServerSeed.existingConfigFiles(levelDirectory));
        try {
            PaperServerSeed.assertSeedSurvived(levelDirectory);
            PaperServerSeed.assertPlayerConfigStored(levelDirectory);
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + "\nLast " + PAPER_LOG_TAIL_LINES
                    + " lines of the Paper log:\n"
                    + lastLines(server.getCombinedLogText(), PAPER_LOG_TAIL_LINES), e);
        }
        System.out.println("[paper-e2e] C2S verified: player config persisted by UUID and by name");
    }

    /**
     * Which server to boot: {@code paper} (default) or {@code folia}.
     *
     * <p>Folia is the only target whose runtime genuinely differs, and this row is the only place
     * the region-thread paths - {@code ChannelSubscriber} on join, {@code PacketSender} sending from
     * a region thread, {@code Schedulers#runAsync} on a received config packet - are ever executed.
     * {@code FoliaServerSmokeTest} proves the plugin boots and detects Folia; this proves it
     * actually talks. Folia ships a subset of Paper's versions, so a variant Folia never released
     * skips rather than fails.</p>
     */
    private static String serverProject() {
        return System.getProperty("smoke.paper.project", PaperServerDownloader.PAPER);
    }

    /** {@code fabric-26.2} -> {@code 26.2}. The protocol has no tolerance for a mismatch here. */
    private static String paperVersionFor(String variant) {
        int separator = variant.indexOf('-');
        if (separator < 0 || separator == variant.length() - 1) {
            throw new IllegalArgumentException("Cannot derive a Minecraft version from " + variant);
        }
        return variant.substring(separator + 1);
    }

    private static boolean optedIn() {
        if (System.getProperty("smoke.paper.e2e") != null) {
            return true;
        }
        String phase = System.getProperty("smoke.phase", "").trim().toLowerCase();
        return phase.equals("paper-e2e") || phase.equals("papere2e") || phase.equals("e2e");
    }

    private static String readyLine(String log) {
        return log.lines()
                .filter(line -> line.contains(PaperServer.READY_MARKER))
                .findFirst()
                .orElse("<not found>");
    }

    private static String lastLines(String log, int count) {
        List<String> lines = log.lines().toList();
        return String.join("\n", lines.subList(Math.max(0, lines.size() - count), lines.size()));
    }
}
