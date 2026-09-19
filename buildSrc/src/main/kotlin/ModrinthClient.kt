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
    private val metadataTimeoutSeconds: Long = METADATA_TIMEOUT_SECONDS,
) {

    companion object {
        const val DOWNLOAD_ATTEMPT_SECONDS = 30L
        const val DOWNLOAD_ATTEMPTS = 2
        const val METADATA_TIMEOUT_SECONDS = 30L
        const val METADATA_ATTEMPTS = 2
        /** Upper bound honoured for a `Retry-After` header on 429/5xx, so a hostile value cannot stall the build. */
        const val MAX_RETRY_AFTER_SECONDS = 5L
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

    /** Base URL, overridable so tests can point the client at a local server. */
    internal var apiBase: String = "https://api.modrinth.com/v2"

    /**
     * The Modrinth version object for [hash].
     *
     * @throws IOException on a non-2xx status, a timeout or a connection failure that survived the retry -
     *         never silently parses an error body as a version.
     */
    fun version(hash: String): JsonObject {
        val body = getJson("$apiBase/version/$hash", "version $hash")
        return JsonParser.parseString(body).asJsonObject
    }

    /**
     * GETs a JSON body with a [metadataTimeoutSeconds] request timeout and a status check. Retries once on
     * 429 / 5xx / timeout / connection failure (honouring `Retry-After`, capped at [MAX_RETRY_AFTER_SECONDS]);
     * every other non-2xx throws immediately. Without the status check a 429 or 5xx body used to be handed
     * to the JSON parser as if it were a version.
     */
    private fun getJson(url: String, label: String): String {
        var last: IOException? = null
        for (attempt in 1..METADATA_ATTEMPTS) {
            val request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(metadataTimeoutSeconds))
                .GET().build()
            val response = try {
                http.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: IOException) {
                last = IOException("${e.javaClass.simpleName}: ${e.message} requesting $url ($label)", e)
                logger.warn("[fetchCompatJars] {} attempt {}/{}: {}", label, attempt, METADATA_ATTEMPTS, last.message)
                continue
            }
            val status = response.statusCode()
            if (status in 200..299) {
                return response.body()
            }
            last = IOException("HTTP $status from $url ($label)")
            val retryable = status == 429 || status >= 500
            if (!retryable) {
                throw last
            }
            logger.warn("[fetchCompatJars] {} attempt {}/{}: {}", label, attempt, METADATA_ATTEMPTS, last.message)
            if (attempt < METADATA_ATTEMPTS) {
                val retryAfter = response.headers().firstValue("Retry-After")
                    .map { it.trim().toLongOrNull() ?: 0L }.orElse(1L)
                    .coerceIn(0L, MAX_RETRY_AFTER_SECONDS)
                Thread.sleep(retryAfter * 1000L)
            }
        }
        throw last ?: IOException("request failed: $url ($label)")
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
            } catch (e: InterruptedException) {
                // Build cancelled while waiting: stop the transfer and let the interruption propagate.
                pending.cancel(true)
                Thread.currentThread().interrupt()
                throw e
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
            // No-op after a successful move; removes the partial file on every failure path. A failure to
            // delete (e.g. the cancelled transfer still holding the handle) must not mask the real error.
            try {
                Files.deleteIfExists(tmp)
            } catch (e: IOException) {
                logger.warn("[fetchCompatJars] could not remove partial download {}: {}", tmp, e.message)
            }
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
        val url = "$apiBase/project/$projectId/version?game_versions=$gv&loaders=$ld"
        // Auto-resolve is a best-effort fallback: a failed lookup is logged and reported as "no version"
        // (the caller then skips the mod), unlike a failed lookup of an explicit pin, which fails the task.
        val body = try {
            getJson(url, "$parentLabel auto-resolve project=$projectId")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
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
