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

stonecutter {
    create(rootProject) {
        versions(
            "1.20",
            "1.20.1",
            "1.21",
            "1.21.1",
            "1.21.9",
            "1.21.10", 
            "1.21.11", 
            "26.1-snapshot-4",
            "26.1-snapshot-5")
        vcsVersion = "1.21.11" // Latest stable
        
        // Use different build files for obfuscated (1.x) vs unobfuscated (26.x) versions
        mapBuilds { _, data ->
            if (data.version.startsWith("26.")) "build-deobf.gradle.kts" else "build.gradle.kts"
        }
    }
}
