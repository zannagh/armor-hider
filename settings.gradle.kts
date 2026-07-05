pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("com.gradle.develocity") version("4.3.2")
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject, file("versions.json5"))
}

// Smoke matrix tests — IDE-visible JUnit suite that forks runClient per
// (loader, version, compat-set) combo. Not part of stonecutter; lives as a sibling subproject.
include(":smoke")
