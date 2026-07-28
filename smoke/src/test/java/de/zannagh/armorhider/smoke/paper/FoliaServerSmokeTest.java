package de.zannagh.armorhider.smoke.paper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Boots a real Folia server with the Armor Hider plugin.
 *
 * <p><strong>Why this exists separately from {@link PaperServerSmokeTest}.</strong> The plugin is
 * advertised as Folia-compatible in two places - {@code folia-supported: true} in both descriptors,
 * and the {@code folia} loader tag on Modrinth - but Folia is the one target where the runtime
 * genuinely differs: it replaced the single main thread with regionised ones, so
 * {@code BukkitScheduler} does not work there. {@code Schedulers} handles that reflectively, and
 * until this test existed nothing ever executed that branch.</p>
 *
 * <p>The load-bearing assertion is {@link #FOLIA_READY_MARKER}: the plugin prints its own detection
 * verdict on enable, so {@code folia=true} is direct proof that
 * {@code Schedulers#classPresent(RegionizedServer)} matched. A plugin that merely <em>loads</em> on
 * Folia while reporting {@code folia=false} would take the {@code BukkitScheduler} path and throw
 * {@code UnsupportedOperationException} on the first save - which the catch-all in
 * {@code Schedulers#runAsync} would swallow into an inline run. That failure mode is invisible in a
 * boot log, hence asserting on the detection rather than on the absence of a crash.</p>
 *
 * <p><strong>What this does not cover.</strong> {@code Schedulers#runAsync} is only reached when a
 * config packet arrives, so a boot-only test never exercises Folia's {@code getAsyncScheduler}
 * reflection or the region-thread send path in {@code PacketSender}/{@code ChannelSubscriber}. For
 * that, point the full end-to-end row at Folia:
 * {@code ./gradlew :smoke:test -Dsmoke.paper.e2e -Dsmoke.paper.project=folia}.</p>
 */
@DisplayName("Folia server boot smoke")
class FoliaServerSmokeTest {

    /**
     * The plugin's own enable line, with the detection verdict inlined. Matching the {@code (} and
     * the {@code folia=true} together keeps this from passing on a truncated or reordered message.
     */
    private static final String FOLIA_READY_MARKER =
            "Armor Hider server relay enabled (folia=true";

    /** Folia builds a world per region on first boot; same budget as the Paper row. */
    private static final Duration BOOT_TIMEOUT = Duration.ofSeconds(180);

    /**
     * Minecraft version to boot Folia at. Folia ships a strict subset of Paper's versions, so this
     * is a separate property rather than reusing {@code smoke.paper.mc}: 26.2 exists (BETA-only,
     * see {@code PaperServerDownloader.CHANNEL_PREFERENCE}), 26.1.2 and 1.21.11 are STABLE, and
     * 1.21.9 / 1.21.10 / 26.1.1 do not exist at all and will skip.
     */
    private static String foliaVersion() {
        return System.getProperty("smoke.folia.mc", "26.2");
    }

    @Test
    @DisplayName("boots Folia with the plugin, detects Folia, shuts down cleanly")
    void bootsFoliaWithPlugin() throws Exception {
        String minecraftVersion = foliaVersion();
        Path pluginJar = PaperEnvironment.locatePluginJar();
        PaperServerDownloader.PaperBuild build =
                PaperEnvironment.resolveOrSkip(PaperServerDownloader.FOLIA, minecraftVersion);
        Path java = PaperEnvironment.resolveJavaOrSkip(build.javaVersion());
        Path serverDirectory = Files.createTempDirectory("armor-hider-folia-smoke");

        System.out.println("[folia-smoke] folia " + minecraftVersion + " build " + build.build()
                + " (" + build.channel() + ", java " + build.javaVersion() + ") jar=" + build.jar());
        System.out.println("[folia-smoke] plugin=" + pluginJar);
        System.out.println("[folia-smoke] serverDir=" + serverDirectory);

        long startedAt = System.nanoTime();
        try (PaperServer server = new PaperServer(build, pluginJar, serverDirectory, java)) {
            server.start();
            server.awaitReady(BOOT_TIMEOUT);
            System.out.println("[folia-smoke] ready on port " + server.getPort() + " after "
                    + (System.nanoTime() - startedAt) / 1_000_000 + " ms");

            assertFoliaDetected(server.getLogText());
            assertNoErrors(server.getLogText());
            server.close();
            System.out.println("[folia-smoke] exit code " + server.getExitCode());

            assertCleanShutdown(server);
            PaperServerSeed.assertSeedSurvived(server.getLevelDirectory());
            System.out.println("[folia-smoke] configs: "
                    + PaperServerSeed.existingConfigFiles(server.getLevelDirectory()));
        } finally {
            System.out.println("[folia-smoke] log kept at "
                    + serverDirectory.resolve("smoke-stdout.log"));
        }
    }

    /**
     * The whole point of the row. A {@code folia=false} here means the plugin loaded but believes it
     * is on Paper, which is worse than not loading at all - it fails silently at runtime.
     */
    private static void assertFoliaDetected(String log) {
        Assertions.assertTrue(log.contains(FOLIA_READY_MARKER), () -> {
            String enableLine = log.lines()
                    .filter(line -> line.contains("Armor Hider server relay enabled"))
                    .findFirst()
                    .orElse("<plugin never enabled>");
            return "Plugin did not report Folia detection. Enable line was:\n" + enableLine
                    + "\n\nIf it says folia=false, Schedulers#FOLIA_MARKER no longer matches this"
                    + " Folia build and every async save silently runs inline.\n" + log;
        });
    }

    /**
     * Any SEVERE/ERROR line or stack trace fails the run - same bar as the Paper row. Folia's boot
     * is clean enough to need no allowlist (verified on 26.2 build 1).
     */
    private static void assertNoErrors(String log) {
        List<String> offenders = log.lines()
                .filter(line -> line.contains("/ERROR]") || line.contains("SEVERE")
                        || line.contains("Exception") || line.contains("Caused by:"))
                .toList();
        Assertions.assertTrue(offenders.isEmpty(),
                () -> "Server log contains errors:\n" + String.join("\n", offenders));
    }

    /** SIGTERM must reach {@code onDisable}, which is where the plugin's final save happens. */
    private static void assertCleanShutdown(PaperServer server) throws InterruptedException {
        Assertions.assertTrue(
                server.awaitLatestLogContains("Disabling ArmorHider", Duration.ofSeconds(10)),
                () -> "Plugin was never disabled - onDisable did not run.\n"
                        + lastLines(server.getCombinedLogText(), 30));
    }

    private static String lastLines(String log, int count) {
        List<String> lines = log.lines().toList();
        return String.join("\n", lines.subList(Math.max(0, lines.size() - count), lines.size()));
    }
}
