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
        else -> listOf()
    }
}

val fabricVersions = extractVersions(versionData["fabric"]!!)
val neoforgeVersions = extractVersions(versionData["neoforge"]!!)

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        vcsVersion = "fabric-1.21.11" // Latest stable

        branch("common") {
            fabricVersions.forEach { version("fabric-$it", it) }
            neoforgeVersions.forEach { version("neoforge-$it", it) }
        }
        branch("fabric") {
            fabricVersions.forEach { version("fabric-$it", it) }
        }
        branch("neoforge") {
            neoforgeVersions.forEach { version("neoforge-$it", it) }
        }
    }
}
