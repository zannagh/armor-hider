package de.zannagh.armorhider.smoke;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the {@code emf-fa} FCGT (fabric-client-gametest) scenario against BOTH pinned EMF versions on
 * {@link SmokeMatrixTest#FCGT_PER_ID_VARIANT}, one gametest JVM at a time. Each parameter forks a
 * single {@code runClientGametest} launch with {@code -Psmoke.emf.version=<id>}, which swaps the EMF
 * jar the fetch drops into {@code run/mods} WITHOUT changing the pinned {@code emf.version} in
 * {@code stonecutter.properties.toml}. {@code run/mods} is wiped before every launch (the fetch task
 * is never up-to-date), so the two sequential launches stay hermetic in one run dir.
 *
 * <p>Purpose: catch an EMF-version regression in the armor compat path (issue #217/EMF custom-model
 * rendering) before a new EMF release is promoted to the default pin. The {@code emf-fa} scenario
 * self-skips its strict EMF-custom-model assertions under software GL (a capability probe), so on a
 * headless runner this is boot/no-crash coverage across both EMF jars; the strict path assertions
 * only bite on a real GPU.</p>
 */
// OPT-IN: two extra full client launches (~one gametest JVM each) that only matter around an EMF
// version bump, so this stays OUT of the default suite. Enable it with -Dsmoke.emf.matrix=true
// (the nightly full-matrix CI job sets it; the PR gate does not).
@EnabledIfSystemProperty(named = "smoke.emf.matrix", matches = "true")
// Both parameterized launches fork runClientGametest against the SAME variant run dir and each wipes
// run/mods on start; under -Dsmoke.parallel they would race. This lock serializes them (and composes
// with any other test that takes the same run-dir key). Cross-class serialization against
// SmokeMatrixTest's fabric-26.2 rows is separate - that class guards its own rows with an in-process
// lock - but the CI nightly step runs this class in its own filtered, non-parallel gradle invocation.
@ResourceLock("fcgt-run-" + SmokeMatrixTest.FCGT_PER_ID_VARIANT)
@DisplayName("EMF version matrix (emf-fa, " + SmokeMatrixTest.FCGT_PER_ID_VARIANT + ")")
class EmfVersionMatrixSmokeTest {

    /** Hard wall-clock ceiling per launch. Mirrors FcgtScenarioTest's per-scenario ceiling. */
    private static final long SCENARIO_CEILING_MS = 8 * 60 * 1000;

    @ParameterizedTest(name = "emf={0} etf={1}")
    @CsvSource({
            // Modrinth version ids for fabric-26.2: emfVersionId, etfVersionId. EMF and ETF are pinned
            // together because EMF 3.3 hard-requires ETF 7.2+ (Fabric refuses to launch otherwise).
            "xQeW3qQB, HLCBKYFD", // EMF 3.2.6 + ETF 7.1.1 - the current default pins
            "GYu73iPJ, URB7DuXS"  // EMF 3.3   + ETF 7.2   - the newer release the matrix guards against
    })
    void emfVersion(String emfVersionId, String etfVersionId) throws Exception {
        Path repoRoot = GradleFork.repoRoot();
        String variant = SmokeMatrixTest.FCGT_PER_ID_VARIANT;

        List<String> cmd = new ArrayList<>();
        cmd.add(GradleFork.gradleScript());
        cmd.add(":fabric:" + variant + ":runClientGametest");
        cmd.add("-Psmoke");
        // The emf-fa scenario needs EMF + ETF (to load the model) and the FA resource pack (to animate).
        cmd.add("-Pcompat=emf,etf,fa");
        cmd.add("-Psmoke.fcgt.only=emf-fa");
        // Override the EMF + ETF jars for THIS launch only; run/mods is wiped each launch, so it is
        // hermetic. ETF moves with EMF so EMF 3.3's ETF 7.2+ requirement is satisfied.
        cmd.add("-Psmoke.emf.version=" + emfVersionId);
        cmd.add("-Psmoke.etf.version=" + etfVersionId);
        cmd.add("--console=plain");
        cmd.add("--no-daemon");
        cmd.add("--build-cache");

        // FCGT calls System.exit when the (single, filtered) gametest returns, so the real exit code
        // is meaningful - wait for the natural exit.
        GradleFork.Result r = GradleFork.runToExit(cmd, repoRoot.toFile(), SCENARIO_CEILING_MS);

        if (r.exitCode() != 0) {
            Assertions.fail(String.format(
                    "EMF matrix launch (emf=%s etf=%s) for scenario 'emf-fa' on %s exited with %d.%n"
                            + "Command: %s%n"
                            + "Last 80 lines of output:%n%s",
                    emfVersionId, etfVersionId, variant, r.exitCode(), String.join(" ", cmd), r.tail(80)));
        }
    }
}
