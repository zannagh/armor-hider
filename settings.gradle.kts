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
    id("com.gradle.develocity") version("4.3.2")
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    
    val fabricVersions = listOf(
        "1.20.1",
        "1.21.1",
        "1.21.4", "1.21.8",
        "1.21.10", "1.21.11",
        "26.1-snapshot-11",
        "26.1-pre-2",
        "26.1-rc-1", "26.1-rc-2",
        "26.1.2"
    )
    val neoforgeVersions = listOf(
        "1.21.1", "1.21.4", "1.21.8",
        "1.21.10", "1.21.11",
        "26.1.2"
    )

    // Required to have a parseable semVer for StoneCutter (26.1-0.snapshot.2 instead of 26.1-snapshot-2)
    // since this causes problems with snapshots higher than -snapshot-10.
    // The numeric prefix (0 for snapshot, 1 for pre) ensures correct ordering:
    // semver compares numeric identifiers numerically, so 0.snapshot.x < 1.pre.x.
    fun semver(v: String) = v
        .replace(Regex("snapshot-(\\d+)"), "0.snapshot.$1")
        .replace(Regex("pre-(\\d+)"), "1.pre.$1")
        .replace(Regex("rc-(\\d+)"), "2.rc.$1")

    create(rootProject) {
        vcsVersion = "fabric-26.1.2" // Latest stable

        branch("common") {
            fabricVersions.forEach { version("fabric-$it", semver(it)) }
            neoforgeVersions.forEach { version("neoforge-$it", semver(it)) }
        }
        branch("fabric") {
            fabricVersions.forEach { version("fabric-$it", semver(it)) }
        }
        branch("neoforge") {
            neoforgeVersions.forEach { version("neoforge-$it", semver(it)) }
        }
    }
}
