import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.logging.Logger
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Modrinth API access for [FetchCompatJars]: version lookup, latest-version resolution and jar
 * download. Split out of the task so the task file stays about *policy* (which jar wins, in what
 * order) rather than about HTTP.
 *
 * One instance per task run - [projectResolutionMemo] is scoped to that run, so a matrix row can
 * never inherit another row's resolution.
 *
 * @param downloadAttemptSeconds wall-clock budget for ONE download attempt, headers and body together.
 *        Two attempts are made, so the worst case per jar is twice this - keep it well under the smoke
 *        matrix's 90 s per-boot ceiling, which wraps the whole `runClient` including this fetch.
 */
internal class ModrinthClient(
    private val logger: Logger,
    private val mcGameVersion: String?,
    private val loader: String?,
    private val downloadAttemptSeconds: Long = DOWNLOAD_ATTEMPT_SECONDS,
) {

    companion object {
        const val DOWNLOAD_ATTEMPT_SECONDS = 30L
        const val DOWNLOAD_ATTEMPTS = 2
        private const val USER_AGENT = "armor-hider-buildscript"
    }

    private val http: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
    }

    /**
     * Memo for project_id → latest-compatible version_id. Multiple compat mods often declare the
     * same required project_id (half the Fabric ecosystem pulls fabric-language-kotlin); without
     * this we would re-query once per occurrence and land in rate-limit territory on `compat=all`.
     */
    private val projectResolutionMemo = mutableMapOf<String, String?>()

    /** The Modrinth version object for [hash]. */
    fun version(hash: String): JsonObject {
        val body = http.send(
            HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/version/$hash"))
                .header("User-Agent", USER_AGENT)
                .GET().build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()
        return JsonParser.parseString(body).asJsonObject
    }

    /**
     * Downloads [url] to [out], replacing whatever was there - but only ever with a complete file.
     *
     * Each attempt streams into a `.part` sibling and is bounded as a WHOLE by [downloadAttemptSeconds]
     * (`HttpRequest.timeout` alone only covers the response headers, so a CDN body stall would still
     * hang the smoke launch to its ceiling). The sibling is atomically moved over [out] only after a
     * 2xx status and a body whose length matches `Content-Length` (when the server sent one); on any
     * failure the sibling is deleted so a truncated jar can never be left in `run/mods/` for the
     * launch to load. [DOWNLOAD_ATTEMPTS] attempts, then the last failure is rethrown; the caller
     * ([FetchCompatJars]) logs it and continues with the remaining pins, so a failed download means a
     * missing jar, never a corrupt one.
     */
    fun download(url: String, out: Path) {
        var last: IOException? = null
        for (attempt in 1..DOWNLOAD_ATTEMPTS) {
            try {
                downloadOnce(url, out)
                return
            } catch (e: IOException) {
                last = e
                logger.warn("[fetchCompatJars] download attempt {}/{} failed for {}: {}",
                        attempt, DOWNLOAD_ATTEMPTS, url, e.message)
            }
        }
        throw IOException("download failed after $DOWNLOAD_ATTEMPTS attempts: $url", last)
    }

    private fun downloadOnce(url: String, out: Path) {
        val tmp = out.resolveSibling("${out.fileName}.part")
        Files.deleteIfExists(tmp)
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(downloadAttemptSeconds))
            .GET().build()
        val pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofFile(tmp))
        try {
            val response = try {
                pending.get(downloadAttemptSeconds, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                pending.cancel(true)
                throw IOException("timed out after ${downloadAttemptSeconds}s downloading $url", e)
            } catch (e: ExecutionException) {
                val cause = e.cause ?: e
                throw IOException("${cause.javaClass.simpleName}: ${cause.message} downloading $url", cause)
            }
            if (response.statusCode() !in 200..299) {
                throw IOException("HTTP ${response.statusCode()} downloading $url")
            }
            val expected = response.headers().firstValueAsLong("Content-Length")
            val actual = Files.size(tmp)
            if (expected.isPresent && expected.asLong != actual) {
                throw IOException("truncated body ($actual of ${expected.asLong} bytes) downloading $url")
            }
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            // No-op after a successful move; removes the partial file on every failure path.
            Files.deleteIfExists(tmp)
        }
    }

    /**
     * Latest version of [projectId] compatible with [mcGameVersion] + [loader], preferring a
     * `release` and falling back to the latest of any type. Returns `null` when no compatible
     * version exists or the filters are unset.
     */
    fun latestForProject(projectId: String, parentLabel: String): String? {
        if (projectResolutionMemo.containsKey(projectId)) {
            return projectResolutionMemo[projectId]
        }
        if (mcGameVersion.isNullOrBlank() || loader.isNullOrBlank()) {
            logger.warn("[fetchCompatJars] {} required project={} but mcGameVersion/loader unset - skipping auto-resolve",
                    parentLabel, projectId)
            projectResolutionMemo[projectId] = null
            return null
        }
        val gv = URLEncoder.encode("""["$mcGameVersion"]""", StandardCharsets.UTF_8)
        val ld = URLEncoder.encode("""["$loader"]""", StandardCharsets.UTF_8)
        val url = "https://api.modrinth.com/v2/project/$projectId/version?game_versions=$gv&loaders=$ld"
        val body = try {
            http.send(
                HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(30))
                    .GET().build(),
                HttpResponse.BodyHandlers.ofString()
            ).body()
        } catch (e: Exception) {
            logger.warn("[fetchCompatJars] {} auto-resolve project={} failed: {}", parentLabel, projectId, e.message)
            projectResolutionMemo[projectId] = null
            return null
        }
        val arr = JsonParser.parseString(body).asJsonArray
        if (arr.size() == 0) {
            logger.warn("[fetchCompatJars] {} required project={} but no {} version exists for MC {}",
                    parentLabel, projectId, loader, mcGameVersion)
            projectResolutionMemo[projectId] = null
            return null
        }
        val release = arr.firstOrNull { it.asJsonObject.get("version_type")?.asString == "release" }?.asJsonObject
        val id = (release ?: arr.first().asJsonObject).get("id").asString
        projectResolutionMemo[projectId] = id
        return id
    }
}
