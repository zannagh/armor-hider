package de.zannagh.armorhider.smoke.paper;

import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Locates the pieces the Paper harness needs - plugin jar, server jar, a suitable JVM - and turns
 * every "not available here" case into a JUnit skip rather than a failure, so a CI runner without
 * network egress or without the right JDK stays green.
 */
public final class PaperEnvironment {

    private PaperEnvironment() {
    }

    /**
     * Resolves the newest usable Paper build for a Minecraft version, skipping the test when the
     * PaperMC API is unreachable.
     */
    public static PaperServerDownloader.PaperBuild resolveOrSkip(String minecraftVersion) {
        return resolveOrSkip(PaperServerDownloader.PAPER, minecraftVersion);
    }

    /**
     * Resolves the newest usable build of {@code project} ({@code paper} or {@code folia}).
     *
     * <p>A version the project never shipped is a <em>skip</em>, not a failure: Folia's coverage is
     * a subset of Paper's (no 1.21.9, no 1.21.10, no 26.1.1), so a row that is perfectly valid for
     * Paper can simply not exist for Folia.</p>
     */
    public static PaperServerDownloader.PaperBuild resolveOrSkip(String project,
                                                                 String minecraftVersion) {
        Assumptions.assumeTrue(hasNetwork(),
                "No network: skipping " + project + " server smoke test");
        try {
            return new PaperServerDownloader(project).resolve(minecraftVersion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (IOException e) {
            Assumptions.abort("Could not resolve " + project + " " + minecraftVersion + ": "
                    + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Finds a {@code java} binary for a Paper build that declares {@code featureRelease}.
     *
     * <p>Resolution order: the per-release launcher Gradle forwards as
     * {@code -Dsmoke.paper.java.<n>}, then an explicit {@code -Dsmoke.paper.java} <em>whose actual
     * feature release matches</em>, then the current JVM if it matches, then macOS' {@code
     * java_home}. Skips the test if none qualifies.</p>
     *
     * <p>The match is deliberately exact rather than "at least". The E2E matrix spans Paper builds
     * needing Java 21 (1.21.x) and Java 25 (26.x), and Gradle forwards a single
     * {@code smoke.paper.java} provisioned for {@code smoke.paper.mc}. Accepting any newer JDK meant
     * a 26.x-configured run silently booted Paper 1.21.4 on Java 25 - an untested combination whose
     * failure would be attributed to the plugin rather than to the launcher.</p>
     */
    public static Path resolveJavaOrSkip(int featureRelease) {
        String perRelease = System.getProperty("smoke.paper.java." + featureRelease);
        if (perRelease != null && !perRelease.isBlank() && Files.isExecutable(Paths.get(perRelease))) {
            return Paths.get(perRelease);
        }
        String configured = System.getProperty("smoke.paper.java");
        if (configured != null && !configured.isBlank() && Files.isExecutable(Paths.get(configured))
                && featureReleaseOf(Paths.get(configured)) == featureRelease) {
            return Paths.get(configured);
        }
        if (Runtime.version().feature() == featureRelease) {
            return Paths.get(System.getProperty("java.home"), "bin", "java");
        }
        Path discovered = javaHomeFor(featureRelease);
        if (discovered != null) {
            return discovered;
        }
        Assumptions.abort("No JDK " + featureRelease + " available for the Paper server"
                + " (install it, or pass -Dsmoke.paper.java." + featureRelease + "=<path>)");
        throw new IllegalStateException("unreachable");
    }

    /** Feature release a {@code java} binary reports, or {@code -1} if it cannot be determined. */
    private static int featureReleaseOf(Path java) {
        try {
            Process process = new ProcessBuilder(java.toString(), "-XshowSettings:properties",
                    "-version").redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            for (String line : output.split("\n")) {
                int marker = line.indexOf("java.specification.version =");
                if (marker >= 0) {
                    return Integer.parseInt(
                            line.substring(marker + "java.specification.version =".length()).trim());
                }
            }
            return -1;
        } catch (IOException | NumberFormatException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /** Absolute path to the built {@code armor-hider-paper-*.jar}. */
    public static Path locatePluginJar() throws IOException {
        Path libs = repositoryRoot().resolve("paper").resolve("build").resolve("libs");
        try (Stream<Path> jars = Files.list(libs)) {
            return jars.filter(path -> path.getFileName().toString().startsWith("armor-hider-paper-"))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No armor-hider-paper jar in " + libs
                            + " - run ./gradlew :paper:shadowJar"));
        } catch (IOException e) {
            throw new IOException("Cannot list " + libs + " - run ./gradlew :paper:shadowJar", e);
        }
    }

    /** Repository root, forwarded by the Gradle test task as {@code armorhider.repo.root}. */
    public static Path repositoryRoot() {
        String root = System.getProperty("armorhider.repo.root");
        if (root == null || root.isBlank()) {
            return Paths.get("").toAbsolutePath().getParent();
        }
        return Paths.get(root);
    }

    private static Path javaHomeFor(int featureRelease) {
        Path helper = Paths.get("/usr/libexec/java_home");
        if (!Files.isExecutable(helper)) {
            return null;
        }
        try {
            Process process = new ProcessBuilder(helper.toString(), "-v",
                    Integer.toString(featureRelease)).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (process.waitFor() != 0) {
                return null;
            }
            Path java = Paths.get(output).resolve("bin").resolve("java");
            return Files.isExecutable(java) ? java : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static boolean hasNetwork() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("fill.papermc.io", 443), 5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
