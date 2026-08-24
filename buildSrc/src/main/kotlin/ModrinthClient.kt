import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.logging.Logger
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

/**
 * Modrinth API access for [FetchCompatJars]: version lookup, latest-version resolution and jar
 * download. Split out of the task so the task file stays about *policy* (which jar wins, in what
 * order) rather than about HTTP.
 *
 * One instance per task run - [projectResolutionMemo] is scoped to that run, so a matrix row can
 * never inherit another row's resolution.
 */
internal class ModrinthClient(
    private val logger: Logger,
    private val mcGameVersion: String?,
    private val loader: String?,
) {

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
                .header("User-Agent", "armor-hider-buildscript")
                .GET().build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()
        return JsonParser.parseString(body).asJsonObject
    }

    /** Streams [url] to [out], replacing whatever was there. */
    fun download(url: String, out: Path) {
        http.send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofInputStream()
        ).body().use { Files.copy(it, out, StandardCopyOption.REPLACE_EXISTING) }
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
                    .header("User-Agent", "armor-hider-buildscript")
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
