pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.quiltmc.org/repository/release/")
        maven("https://repo.papermc.io/repository/maven-public/")
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
        "1.20", "1.20.1",
        "1.21", "1.21.1",
        "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8",
        "1.21.9", "1.21.10", "1.21.11",
        "26.1-snapshot-1", "26.1-snapshot-2", "26.1-snapshot-3",
        "26.1-snapshot-4", "26.1-snapshot-5", "26.1-snapshot-6",
        "26.1-snapshot-7", "26.1-snapshot-8", "26.1-snapshot-9",
    )
    val neoforgeVersions = listOf(
        "1.21", "1.21.1", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8",
        "1.21.9", "1.21.10", "1.21.11",
    )
    val quiltVersions = fabricVersions

    create(rootProject) {
        vcsVersion = "fabric-1.21.11" // Latest stable

        branch("common") {
            fabricVersions.forEach { version("fabric-$it", it) }
            neoforgeVersions.forEach { version("neoforge-$it", it) }
            quiltVersions.forEach { version("quilt-$it", it) }
        }
        branch("fabric") {
            fabricVersions.forEach { version("fabric-$it", it) }
        }
        branch("neoforge") {
            neoforgeVersions.forEach { version("neoforge-$it", it) }
        }
        branch("quilt") {
            quiltVersions.forEach { version("quilt-$it", it) }
        }
    }
}
