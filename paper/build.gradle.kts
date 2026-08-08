plugins {
    java
    jacoco
    id("com.gradleup.shadow") version "9.2.2"
}

// The Paper plugin talks nothing but the wire protocol, so a single jar covers every
// supported game version. Versioning still has to mirror multiloader-common.gradle.kts,
// because the publish pipeline greps for `armor-hider-<loader>-<semVer>+<display_version>.jar`.
val semVer = findProperty("semVer")?.toString()?.takeIf { it.isNotEmpty() } ?: "0.0.1-preview.0"
val displayVersion = "paper"

// Derived from stonecutter.properties.toml so the published game-version list cannot drift
// away from what the loaders actually ship.
//
// Snapshots, pre-releases and release candidates are dropped: PaperMC only ever publishes
// builds for stable Minecraft releases, so advertising e.g. "26.1-snapshot-7" would offer
// server admins a plugin for a platform that does not exist. The mod's own Fabric/NeoForge
// artifacts legitimately keep those versions - this filter is Paper-specific.
val unstableVersionMarkers = listOf("-snapshot-", "-pre-", "-rc-")

// Minecraft versions PaperMC never released at all - both 404 on
// https://fill.papermc.io/v3/projects/paper/versions/<v> (checked 2026-07-28).
// Paper skipped 1.21.2 outright (its version group jumps 1.21.1 -> 1.21.3), and 26.1
// only ever shipped as the point releases 26.1.1 / 26.1.2. Advertising them would offer
// admins a plugin for a platform that does not exist.
val versionsWithoutPaperBuilds = setOf("1.21.2", "26.1")

val supportedGameVersions: String = rootProject.file("stonecutter.properties.toml").readLines()
    .filter { it.trimStart().startsWith("game_versions") }
    .flatMap { it.substringAfter('=').trim().trim('"').split(",") }
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .filter { version -> unstableVersionMarkers.none { version.contains(it) } }
    .filterNot { it in versionsWithoutPaperBuilds }
    .distinct()
    .joinToString(",")

group = "de.zannagh.armorhider"
version = "$semVer+$displayVersion"

// Read back by the root `stageArtifacts` task via proj.findProperty(...).
extra["display_version"] = displayVersion
extra["game_versions"] = supportedGameVersions

base {
    archivesName.set("armor-hider-paper")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Deliberately an OLD API level: everything used here has been stable Bukkit API since 1.13,
    // and compiling low keeps the single jar loadable on 1.20.1 through 26.x.
    // 1.20.4 rather than 1.20.6 because Paper moved to a Java 21 baseline at 1.20.5, and Gradle
    // then refuses the dependency against `options.release = 17` - which we need, since a 1.20.1
    // server still runs on Java 17. paperweight-userdev is explicitly not used: nothing touches NMS.
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Plain unit tests over the pure-logic classes (channel selection, config shape). Cheap and
// hermetic - no Bukkit, no server - so unlike :smoke these DO run as part of `check`.
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand("version" to project.version.toString())
    }
}

// The shaded jar is THE jar; the thin one would only confuse the staging/publish glob.
tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    // Third-party only. Never relocate our own packages or org.bukkit.
    relocate("com.google.gson", "de.zannagh.armorhider.paper.libs.gson")
    from(rootProject.file("LICENSE")) {
        rename { "LICENSE_armor-hider-paper" }
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
