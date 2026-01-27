import java.util.function.Function

object Versioning {

    fun getGitVersion(propertyProvider: Function<String, Any?>, gameVersion: String): String {
        return getGitVersion(
            propertyProvider.apply("prerelease"),
            propertyProvider.apply("preReleaseVersion"),
            propertyProvider.apply("semVer"),
    gameVersion,
            propertyProvider.apply("mod_loader")
        )
    }
    
    fun getGitVersion(preRelease: Any?, preReleaseVersion: Any?, semVer: Any?, gameVersion: String, modLoader: Any? = "fabric"): String {
        var isPreRelease = true
        val preReleaseProperty = preRelease?.toString() ?: ""
        if (preReleaseProperty.isNotEmpty() && preReleaseProperty.lowercase() == "false") {
            isPreRelease = false
        }
        val ciVersionProperty = semVer?.toString() ?: ""
        val modLoader = modLoader?.toString() ?: "fabric"

        val buildVersion = if (ciVersionProperty.isNotEmpty()) {
            val ciVersion = "$gameVersion-$ciVersionProperty"
            "$modLoader-$ciVersion"
        } else {
            "$modLoader-${Versioning.getVersionFromGitVersionOrTag(gameVersion)}"
        }

        return if (isPreRelease) {
            val preReleaseVersion = preReleaseVersion?.toString() ?: ""
            if (preReleaseVersion.isNotEmpty()) {
                "$buildVersion-preview.$preReleaseVersion"
            } else {
                "$buildVersion-preview"
            }
        }
        else {
            buildVersion
        }
    }
    
    fun getVersionFromGitVersionOrTag(gameVersion: String): String {
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
                return getVersionFromGitVersionOrTagFallback(
                    semVerProc.inputStream.bufferedReader().readText().trim(),
                    commitsSinceProc.inputStream.bufferedReader().readText().trim(),
                    uncommittedProc.inputStream.bufferedReader().readText().trim(),
                    gameVersion
                )
            }
            return getVersionFromGitVersionOrTagFallback(semVer, commitsSinceSource, uncommittedChanges, gameVersion)
        } catch (_: Exception) {
            val lastKnownTag = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags"))
                .inputStream.bufferedReader().readText().trim()
            return if (lastKnownTag.contains(gameVersion)) {
                lastKnownTag
            } else {
                "$gameVersion-$lastKnownTag"
            }
        }
    }

    private fun getVersionFromGitVersionOrTagFallback(
        semVer: String,
        commitsSinceSource: String,
        uncommittedChanges: String,
        gameVersion: String
    ): String {
        if (semVer.isEmpty() || !semVer.contains(Regex("\\d")) || !commitsSinceSource.matches(Regex("\\d.*"))) {
            throw Exception("Invalid version info")
        }
        return when (commitsSinceSource) {
            "0" -> if (uncommittedChanges == "0") "$gameVersion-$semVer" else "$gameVersion-$semVer-$uncommittedChanges"
            else -> "$gameVersion-$semVer-$commitsSinceSource"
        }
    }
}
