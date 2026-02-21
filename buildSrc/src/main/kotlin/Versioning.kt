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
            getModVersionFromGit()
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

    private fun getModVersionFromGit(): String {
        try {
            val semVer: String
            val commitsSinceSource: String
            val uncommittedChanges: String
            try {
                semVer = Runtime.getRuntime().exec(arrayOf("gitversion", "/output", "json", "/showVariable", "majorMinorPatch"))
                    .inputStream.bufferedReader().readText().trim()
                commitsSinceSource = Runtime.getRuntime().exec(arrayOf("gitversion", "/output", "json", "/showVariable", "CommitsSinceVersionSource"))
                    .inputStream.bufferedReader().readText().trim()
                uncommittedChanges = Runtime.getRuntime().exec(arrayOf("gitversion", "/output", "json", "/showVariable", "UncommittedChanges"))
                    .inputStream.bufferedReader().readText().trim()
            } catch (_: Exception) {
                val semVerProc = Runtime.getRuntime().exec(arrayOf("dotnet", "gitversion", "/output", "json", "/showVariable", "majorMinorPatch"))
                val commitsSinceProc = Runtime.getRuntime().exec(arrayOf("dotnet", "gitversion", "/output", "json", "/showVariable", "CommitsSinceVersionSource"))
                val uncommittedProc = Runtime.getRuntime().exec(arrayOf("dotnet", "gitversion", "/output", "json", "/showVariable", "UncommittedChanges"))
                return modVersionFromGitVersionFallback(
                    semVerProc.inputStream.bufferedReader().readText().trim(),
                    commitsSinceProc.inputStream.bufferedReader().readText().trim(),
                    uncommittedProc.inputStream.bufferedReader().readText().trim()
                )
            }
            return modVersionFromGitVersionFallback(semVer, commitsSinceSource, uncommittedChanges)
        } catch (_: Exception) {
            return Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags"))
                .inputStream.bufferedReader().readText().trim()
        }
    }

    private fun modVersionFromGitVersionFallback(
        semVer: String,
        commitsSinceSource: String,
        uncommittedChanges: String
    ): String {
        if (semVer.isEmpty() || !semVer.contains(Regex("\\d")) || !commitsSinceSource.matches(Regex("\\d.*"))) {
            throw Exception("Invalid version info")
        }
        return when (commitsSinceSource) {
            "0" -> if (uncommittedChanges == "0") semVer else "$semVer-$uncommittedChanges"
            else -> "$semVer-$commitsSinceSource"
        }
    }
}
