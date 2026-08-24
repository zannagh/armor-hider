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
 * - `compat=all` (default)   - fetch every configured compat mod for this MC version
 * - `compat=none`            - empty mods dir
 * - `compat=clean`           - alias of none
 * - `compat=key1,key2,...`   - fetch only the listed keys (matching the property names without
 *                              the `.version` suffix; e.g. `gender,geckolib`)
 *
 * Modrinth `dependencies[]` of type `required` are followed in two ways:
 *  - If `dependency.version_id` is pinned, that exact version is fetched.
 *  - Else if `dependency.project_id` is set, the task queries Modrinth's project-versions
 *    endpoint filtered by {@link #mcGameVersion} + {@link #loader} and picks the latest
 *    `release` (or any version if no releases match). This makes "give me a complete
 *    working modset" actually work without having to pin every transitive dep by hand.
 *
 * <h2>One jar per project</h2>
 * Dedup is by Modrinth <b>project</b>, not by version hash, in two phases: explicit pins are
 * downloaded first, then deps are followed breadth-first and skipped once their project has a jar.
 * <p>
 * Hash dedup let several versions of the SAME mod land side by side - each hash is distinct, so
 * each sailed through the seen-check. On fabric-1.20.1 `compat=all` that meant three fabric-api
 * jars at once (0.92.9 pinned, 0.84.0 via Gender's version-pinned dep, 0.92.11 via WaveyCapes'
 * project-id dep resolving to latest); five of eleven Fabric variants had a duplicate. Loader
 * remaps every copy, and the nested jars they share collide in tinyremapper's zip FileSystem
 * cache - one thread closes a FileSystem another is walking and the launch dies with
 * `Failed to remap mods! / ClosedFileSystemException`. A race, so it reds a nightly only now and
 * then. Pins-first is the other half: a dep must never displace a deliberately pinned version.
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

    /**
     * Whether to follow Modrinth {@code required} dependencies of the fetched versions. Defaults to
     * {@code true} (the compat-mod-set behaviour). Set {@code false} when fetching a resource pack
     * (e.g. Fresh Animations) into {@code run/resourcepacks/}: its required deps are the EMF/ETF
     * mod jars, which belong in {@code run/mods/} and would otherwise be dropped into the pack dir
     * where Minecraft tries to load them as packs.
     */
    @get:Input
    @get:Optional
    abstract val followDependencies: Property<Boolean>

    /** Modrinth access for this run. Rebuilt per {@link #fetch()} so its memo never leaks rows. */
    private lateinit var modrinth: ModrinthClient

    @TaskAction
    fun fetch() {
        val target = modsDir.get().asFile
        target.mkdirs()
        // Wipe any previously-downloaded compat jars so the set is hermetic per run.
        target.listFiles()?.forEach { it.delete() }
        modrinth = ModrinthClient(logger, mcGameVersion.orNull, loader.orNull)

        val keys = versionHashes.get().keys.toMutableSet()
        keys.retainAll(include.get())
        if (keys.isEmpty()) {
            logger.lifecycle("[fetchCompatJars] No compat mods selected; mods dir left empty")
            return
        }

        val seenHashes = mutableSetOf<String>()
        val claimedProjects = mutableSetOf<String>()
        val pendingDeps = ArrayDeque<PendingDep>()

        // Phase 1 - explicit pins only, deps deferred. Downloading these first is what makes the
        // pin authoritative: it claims its project before any transitive resolution gets a look in,
        // so key iteration order can no longer decide which fabric-api the run boots against.
        keys.forEach { key ->
            val hash = versionHashes.get()[key] ?: return@forEach
            try {
                fetchVersion(hash, target, seenHashes, claimedProjects, pendingDeps, key)
            } catch (e: Exception) {
                logger.warn("[fetchCompatJars] {} ({}): {}", key, hash, e.message)
            }
        }

        // Phase 2 - transitive deps, breadth-first. Each fetch may enqueue its own deps onto the
        // same queue; the project claim-check inside fetchVersion is what terminates it.
        while (pendingDeps.isNotEmpty()) {
            val pending = pendingDeps.removeFirst()
            try {
                val hash = pending.versionId ?: run {
                    val projectId = pending.projectId ?: return@run null
                    // Already satisfied - skip without querying Modrinth at all. Saves a request
                    // per occurrence on compat=all runs, which is where rate limits bite. Logged
                    // rather than dropped silently: a run that quietly omits a mod looks identical
                    // to one that never wanted it.
                    if (projectId in claimedProjects) {
                        logger.lifecycle(
                            "[fetchCompatJars] {} → skipped (project {} already provided by an earlier selection)",
                            pending.label, projectId
                        )
                        return@run null
                    }
                    modrinth.latestForProject(projectId, pending.label)
                } ?: continue
                fetchVersion(hash, target, seenHashes, claimedProjects, pendingDeps, pending.label)
            } catch (e: Exception) {
                logger.warn("[fetchCompatJars] {}: {}", pending.label, e.message)
            }
        }
    }

    /**
     * A required dep discovered while fetching, queued for phase 2. Exactly one of
     * {@link #versionId} / {@link #projectId} is set - a version-pinned dep resolves to itself, a
     * project-id dep still has to be resolved against Modrinth when its turn comes.
     */
    private data class PendingDep(
        val versionId: String?,
        val projectId: String?,
        val label: String,
    )

    /**
     * Fetch one Modrinth version, unless its project already has a jar in the dir, and queue its
     * `required` deps for phase 2 rather than recursing into them.
     */
    private fun fetchVersion(
        hash: String,
        target: File,
        seen: MutableSet<String>,
        claimedProjects: MutableSet<String>,
        pendingDeps: ArrayDeque<PendingDep>,
        label: String,
    ) {
        if (!seen.add(hash)) return
        val json = modrinth.version(hash)

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
                    val resolved = modrinth.latestForProject(projectId, "$label/auto-correct")
                    if (resolved != null && resolved != hash) {
                        // Replace the bad pin with the auto-resolved one. Deliberately NOT
                        // pre-adding `resolved` to `seen` as a recursion guard: fetchVersion opens
                        // with `if (!seen.add(hash)) return`, so pre-adding made it return on entry
                        // and the replacement was never downloaded - the stale pin was dropped with
                        // nothing put back. Let that same check do the dedup; the resolved version
                        // targets `mc` by construction, so it cannot re-enter this branch.
                        fetchVersion(resolved, target, seen, claimedProjects, pendingDeps, label)
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

        // One jar per project. A second version of an already-fetched mod is dropped here rather
        // than written alongside the first - see the class doc for what duplicate copies do to
        // fabric-loader's remapper. Phase ordering makes the survivor the explicit pin.
        val project = json.get("project_id")?.takeIf { !it.isJsonNull }?.asString
        if (project != null && project in claimedProjects) {
            logger.lifecycle(
                "[fetchCompatJars] {} → skipped (project {} already provided by an earlier selection)",
                label, project
            )
            return
        }

        val file = json.getAsJsonArray("files").get(0).asJsonObject
        val url = file.get("url").asString
        val filename = file.get("filename").asString
        val out = target.toPath().resolve(filename)
        logger.lifecycle("[fetchCompatJars] {} → {}", label, filename)
        modrinth.download(url, out)
        // Claim only once the jar is actually on disk. Claiming before the download would let a
        // failed fetch suppress every later fallback for that project - the run would end up with
        // no copy at all, which is worse than the duplicate this dedup exists to prevent.
        if (project != null) {
            claimedProjects.add(project)
        }

        // Queue required deps for phase 2 instead of recursing: depth-first recursion would let
        // one pin's dep chain claim projects before the remaining pins are even looked at.
        if (followDependencies.getOrElse(true) == false) return
        json.getAsJsonArray("dependencies")?.forEach { dep ->
            val obj = dep.asJsonObject
            val type = obj.get("dependency_type")?.asString ?: return@forEach
            if (type != "required") return@forEach
            val versionId = obj.get("version_id")?.takeIf { !it.isJsonNull }?.asString
            if (versionId != null) {
                pendingDeps.addLast(PendingDep(versionId, null, "$label/dep"))
                return@forEach
            }
            val projectId = obj.get("project_id")?.takeIf { !it.isJsonNull }?.asString ?: return@forEach
            pendingDeps.addLast(PendingDep(null, projectId, "$label/dep:$projectId"))
        }
    }

}
