package de.zannagh.armorhider.smoke.fallback;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * The relay the fallback E2E runs against, behind one interface so the same assertions cover both backends:
 * the in-JVM {@link StubEunomiaRelay} (default - no external process, so CI and a laptop with nothing installed
 * still run it) and the real C# relay ({@code Eunomia.Server.Web}) forked via {@code dotnet run} when
 * {@code -Darmorhider.relay.dotnet=<path-to-eunomia/csharp>} is set and {@code dotnet} is on the PATH.
 *
 * <p>A green stub run predicts a green real-server run because the stub reproduces the C# wire contract exactly;
 * pointing the property at the checkout is how you confirm that against the actual server once it is running.</p>
 */
final class RelayUnderTest implements AutoCloseable {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final String base;
    private final StubEunomiaRelay stub;
    private final Process process;

    private RelayUnderTest(String base, StubEunomiaRelay stub, Process process) {
        this.base = base;
        this.stub = stub;
        this.process = process;
    }

    String base() {
        return base;
    }

    /** The configured real relay checkout, or {@code null} to use the in-JVM stub. */
    static String dotnetProject() {
        String value = System.getProperty("armorhider.relay.dotnet");
        return value == null || value.isBlank() ? null : value;
    }

    /** Starts the real C# relay if configured, otherwise the enabled in-JVM stub. */
    static RelayUnderTest start() throws Exception {
        String project = dotnetProject();
        return project == null ? startStub(true) : startDotnet(project);
    }

    /** Always the in-JVM stub, with the server-side fallback flag set as given (for the two-sided-gate test). */
    static RelayUnderTest startStub(boolean fallbackEnabled) throws IOException {
        StubEunomiaRelay relay = new StubEunomiaRelay(freePort(), fallbackEnabled);
        relay.start(2000, false);
        return new RelayUnderTest(relay.base(), relay, null);
    }

    private static RelayUnderTest startDotnet(String project) throws Exception {
        int port = freePort();
        String base = "http://127.0.0.1:" + port;
        Path workDir = Files.createTempDirectory("armorhider-fallback-e2e");
        ProcessBuilder builder = new ProcessBuilder(
                "dotnet", "run", "--project", project + "/src/Eunomia.Server.Web", "--urls", base)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(workDir.resolve("server.log").toFile());
        builder.environment().put("EUNOMIA_DISABLE_MOJANG_GATE", "1");
        builder.environment().put("EUNOMIA_DATA_DIR", workDir.resolve("data").toString());
        Process process = builder.start();
        awaitHealth(base, process, workDir);
        return new RelayUnderTest(base, null, process);
    }

    private static void awaitHealth(String base, Process process, Path workDir) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("relay exited early; log:\n" + tail(workDir));
            }
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/health"))
                        .timeout(Duration.ofSeconds(2)).GET().build();
                if (HTTP.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                // not up yet
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("relay /health never came up; log:\n" + tail(workDir));
    }

    private static String tail(Path workDir) {
        try {
            return Files.readString(workDir.resolve("server.log"));
        } catch (IOException e) {
            return "(no log)";
        }
    }

    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Override
    public void close() throws Exception {
        if (stub != null) {
            stub.stop();
        }
        if (process != null) {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }
}
