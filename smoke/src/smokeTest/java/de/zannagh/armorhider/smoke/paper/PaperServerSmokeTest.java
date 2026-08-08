package de.zannagh.armorhider.smoke.paper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Boots a real PaperMC server with the Armor Hider plugin and proves the server half of the
 * end-to-end harness on its own, before any client is involved.
 *
 * <p>Asserts the ready line, the plugin's own enable message, an error-free log, and that the
 * pre-seeded {@code armor-hider.json} survived a load/save round trip through {@code onDisable}.
 * Everything network-dependent degrades to a skip, so a CI runner without egress stays green.</p>
 */
@ResourceLock("smoke-server")
@DisplayName("Paper server boot smoke")
class PaperServerSmokeTest {

    /** Log line emitted by {@code ArmorHiderPlugin.onEnable}. */
    private static final String PLUGIN_READY_MARKER = "Armor Hider server relay enabled";

    private static final Duration BOOT_TIMEOUT = Duration.ofSeconds(180);

    @Test
    @DisplayName("boots Paper with the plugin, reads the seeded config, shuts down cleanly")
    void bootsPaperWithPlugin() throws Exception {
        String minecraftVersion = System.getProperty("smoke.paper.mc", "26.2");
        Path pluginJar = PaperEnvironment.locatePluginJar();
        PaperServerDownloader.PaperBuild build =
                PaperEnvironment.resolveOrSkip(minecraftVersion);
        Path java = PaperEnvironment.resolveJavaOrSkip(build.javaVersion());
        Path serverDirectory = Files.createTempDirectory("armor-hider-paper-smoke");

        System.out.println("[paper-smoke] paper " + minecraftVersion + " build " + build.build()
                + " (java " + build.javaVersion() + ") jar=" + build.jar());
        System.out.println("[paper-smoke] plugin=" + pluginJar);
        System.out.println("[paper-smoke] serverDir=" + serverDirectory);

        long startedAt = System.nanoTime();
        try (PaperServer server = new PaperServer(build, pluginJar, serverDirectory, java)) {
            server.start();
            server.awaitReady(BOOT_TIMEOUT);
            long bootMillis = (System.nanoTime() - startedAt) / 1_000_000;
            System.out.println("[paper-smoke] ready on port " + server.getPort() + " after "
                    + bootMillis + " ms");
            System.out.println("[paper-smoke] ready line: " + readyLine(server.getLogText()));

            assertPluginEnabled(server.getLogText());
            assertNoErrors(server.getLogText());
            server.close();
            System.out.println("[paper-smoke] exit code " + server.getExitCode());
            System.out.println("[paper-smoke] shutdown tail:\n"
                    + lastLines(server.getLatestLogText(), 4));

            assertCleanShutdown(server);
            assertSeedRoundTripped(server);
        } finally {
            System.out.println("[paper-smoke] log kept at " + serverDirectory.resolve("smoke-stdout.log"));
        }
    }

    /**
     * Reproduces the state a server left behind by a pre-fix plugin build on 26.x: a config in the
     * overworld <em>dimension</em> directory, where {@code World#getWorldFolder()} used to point.
     * The plugin must move it to the level root instead of stranding it and starting from defaults.
     *
     * <p>Skipped on 1.21.x, where {@code getWorldFolder()} already was the level root and there is
     * nothing to migrate.</p>
     */
    @Test
    @DisplayName("migrates a config stranded in the dimension directory to the level root")
    void migratesStrandedDimensionConfig() throws Exception {
        String minecraftVersion = System.getProperty("smoke.paper.mc", "26.2");
        Path pluginJar = PaperEnvironment.locatePluginJar();
        PaperServerDownloader.PaperBuild build = PaperEnvironment.resolveOrSkip(minecraftVersion);
        Path java = PaperEnvironment.resolveJavaOrSkip(build.javaVersion());
        Path serverDirectory = Files.createTempDirectory("armor-hider-paper-migrate");

        try (PaperServer server = new PaperServer(build, pluginJar, serverDirectory, java)) {
            Assumptions.assumeTrue(server.usesPerDimensionLayout(),
                    "Only 26.x ever wrote into a dimension directory");
            server.prepare();
            // The dimension directory only exists after a world has been generated, and creating it
            // by hand hard-fails 26.2 boot - hence the extra plugin-less boot here.
            server.generateWorld();
            Path stranded = PaperServerSeed.writeStranded(server.getLevelDirectory());
            System.out.println("[paper-smoke] stranded config at " + stranded);

            server.launch();
            server.awaitReady(BOOT_TIMEOUT);
            Assertions.assertTrue(server.getLogText().contains("Migrated config out of the dimension"),
                    () -> "Plugin did not log the dimension-directory migration.\n"
                            + server.getLogText());
            server.close();

            Assertions.assertFalse(Files.exists(stranded),
                    () -> "Stranded config was left behind at " + stranded);
            System.out.println("[paper-smoke] configs after migration: "
                    + PaperServerSeed.existingConfigFiles(server.getLevelDirectory()));
            PaperServerSeed.assertSeedSurvived(server.getLevelDirectory());
        } finally {
            System.out.println("[paper-smoke] log kept at "
                    + serverDirectory.resolve("smoke-stdout.log"));
        }
    }

