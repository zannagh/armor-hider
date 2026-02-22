pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Per-loader version groups from supportedVersions.json
@Suppress("UNCHECKED_CAST")
val versionData = com.google.gson.Gson()
    .fromJson(file("supportedVersions.json").reader(), Map::class.java) as Map<String, Map<String, Any?>>

fun extractVersions(data: Map<String, Any?>): List<String> = data.flatMap { (key, value) ->
    when (value) {
        null -> listOf(key)
        is List<*> -> value.map { it.toString() }
        else -> listOf(key)
    }
}

val fabricVersions = extractVersions(versionData["fabric"]!!)
val neoforgeVersions = extractVersions(versionData["neoforge"]!!)
val allVersions = (fabricVersions + neoforgeVersions).distinct()

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions(*allVersions.toTypedArray())
        vcsVersion = "1.21.11" // Latest stable

        branch("common") {
            versions(*allVersions.toTypedArray())
        }
        branch("fabric") {
            versions(*fabricVersions.toTypedArray())
        }
        branch("neoforge") {
            versions(*neoforgeVersions.toTypedArray())
        }
    }
}
