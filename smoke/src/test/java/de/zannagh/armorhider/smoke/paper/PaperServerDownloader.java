package de.zannagh.armorhider.smoke.paper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Resolves and caches a PaperMC server jar via the fill.papermc.io v3 API.
 *
 * <p>Three details are load-bearing and were verified empirically:</p>
 * <ul>
 *   <li>{@code builds/latest?channel=STABLE} silently ignores the channel filter, so the full
 *       {@code /builds} list is fetched and filtered here.</li>
 *   <li>The download URL is content-addressed on a different host and must be read from
 *       {@code downloads."server:default".url} rather than constructed.</li>
 *   <li>PaperMC documents generic User-Agents as disallowed, hence the explicit one below.</li>
 * </ul>
 */
public final class PaperServerDownloader {

    /** User-Agent sent to the PaperMC API; generic agents are documented as disallowed. */
    public static final String USER_AGENT = "armor-hider-ci/1.0 (patrick@weindl.org)";

    private static final String API_BASE = "https://fill.papermc.io/v3/projects/paper/versions/";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Path cacheDirectory;

    /** Uses the default cache directory, {@code ~/.cache/armor-hider/paper}. */
    public PaperServerDownloader() {
        this(defaultCacheDirectory());
    }

    public PaperServerDownloader(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    /** {@code ~/.cache/armor-hider/paper}, overridable with {@code -Dsmoke.paper.cache=...}. */
    public static Path defaultCacheDirectory() {
        String override = System.getProperty("smoke.paper.cache");
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        return Paths.get(System.getProperty("user.home"), ".cache", "armor-hider", "paper");
    }

    /**
     * Resolves the newest STABLE build for a Minecraft version and returns its jar, downloading
     * and sha256-verifying it only when the cache does not already hold it.
     *
     * @param minecraftVersion e.g. {@code 26.2}. Note that 1.21.2 and 26.1 do not exist at all,
     *                         and 1.21.5 / 1.21.9 / 26.1.1 have no STABLE build.
     */
    public PaperBuild resolve(String minecraftVersion) throws IOException, InterruptedException {
        int javaVersion = fetchMinimumJavaVersion(minecraftVersion);
        Map<String, Object> build = newestStableBuild(minecraftVersion);
        Map<String, Object> download = Json.object(Json.path(build, "downloads", "server:default"));

        int buildNumber = (int) Math.round((Double) build.get("id"));
        String url = (String) download.get("url");
        String sha256 = (String) Json.path(download, "checksums", "sha256");

        Path jar = cacheDirectory.resolve("paper-" + minecraftVersion + "-" + buildNumber + ".jar");
        if (!isCached(jar, sha256)) {
            download(url, jar, sha256);
        }
        return new PaperBuild(minecraftVersion, buildNumber, javaVersion, jar, sha256);
    }

    /** Reads {@code version.java.version.minimum} - 17 pre-1.20.5, 21 through 1.21.11, 25 from 26.1.1. */
    private int fetchMinimumJavaVersion(String minecraftVersion) throws IOException, InterruptedException {
        Object root = Json.parse(get(API_BASE + minecraftVersion));
        Object minimum = Json.path(root, "version", "java", "version", "minimum");
        if (minimum == null) {
            throw new IOException("No java.version.minimum for Minecraft " + minecraftVersion);
        }
        return (int) Math.round((Double) minimum);
    }

    /** The builds list is newest-first; STABLE is filtered client-side because the API filter lies. */
    private Map<String, Object> newestStableBuild(String minecraftVersion)
            throws IOException, InterruptedException {
        List<Object> builds = Json.array(Json.parse(get(API_BASE + minecraftVersion + "/builds")));
        for (Object entry : builds) {
            Map<String, Object> build = Json.object(entry);
            if ("STABLE".equals(build.get("channel"))) {
                return build;
            }
        }
        throw new IOException("Minecraft " + minecraftVersion + " has no STABLE Paper build ("
                + builds.size() + " builds seen)");
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private void download(String url, Path target, String expectedSha256)
            throws IOException, InterruptedException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".part");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<Path> response =
                http.send(request, HttpResponse.BodyHandlers.ofFile(temporary));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(temporary);
            throw new IOException("HTTP " + response.statusCode() + " downloading " + url);
        }
        String actual = sha256(temporary);
        if (!actual.equalsIgnoreCase(expectedSha256)) {
            Files.deleteIfExists(temporary);
            throw new IOException("Checksum mismatch for " + url + ": expected " + expectedSha256
                    + " but got " + actual);
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean isCached(Path jar, String expectedSha256) throws IOException {
        if (!Files.isRegularFile(jar)) {
            return false;
        }
        return sha256(jar).equalsIgnoreCase(expectedSha256);
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A resolved, on-disk Paper server jar.
     *
     * @param minecraftVersion the Minecraft version, e.g. {@code 26.2}
     * @param build            the Paper build number
     * @param javaVersion      the minimum Java feature release the jar demands
     * @param jar              the cached jar
     * @param sha256           the verified content hash
     */
    public record PaperBuild(String minecraftVersion, int build, int javaVersion, Path jar,
                             String sha256) {
    }
}
