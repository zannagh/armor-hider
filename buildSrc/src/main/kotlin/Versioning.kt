import java.util.function.Function

object Versioning {

    /**
     * Returns just the mod version (no loader prefix, no game version).
     * E.g., "0.7.10", "0.7.10-3", "0.7.10-3-preview"
     *
     * The build script assembles the full artifact version as:
     * `<modVersion>+<gameVersionRange>`
     */
    fun getModVersion(propertyProvider: Function<String, Any?>): String {
        val preRelease = propertyProvider.apply("prerelease")
        val preReleaseVersion = propertyProvider.apply("preReleaseVersion")
        val semVer = propertyProvider.apply("semVer")

        var isPreRelease = true
        val preReleaseProperty = preRelease?.toString() ?: ""
        if (preReleaseProperty.isNotEmpty() && preReleaseProperty.lowercase() == "false") {
            isPreRelease = false
        }
        val ciVersionProperty = semVer?.toString() ?: ""

        val baseVersion = if (ciVersionProperty.isNotEmpty()) {
            ciVersionProperty
        } else {
            "0.0.1"
        }

        return if (isPreRelease) {
            val preReleaseVersionStr = preReleaseVersion?.toString() ?: ""
            if (preReleaseVersionStr.isNotEmpty()) {
                "$baseVersion-preview.$preReleaseVersionStr"
            } else {
                "$baseVersion-preview"
            }
        } else {
            baseVersion
        }
    }
}
