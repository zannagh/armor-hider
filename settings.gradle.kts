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
        versions("1.21.9", "1.21.10", "1.21.11")
        vcsVersion = "1.21.11"
    }
}
