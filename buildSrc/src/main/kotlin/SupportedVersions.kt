import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Parses `supportedVersions.json` and provides version group information.
 *
 * JSON format (per-loader sections):
 * ```json
 * {
 *   "fabric": {
 *     "1.20+": ["1.20", "1.20.1"],
 *     "1.21.4": null
 *   },
 *   "neoforge": {
 *     "1.21.4": null,
 *     "1.21.11": null
 *   }
 * }
 * ```
 *
 * @param loader If specified, only that loader's section is used.
 *               If null, all sections are merged (fabric-first, for common branch).
 */
class SupportedVersions(jsonFile: File, loader: String? = null) {

    /** Group key → list of individual versions. */
    private val groupMap: Map<String, List<String>>

    /** Individual version → group key (reverse lookup). */
    private val versionToGroup: Map<String, String>

    init {
        val type = object : TypeToken<Map<String, Map<String, List<String>?>>>() {}.type
        val full: Map<String, Map<String, List<String>?>> = Gson().fromJson(jsonFile.reader(), type)

        val section: Map<String, List<String>?> = if (loader != null && full.containsKey(loader)) {
            full[loader]!!
        } else {
            // Merge all sections (first-wins for overlapping group keys)
            val merged = linkedMapOf<String, List<String>?>()
            for (loaderSection in full.values) {
                for ((key, value) in loaderSection) {
                    merged.putIfAbsent(key, value)
                }
            }
            merged
        }

        groupMap = section.mapValues { (key, value) -> value ?: listOf(key) }
        versionToGroup = groupMap.flatMap { (key, versions) ->
            versions.map { it to key }
        }.toMap()
    }

    /** All individual versions, flattened (for Stonecutter targets). */
    val allVersions: List<String> get() = groupMap.values.flatten()

    /** All group keys (for CI matrix / Modrinth uploads). */
    val groupKeys: List<String> get() = groupMap.keys.toList()

    /** All compatible game versions for any version in the same group. */
    fun getGameVersions(version: String): List<String> {
        val groupKey = versionToGroup[version] ?: return listOf(version)
        return groupMap[groupKey] ?: listOf(version)
    }

    /**
     * Display version (= group key).
     * For "1.20" or "1.20.1" → "1.20+"
     * For "1.21.4" → "1.21.4"
     */
    fun getDisplayVersion(version: String): String =
        versionToGroup[version] ?: version

    /**
     * NeoForge version constraint for neoforge.mods.toml's `dependencies.minecraft.versionRange`.
     *
     * Uses Maven version range format (template wraps result in `[...]`):
     * - Single version: "1.21.4" → template produces "[1.21.4]"
     * - Range: "1.21.5,1.21.8" → template produces "[1.21.5,1.21.8]"
     */
    fun getNeoForgeVersionConstraint(version: String): String {
        val versions = getGameVersions(version)
        return if (versions.size <= 1) {
            versions.firstOrNull() ?: version
        } else {
            "${versions.first()},${versions.last()}"
        }
    }

    /** Primary build version for a group (first version in the list). */
    fun getPrimaryVersion(groupKey: String): String =
        groupMap[groupKey]?.first() ?: groupKey
}