    private static void assertPluginEnabled(String log) {
        Assertions.assertTrue(log.contains(PLUGIN_READY_MARKER),
                () -> "Plugin never logged '" + PLUGIN_READY_MARKER + "'.\n" + log);
    }

    /** Any SEVERE/ERROR line or stack trace fails the run - the plugin must boot silently. */
    private static void assertNoErrors(String log) {
        List<String> offenders = log.lines()
                .filter(line -> line.contains("/ERROR]") || line.contains("SEVERE")
                        || line.contains("Exception") || line.contains("Caused by:"))
                .toList();
        Assertions.assertTrue(offenders.isEmpty(),
                () -> "Server log contains errors:\n" + String.join("\n", offenders));
    }

    /**
     * SIGTERM must reach the graceful path - {@code onDisable} (and therefore the plugin's final
     * save) only runs there. "Stopping the server" is the first line Paper's shutdown hook emits.
     */
    private static void assertCleanShutdown(PaperServer server) throws InterruptedException {
        // Bukkit logs "Disabling <plugin>" immediately before calling onDisable, which is where
        // the plugin saves - so it is the strongest available proof the graceful path ran.
        Assertions.assertTrue(
                server.awaitLatestLogContains("Disabling ArmorHider", Duration.ofSeconds(10)),
                () -> "Plugin was never disabled - onDisable did not run.\n"
                        + lastLines(server.getCombinedLogText(), 30));
        Assertions.assertTrue(server.getCombinedLogText().contains("Stopping server"),
                () -> "SIGTERM did not produce a graceful shutdown.\n"
                        + lastLines(server.getCombinedLogText(), 30));
    }

    /**
     * The plugin rewrites the config in {@code onDisable}. If it had not read the seed it would
     * write defaults back, so surviving values prove the seeded file was loaded.
     */
    private static void assertSeedRoundTripped(PaperServer server) throws IOException {
        Path levelDirectory = server.getLevelDirectory();
        System.out.println("[paper-smoke] configs: "
                + PaperServerSeed.existingConfigFiles(levelDirectory));
        PaperServerSeed.assertSeedSurvived(levelDirectory);
    }

    private static String lastLines(String log, int count) {
        List<String> lines = log.lines().toList();
        return String.join("\n", lines.subList(Math.max(0, lines.size() - count), lines.size()));
    }

    private static String readyLine(String log) {
        return log.lines()
                .filter(line -> line.contains(PaperServer.READY_MARKER))
                .findFirst()
                .orElse("<not found>");
    }

    /**
     * The offline UUID must match what the server derives for the same name, otherwise the ops
     * entry silently fails to apply and the client's permission-level assertion loses its teeth.
     */
    @Test
    @DisplayName("smoke player identity is the offline UUID and is opped at level 4")
    void smokePlayerIsOpped() {
        Assertions.assertEquals(
                java.util.UUID.nameUUIDFromBytes(
                        "OfflinePlayer:ArmorHiderSmoke".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                SmokePlayer.uuid());
        String ops = SmokePlayer.opsJson();
        Assertions.assertAll(
                () -> Assertions.assertTrue(ops.contains("\"name\":\"ArmorHiderSmoke\""), ops),
                () -> Assertions.assertTrue(ops.contains("\"level\":4"), ops),
                () -> Assertions.assertTrue(ops.contains(SmokePlayer.uuidString()), ops));
    }

    /** Sanity-checks the harness itself without needing a network or a server. */
    @Test
    @DisplayName("seed document carries the distinctive non-default values")
    void seedDocumentIsDistinctive() {
        String document = PaperServerSeed.document();
        Assertions.assertAll(
                () -> Assertions.assertTrue(document.contains("\"enableCombatDetection\": false")),
                () -> Assertions.assertTrue(document.contains("\"forceArmorHiderOff\": true")),
                () -> Assertions.assertTrue(
                        document.contains("\"disableArmorHiderOnInvisibilityGlobally\": true")),
                () -> Assertions.assertTrue(
                        document.contains("\"allowIndividualPlayerConfigurations\": false")));
    }
}
