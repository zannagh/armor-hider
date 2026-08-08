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
 *
 * <p>The same API serves {@link #FOLIA} under a different project id and an identical response
 * shape, which is why this class is parameterised rather than duplicated.</p>
 */
public final class PaperServerDownloader {

    /** User-Agent sent to the PaperMC API; generic agents are documented as disallowed. */
    public static final String USER_AGENT = "armor-hider-ci/1.0 (patrick@weindl.org)";

    /** fill.papermc.io project id for Paper. */
    public static final String PAPER = "paper";

    /** fill.papermc.io project id for Folia. */
    public static final String FOLIA = "folia";

    private static final String API_ROOT = "https://fill.papermc.io/v3/projects/";

    /**
     * Build channels, most-preferred first.
     *
     * <p>Paper publishes STABLE for every version this harness targets, but Folia does not: 26.2
     * has exactly one build and it is BETA (checked 2026-07-29), while 26.1.2 and the 1.21.x line
     * are STABLE. Insisting on STABLE would therefore skip Folia on the newest line - the one most
     * worth testing - so the fallback is deliberate. {@link PaperBuild#channel()} carries what was
     * actually picked so a test can say so out loud instead of quietly running a beta.</p>
     */
    private static final List<String> CHANNEL_PREFERENCE = List.of("STABLE", "BETA", "ALPHA");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final String project;
    private final Path cacheDirectory;

    /** Paper, in the default cache directory {@code ~/.cache/armor-hider/paper}. */
    public PaperServerDownloader() {
        this(PAPER, defaultCacheDirectory());
    }

    /** The given project, in the default cache directory. */
    public PaperServerDownloader(String project) {
        this(project, defaultCacheDirectory());
    }

    public PaperServerDownloader(String project, Path cacheDirectory) {
        this.project = project;
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
     * Resolves the newest acceptable build for a Minecraft version and returns its jar, downloading
     * and sha256-verifying it only when the cache does not already hold it.
     *
     * <p>STABLE is preferred; see {@link #CHANNEL_PREFERENCE} for why a lower channel is accepted.
     * </p>
     *
     * @param minecraftVersion e.g. {@code 26.2}. Note that for Paper 1.21.2 and 26.1 do not exist
     *                         at all, and 1.21.5 / 1.21.9 / 26.1.1 have no STABLE build.
     */
    public PaperBuild resolve(String minecraftVersion) throws IOException, InterruptedException {
        int javaVersion = fetchMinimumJavaVersion(minecraftVersion);
        Map<String, Object> build = newestAcceptableBuild(minecraftVersion);
        Map<String, Object> download = Json.object(Json.path(build, "downloads", "server:default"));

        int buildNumber = (int) Math.round((Double) build.get("id"));
        String channel = String.valueOf(build.get("channel"));
        String url = (String) download.get("url");
        String sha256 = (String) Json.path(download, "checksums", "sha256");

        Path jar = cacheDirectory.resolve(
                project + "-" + minecraftVersion + "-" + buildNumber + ".jar");
        if (!isCached(jar, sha256)) {
            download(url, jar, sha256);
        }
        return new PaperBuild(project, minecraftVersion, buildNumber, channel, javaVersion, jar,
                sha256);
    }

    /** Reads {@code version.java.version.minimum} - 17 pre-1.20.5, 21 through 1.21.11, 25 from 26.1.1. */
    private int fetchMinimumJavaVersion(String minecraftVersion) throws IOException, InterruptedException {
        Object root = Json.parse(get(versionsBase() + minecraftVersion));
        Object minimum = Json.path(root, "version", "java", "version", "minimum");
        if (minimum == null) {
            throw new IOException("No java.version.minimum for Minecraft " + minecraftVersion);
        }
        return (int) Math.round((Double) minimum);
    }

    /**
     * The builds list is newest-first; the channel is filtered client-side because the API's own
     * filter lies. Channels are tried in {@link #CHANNEL_PREFERENCE} order, so a STABLE build always
     * wins over a newer BETA one.
     */
    private Map<String, Object> newestAcceptableBuild(String minecraftVersion)
            throws IOException, InterruptedException {
        List<Object> builds =
                Json.array(Json.parse(get(versionsBase() + minecraftVersion + "/builds")));
        for (String channel : CHANNEL_PREFERENCE) {
            for (Object entry : builds) {
                Map<String, Object> build = Json.object(entry);
                if (channel.equals(build.get("channel"))) {
                    return build;
                }
            }
        }
        throw new IOException("Minecraft " + minecraftVersion + " has no usable " + project
                + " build (" + builds.size() + " builds seen, none in " + CHANNEL_PREFERENCE + ")");
    }

    private String versionsBase() {
        return API_ROOT + project + "/versions/";
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
     * A resolved, on-disk server jar.
     *
     * @param project          the fill.papermc.io project, {@link #PAPER} or {@link #FOLIA}
     * @param minecraftVersion the Minecraft version, e.g. {@code 26.2}
     * @param build            the build number
     * @param channel          the build channel actually picked, e.g. {@code STABLE}
     * @param javaVersion      the minimum Java feature release the jar demands
     * @param jar              the cached jar
     * @param sha256           the verified content hash
     */
    public record PaperBuild(String project, String minecraftVersion, int build, String channel,
                             int javaVersion, Path jar, String sha256) {

        /** Whether this is a Folia jar, i.e. whether regionised threading is in play. */
        public boolean isFolia() {
            return FOLIA.equals(project);
        }
    }
}
