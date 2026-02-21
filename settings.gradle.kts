pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

@Suppress("UNCHECKED_CAST")
val versionData = com.google.gson.Gson()
    .fromJson(file("supportedVersions.json").reader(), Map::class.java) as Map<String, Any?>
val buildVersions = versionData.flatMap { (key, value) ->
    when (value) {
        null -> listOf(key as String)
        is List<*> -> value.map { it.toString() }
        else -> listOf(key as String)
    }
}

stonecutter {
    create(rootProject) {
        versions(*buildVersions.toTypedArray())
        vcsVersion = "1.21.11" // Latest stable
        
        // Use different build files for obfuscated (1.x) vs unobfuscated (26.x) versions
        mapBuilds { _, data ->
            if (data.version.startsWith("26.")) "build-deobf.gradle.kts" else "build.gradle.kts"
        }
    }
}
