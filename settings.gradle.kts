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

// NeoForge: 1.21.4+ only (1.20.x has no NeoForge; 1.21/1.21.1 Mixin too old; no 26.x releases)
val neoforgeVersions = buildVersions.filter {
    !it.startsWith("1.20") && it != "1.21" && it != "1.21.1" && !it.startsWith("26.")
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions(*buildVersions.toTypedArray())
        vcsVersion = "1.21.11" // Latest stable

        branch("common") {
            versions(*buildVersions.toTypedArray())
        }
        branch("fabric") {
            versions(*buildVersions.toTypedArray())
        }
        branch("neoforge") {
            versions(*neoforgeVersions.toTypedArray())
        }
    }
}
