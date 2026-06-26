import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration

/**
 * Fetches Modrinth jars for the compat dependencies declared in `stonecutter.properties.toml`
 * (gender.version / geckolib.version / waveycapes.version / mekanism.version / figura.version /
 * elytratrims.version / iris.version) plus their required dependencies, drops the resulting jars
 * into `run/mods/`.
 *
 * Intended for use by smoke tests. Gate on `-Psmoke` in the loom config so dev runs aren't
 * affected.
 *
 * Knobs (passed as gradle project properties via -P):
 * - `compat=all` (default)   — fetch every configured compat mod for this MC version
 * - `compat=none`            — empty mods dir
 * - `compat=clean`           — alias of none
 * - `compat=key1,key2,...`   — fetch only the listed keys (matching the property names without
 *                              the `.version` suffix; e.g. `gender,geckolib`)
 *
 * Modrinth `dependencies[]` of type `required` are followed in two ways:
 *  - If `dependency.version_id` is pinned, that exact version is fetched.
 *  - Else if `dependency.project_id` is set, the task queries Modrinth's project-versions
 *    endpoint filtered by {@link #mcGameVersion} + {@link #loader} and picks the latest
 *    `release` (or any version if no releases match). This makes "give me a complete
 *    working modset" actually work without having to pin every transitive dep by hand.
 *
 * Set {@link #mcGameVersion} (e.g. "1.21.8") and {@link #loader} (e.g. "fabric") on the
 * caller side; without them, project-id resolution is skipped and the task falls back to
 * version-pinned-only behavior with a warning.
 */
abstract class FetchCompatJars : DefaultTask() {

    @get:OutputDirectory
    abstract val modsDir: DirectoryProperty

    /** Map of compat-key → Modrinth version hash. Caller fills from stonecutter properties. */
    @get:Input
    abstract val versionHashes: MapProperty<String, String>

    /** Set of compat-keys to include. Empty = include all. */
    @get:Input
    @get:Optional
    abstract val include: SetProperty<String>

    /** Current Minecraft game version (e.g. "1.21.8"). Used as filter when auto-resolving
     *  project-id-only required deps. */
    @get:Input
    @get:Optional
    abstract val mcGameVersion: Property<String>

    /** Modrinth loader filter (e.g. "fabric", "neoforge"). Pair with {@link #mcGameVersion}. */
    @get:Input
    @get:Optional
    abstract val loader: Property<String>

