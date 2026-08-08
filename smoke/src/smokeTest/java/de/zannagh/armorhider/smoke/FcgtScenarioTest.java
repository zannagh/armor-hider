package de.zannagh.armorhider.smoke;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Surfaces each in-game FCGT (fabric-client-gametest) scenario as its OWN discoverable/runnable node
 * in the IDE. {@link SmokeMatrixTest}'s {@code ENTITY_RENDER} phase runs every FCGT entrypoint in a
 * single {@code runClientGametest} launch (fast, but one scenario's failure reds the whole row and
 * you cannot run just one from the IDE). This class instead forks one launch per scenario on the
 * canonical variant {@link SmokeMatrixTest#FCGT_PER_ID_VARIANT}, so clicking e.g.
 * {@code gender-breast-armor} in IntelliJ boots exactly that one test.
 *
 * <p>Scope: only the in-game render/logic scenarios that register UNCONDITIONALLY on that variant
 * with {@code -Psmoke -Pcompat=all}. Deliberately excluded:</p>
 * <ul>
 *   <li>{@code paper-handshake} - needs a running Paper server; covered by {@link PaperE2ESmokeTest}.</li>
 *   <li>{@code first-person} - its entrypoint only registers when {@code -Pfirstperson.version} is set
 *       (see the {@code fcgtTestCatalog} gate in {@code multiloader-loom.gradle.kts}); without that flag
 *       the build's own {@code -Psmoke.fcgt.only} validation would reject the id. Run it manually with
 *       {@code -Pfirstperson.version=<v> -Pcompat=fpm}.</li>
 * </ul>
 *
 * <p>Ids mirror the {@code fcgtTestCatalog} registry in {@code multiloader-loom.gradle.kts}; a stale id
 * fails fast because {@code runClientGametest} rejects an unknown {@code -Psmoke.fcgt.only} value.</p>
 */
// OPT-IN: this per-scenario expansion (8 client launches) is redundant with the fast batched
// ENTITY_RENDER row that SmokeMatrixTest runs for this same variant, so it stays OUT of the default
// suite to save ~7 min. Enable it for granular IDE debugging with -Dsmoke.fcgt.perId=true.
@EnabledIfSystemProperty(named = "smoke.fcgt.perId", matches = "true")
@DisplayName("FCGT scenarios (per-id, " + SmokeMatrixTest.FCGT_PER_ID_VARIANT + ")")
class FcgtScenarioTest {

    /** Hard wall-clock ceiling per scenario launch. Mirrors SmokeMatrixTest's ENTITY_RENDER ceiling. */
    private static final long SCENARIO_CEILING_MS = 8 * 60 * 1000;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "entity-render",
            "individual-config",
            "keybind",
            "combat-detection",
            "water-transparency",
            "gender-breast-armor",
            "armored-elytra-gender",
            "emf-fa"
    })
    void scenario(String fcgtId) throws Exception {
        Path repoRoot = GradleFork.repoRoot();
        String variant = SmokeMatrixTest.FCGT_PER_ID_VARIANT;

        List<String> cmd = new ArrayList<>();
        cmd.add(GradleFork.gradleScript());
        cmd.add(":fabric:" + variant + ":runClientGametest");
        cmd.add("-Psmoke");
        // compat=all so every scenario's required compat mod (FGM, EMF/ETF/FA, ...) is in run/mods.
        cmd.add("-Pcompat=all");
        cmd.add("-Psmoke.fcgt.only=" + fcgtId);
        cmd.add("-Psmoke.delay.ms=" + System.getProperty("smoke.delay.ms", "15000"));
        cmd.add("--console=plain");
        cmd.add("--no-daemon");

        // FCGT calls System.exit when the (single, filtered) gametest returns, so the real exit code
        // is meaningful - wait for the natural exit.
        GradleFork.Result r = GradleFork.runToExit(cmd, repoRoot.toFile(), SCENARIO_CEILING_MS);

        if (r.exitCode() != 0) {
            Assertions.fail(String.format(
                    "FCGT scenario '%s' on %s exited with %d.%n"
                            + "Command: %s%n"
                            + "Last 80 lines of output:%n%s",
                    fcgtId, variant, r.exitCode(), String.join(" ", cmd), r.tail(80)));
        }
    }
}
