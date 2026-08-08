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

// Build-scan publishing is OPT-IN: only when the env var ARMOR_HIDER_BUILD_SCAN_PUBLISH=true is set
// do we accept the Gradle Terms of Use and upload the scan to the public scans.gradle.com. This keeps
// a random clone building locally from ever publishing scans - it must be enabled deliberately (this
// repo's maintainers export it in their shell; CI sets it on the runner). When enabled in CI, the
// official `gradle/actions/setup-gradle` step (used via the setup-gradle-env composite action) adds
// the scan link to the GitHub Actions job summary automatically.
// Non-null literal as the receiver so a missing env var (null) can never NPE during settings eval.
val publishBuildScan = "true".equals(System.getenv("ARMOR_HIDER_BUILD_SCAN_PUBLISH"), ignoreCase = true)
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        // Agree to the ToS only when opted in; leaving it unset means the plugin never publishes.
        if (publishBuildScan) {
            termsOfUseAgree = "yes"
        }
        publishing.onlyIf { publishBuildScan }
    }
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject, file("versions.json5"))
}

// Smoke matrix tests - IDE-visible JUnit suite that forks runClient per
// (loader, version, compat-set) combo. Not part of stonecutter; lives as a sibling subproject.
include(":smoke")

// PaperMC/Bukkit server-side plugin. Reimplements the mod's server half as a
// schema-agnostic relay, so it needs no per-MC-version variants - one jar covers
// 1.20.1 through 26.x. Sibling subproject, not a stonecutter branch.
include(":paper")