    private val http: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
    }

    /**
     * Memo for project_id → latest-compatible version_id resolutions per task run. Multiple
     * compat mods often declare the same required project_id dep (e.g. half the Fabric
     * ecosystem pulls fabric-language-kotlin) — without memoization we'd re-query Modrinth
     * once per occurrence, easily landing in rate-limit territory on `compat=all` runs.
     * Cleared at the start of every {@link #fetch()} invocation.
     */
    private val projectResolutionMemo = mutableMapOf<String, String?>()

    @TaskAction
    fun fetch() {
        val target = modsDir.get().asFile
        target.mkdirs()
        // Wipe any previously-downloaded compat jars so the set is hermetic per run.
        target.listFiles()?.forEach { it.delete() }
        projectResolutionMemo.clear()

        val keys = versionHashes.get().keys.toMutableSet()
        keys.retainAll(include.get())
        if (keys.isEmpty()) {
            logger.lifecycle("[fetchCompatJars] No compat mods selected; mods dir left empty")
            return
        }

        val seenHashes = mutableSetOf<String>()
        keys.forEach { key ->
            val hash = versionHashes.get()[key] ?: return@forEach
            try {
                fetchVersion(hash, target, seenHashes, key)
            } catch (e: Exception) {
                logger.warn("[fetchCompatJars] {} ({}): {}", key, hash, e.message)
            }
        }
    }

    /** Recursively fetch a Modrinth version + its `required` deps that pin a version_id. */
    private fun fetchVersion(hash: String, target: File, seen: MutableSet<String>, label: String) {
        if (!seen.add(hash)) return
        val body = http.send(
            HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/version/$hash"))
                .header("User-Agent", "armor-hider-buildscript")
                .GET().build(),
            HttpResponse.BodyHandlers.ofString()
        ).body()
        val json = JsonParser.parseString(body).asJsonObject

        // If the manually-pinned version doesn't actually target our MC, try to auto-resolve
        // a correct version from the same project. Common with stale pins after MC bumps.
        val mc = mcGameVersion.orNull
        if (!mc.isNullOrBlank()) {
            val pinnedGameVersions = json.getAsJsonArray("game_versions")
                ?.mapNotNull { it.takeIf { !it.isJsonNull }?.asString } ?: emptyList()
            if (pinnedGameVersions.isNotEmpty() && mc !in pinnedGameVersions) {
                val projectId = json.get("project_id")?.takeIf { !it.isJsonNull }?.asString
                logger.warn(
                    "[fetchCompatJars] {} pinned hash {} targets {} not MC {}; attempting auto-resolve",
                    label, hash, pinnedGameVersions, mc
                )
                if (projectId != null) {
                    val resolved = resolveLatestForProject(projectId, "$label/auto-correct")
                    if (resolved != null && resolved != hash && seen.add(resolved)) {
                        // Replace the bad pin with the auto-resolved one.
                        fetchVersion(resolved, target, seen, label)
                        return
                    }
                }
                logger.warn(
                    "[fetchCompatJars] {} could not auto-correct; skipping {} to avoid an incompatible-mod boot failure",
                    label, hash
                )
                return
            }
        }

        val file = json.getAsJsonArray("files").get(0).asJsonObject
        val url = file.get("url").asString
        val filename = file.get("filename").asString
        val out = target.toPath().resolve(filename)
        logger.lifecycle("[fetchCompatJars] {} → {}", label, filename)
        http.send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofInputStream()
        ).body().use { Files.copy(it, out, StandardCopyOption.REPLACE_EXISTING) }

        // Follow required deps: version-pinned first, project-id auto-resolution second.
        json.getAsJsonArray("dependencies")?.forEach { dep ->
            val obj = dep.asJsonObject
            val type = obj.get("dependency_type")?.asString ?: return@forEach
            if (type != "required") return@forEach
            val versionId = obj.get("version_id")?.takeIf { !it.isJsonNull }?.asString
            if (versionId != null) {
                fetchVersion(versionId, target, seen, "$label/dep")
                return@forEach
            }
            val projectId = obj.get("project_id")?.takeIf { !it.isJsonNull }?.asString ?: return@forEach
            val resolved = resolveLatestForProject(projectId, label)
            if (resolved != null) {
                fetchVersion(resolved, target, seen, "$label/dep:$projectId")
            }
        }
    }

    /**
     * Auto-resolve a project-id-only required dep: query Modrinth for the latest version of
     * {@code projectId} compatible with {@link #mcGameVersion} + {@link #loader}, return its
     * version_id. Returns {@code null} if no compatible version exists or if the filters
     * aren't set.
     */
    private fun resolveLatestForProject(projectId: String, parentLabel: String): String? {
        // Same project_id may be required by many compat mods (fabric-language-kotlin is
        // pulled by 4-5 different mods in our matrix). Memoize so we hit Modrinth at most
        // once per project per task run.
        if (projectResolutionMemo.containsKey(projectId)) {
            return projectResolutionMemo[projectId]
        }
        val mc = mcGameVersion.orNull
        val ldr = loader.orNull
        if (mc.isNullOrBlank() || ldr.isNullOrBlank()) {
            logger.warn("[fetchCompatJars] {} required project={} but mcGameVersion/loader unset — skipping auto-resolve",
                    parentLabel, projectId)
            projectResolutionMemo[projectId] = null
            return null
        }
        val gv = URLEncoder.encode("""["$mc"]""", StandardCharsets.UTF_8)
        val ld = URLEncoder.encode("""["$ldr"]""", StandardCharsets.UTF_8)
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
                    parentLabel, projectId, ldr, mc)
            projectResolutionMemo[projectId] = null
            return null
        }
        // Prefer the latest release; fall back to the latest of any type.
        val release = arr.firstOrNull { it.asJsonObject.get("version_type")?.asString == "release" }?.asJsonObject
        val pick = (release ?: arr.first().asJsonObject)
        val id = pick.get("id").asString
        projectResolutionMemo[projectId] = id
        return id
    }
}
