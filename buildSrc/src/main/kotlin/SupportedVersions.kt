import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Parses `supportedVersions.json` and provides version group information.
 *
 * JSON format:
 * ```json
 * {
 *   "1.20-1.20.1": ["1.20", "1.20.1"],
 *   "1.21.4": null
 * }
 * ```
 * Keys = group labels (descriptive range or single version).
 * Null = single version (key IS the version).
 * Array = grouped compatible versions (all become Stonecutter targets).
 */
class SupportedVersions(jsonFile: File) {

    /** Group key → list of individual versions. */
    private val groupMap: Map<String, List<String>>

    /** Individual version → group key (reverse lookup). */
    private val versionToGroup: Map<String, String>

    init {
        val type = object : TypeToken<Map<String, List<String>?>>() {}.type
        val raw: Map<String, List<String>?> = Gson().fromJson(jsonFile.reader(), type)

        groupMap = raw.mapValues { (key, value) -> value ?: listOf(key) }
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
     * For "1.20" or "1.20.1" → "1.20-1.20.1"
     * For "1.21.4" → "1.21.4"
     */
    fun getDisplayVersion(version: String): String =
        versionToGroup[version] ?: version

    /**
     * Fabric version constraint for fabric.mod.json's `depends.minecraft`.
     *
     * Single version: exact string (e.g., "1.21.4")
     * Grouped versions: ">=[first] <=[last]" (e.g., ">=1.20 <=1.20.1")
     *
     * @param transform optional version name transform (e.g., snapshot → alpha for deobf builds)
     */
    fun getFabricVersionConstraint(
        version: String,
        transform: (String) -> String = { it }
    ): String {
        val versions = getGameVersions(version).map(transform)
        return if (versions.size == 1) {
            versions.first()
        } else {
            ">=${versions.first()} <=${versions.last()}"
        }
    }

    /**
     * NeoForge version constraint for neoforge.mods.toml's `dependencies.minecraft.versionRange`.
     *
     * Uses Maven version range format (template wraps result in `[...]`):
     * - Single version: "1.21.4" → template produces "[1.21.4]"
     * - Range: "1.21.5,1.21.8" → template produces "[1.21.5,1.21.8]"
     *
     * Excludes 26.x versions since NeoForge has no 26.x releases.
     */
    fun getNeoForgeVersionConstraint(version: String): String {
        val versions = getGameVersions(version).filter { !it.startsWith("26.") }
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
