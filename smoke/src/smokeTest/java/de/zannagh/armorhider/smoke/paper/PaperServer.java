package de.zannagh.armorhider.smoke.paper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Boots a real PaperMC server with the Armor Hider plugin attached, and tears it down again.
 *
 * <p>Readiness and failure are asserted on <em>log content</em>, never on exit codes: without an
 * accepted EULA the JVM exits {@code 0}, so a zero exit proves nothing.</p>
 *
 * <p>{@link #start()} is a single boot on every version: the plugin now resolves its config to the
 * level root, and pre-creating {@code <world-dir>/world} is harmless on 26.2 (verified). Only the
 * <em>dimension</em> directory may not be pre-created - Paper 26.2 then aborts with "Unable to read
 * or access the world gen settings file for dimension minecraft:overworld" - so a test that needs a
 * file in there must call {@link #generateWorld()} first. See {@link #prepare()}/{@link #launch()}.
 * </p>
 */
public final class PaperServer implements AutoCloseable {

    /**
     * Locale-safe suffix of Paper's ready line. The preceding duration is locale-formatted
     * ({@code 0,687s} under a European locale), and {@code Done preparing level} does not exist
     * before 1.21 and fires too early - so this suffix is the only reliable anchor.
     */
    public static final String READY_MARKER = ")! For help, type \"help\"";

    private static final List<String> FAILURE_MARKERS = List.of(
            "Failed to load eula.txt",
            "FAILED TO BIND TO PORT!",
            "The server has stopped responding",
            "Encountered an unexpected exception");

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration WORLD_GEN_TIMEOUT = Duration.ofSeconds(180);

    private final PaperServerDownloader.PaperBuild build;
    private final Path pluginJar;
    private final Path serverDirectory;
    private final Path javaExecutable;
    private final int port;

    private PaperProcess server;
    private Integer exitCode;

    /**
     * @param build           the resolved server jar
     * @param pluginJar       absolute path to {@code armor-hider-paper-*.jar}, passed straight to
     *                        {@code --add-plugin} so there is no copy step and no stale-jar risk
     * @param serverDirectory an empty working directory for this server instance
     * @param javaExecutable  a {@code java} binary of at least {@link
     *                        PaperServerDownloader.PaperBuild#javaVersion()}
     */
    public PaperServer(PaperServerDownloader.PaperBuild build, Path pluginJar,
                       Path serverDirectory, Path javaExecutable) {
        this.build = build;
        this.pluginJar = pluginJar.toAbsolutePath();
        this.serverDirectory = serverDirectory.toAbsolutePath();
        this.javaExecutable = javaExecutable;
        this.port = pickFreePort();
    }

    /** Writes the server directory, seeds the level root, and starts Paper with the plugin. */
    public void start() throws IOException, InterruptedException {
        prepare();
        PaperServerSeed.write(getLevelDirectory());
        launch();
    }

    /** Writes the server directory and {@code ops.json}, but starts nothing. */
    public void prepare() throws IOException {
        writeServerDirectory();
        PaperServerSeed.writeOps(serverDirectory);
    }

    /**
     * Boots once <em>without</em> the plugin so Paper creates the per-dimension directories, then
     * stops again.
     *
     * <p>Needed only by tests that must place a file inside
     * {@code <level>/dimensions/minecraft/overworld}: creating that directory by hand makes Paper
     * 26.2 abort with {@code IllegalStateException: Unable to read or access the world gen settings
     * file for dimension minecraft:overworld}. The normal {@link #start()} path does not need
     * it.</p>
     */
    public void generateWorld() throws IOException, InterruptedException {
        Path logFile = serverDirectory.resolve("smoke-worldgen.log");
        try (PaperProcess generation = new PaperProcess(command(false), serverDirectory, logFile)) {
            generation.awaitMarker(READY_MARKER, FAILURE_MARKERS, WORLD_GEN_TIMEOUT);
        }
    }

    /** Starts the real, plugin-carrying boot. Seeding must already have happened. */
    public void launch() throws IOException {
        server = new PaperProcess(command(true), serverDirectory, getLogFile());
    }

    /**
     * Blocks until the ready line appears.
     *
     * @throws IllegalStateException if a known failure marker appears, the process dies, or the
     *                               timeout elapses
     */
    public void awaitReady(Duration timeout) throws InterruptedException {
        server.awaitMarker(READY_MARKER, FAILURE_MARKERS, timeout);
    }

    /** The port the server was told to bind, chosen by binding an ephemeral socket and closing it. */
    public int getPort() {
        return port;
    }

    /** Everything the server has written to stdout/stderr so far. */
    public String getLogText() {
        return server == null ? "" : server.getLog();
    }

    /** The {@code --world-dir} root; the level itself lives in {@link #getLevelDirectory()}. */
    public Path getWorldDir() {
        return serverDirectory.resolve("worlds");
    }

    /** {@code <world-dir>/world} - the level root the plugin's config lives under. */
    public Path getLevelDirectory() {
        return getWorldDir().resolve("world");
    }

    /** The server's working directory. */
    public Path getServerDirectory() {
        return serverDirectory;
    }

    /** The mirrored stdout capture of the real (plugin-carrying) boot. */
    public Path getLogFile() {
        return serverDirectory.resolve("smoke-stdout.log");
    }

    /**
     * {@code logs/latest.log}, or {@code ""} if it does not exist.
     *
     * <p>Use this only <em>after</em> shutdown. Paper's console appender emits nothing at all once
     * SIGTERM arrives - the whole shutdown sequence, {@code onDisable} included, reaches the file
     * appender and never the stdout pipe (verified on 26.2). Conversely the file appender is async
     * and lags during boot, so readiness is still decided on the pipe. Line formats differ between
     * the two, so only message bodies may be matched.</p>
     */
    public String getLatestLogText() {
        Path latest = serverDirectory.resolve("logs").resolve("latest.log");
        try {
            return Files.isRegularFile(latest) ? Files.readString(latest, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    /** Stdout during boot plus {@code logs/latest.log}; the only view that spans the shutdown. */
    public String getCombinedLogText() {
        return getLogText() + "\n" + getLatestLogText();
    }

    /**
     * Waits for a message body to show up in {@code logs/latest.log}, absorbing the lag of the
     * async file appender after shutdown.
     *
     * @return whether the marker appeared before the timeout
     */
    public boolean awaitLatestLogContains(String marker, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (getLatestLogText().contains(marker)) {
                return true;
            }
            Thread.sleep(200);
        }
        return getLatestLogText().contains(marker);
    }

    /**
     * The exit code of the finished server process, or {@code null} while it is still running.
     * SIGTERM leaves 143 - the value proves nothing on its own, which is why readiness and success
     * are asserted on log content instead.
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /** Stops the server with SIGTERM, which does run {@code onDisable}. Idempotent. */
    @Override
    public void close() {
        if (server == null) {
            return;
        }
        server.stop(SHUTDOWN_TIMEOUT);
        exitCode = server.getExitCode();
    }

    /** The {@code <level>/dimensions/<namespace>/<id>} world layout arrived with the 26.x line. */
    public boolean usesPerDimensionLayout() {
        return !build.minecraftVersion().startsWith("1.");
    }

    private List<String> command(boolean withPlugin) {
        List<String> command = new ArrayList<>(List.of(
                javaExecutable.toString(),
                "-Xms2G", "-Xmx2G",
                // The watchdog otherwise trips on a stalled tick under CI load and tries to exec a
                // ./start.sh that does not exist here.
                "-Ddisable.watchdog=true",
                "-Duser.language=en", "-Duser.country=US",
                "-jar", build.jar().toAbsolutePath().toString(),
                "--nogui", "--nojline",
                "--host", "127.0.0.1",
                "--port", Integer.toString(port),
                "--world-dir", getWorldDir().toString()));
        if (withPlugin) {
            command.add("--add-plugin");
            command.add(pluginJar.toString());
        }
        return command;
    }

    private void writeServerDirectory() throws IOException {
        Files.createDirectories(serverDirectory);
        Files.createDirectories(getWorldDir());
        Files.writeString(serverDirectory.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);
        Files.writeString(serverDirectory.resolve("server.properties"), serverProperties(),
                StandardCharsets.UTF_8);
    }

    /**
     * A minimal flat void world on an offline-mode server. Offline mode is required: the dev client
     * authenticates with the literal access token {@code FabricMC} and an offline UUID, which is
     * also what makes {@link SmokePlayer#uuid()} match server-side.
     */
    private String serverProperties() {
        return String.join("\n",
                "online-mode=false",
                "enforce-secure-profile=false",
                // The colon MUST be backslash-escaped in a .properties file.
                "level-type=minecraft\\:flat",
                "generator-settings={\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1}],"
                        + "\"biome\":\"minecraft:the_void\"}",
                "level-name=world",
                "spawn-protection=0",
                "view-distance=3",
                "simulation-distance=3",
                "max-players=5",
                "server-port=" + port,
                "");
    }

    /** Binding zero and closing is reliable; probing for a conflict afterwards is not. */
    private static int pickFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not reserve a port for Paper", e);
        }
    }
}
