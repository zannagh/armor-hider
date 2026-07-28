plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Minecraft version the Paper server smoke test boots. Must be a version PaperMC actually
// publishes a STABLE build for: 1.21.2 and 26.1 do not exist at all, and 1.21.5 / 1.21.9 /
// 26.1.1 only ever shipped ALPHA builds.
val paperMinecraftVersion = findProperty("smoke.paper.mc")?.toString()?.takeIf { it.isNotEmpty() }
    ?: "26.2"

// Paper's minimum Java: 17 for 1.20–1.20.4, 21 for 1.20.5–1.21.11, 25 from 26.1.1 onward.
// The test re-reads the authoritative value from the API; this only decides which toolchain
// Gradle provisions for the forked server JVM.
val paperJavaVersion = when {
    paperMinecraftVersion.startsWith("26.") && paperMinecraftVersion != "26.1" -> 25
    paperMinecraftVersion.startsWith("1.20.") &&
        paperMinecraftVersion.removePrefix("1.20.").toIntOrNull()?.let { it <= 4 } == true -> 17
    paperMinecraftVersion == "1.20" -> 17
    else -> 21
}

val paperServerLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(paperJavaVersion))
}

// The E2E matrix spans Paper builds with DIFFERENT Java requirements (21 for 1.21.x, 25 for 26.x),
// so a single forwarded launcher cannot serve every row: `smoke.paper.java` is provisioned for
// `smoke.paper.mc` only, and PaperEnvironment previously accepted any JDK >= the minimum, which
// would have run Paper 1.21.4 on Java 25. Offer one launcher per feature release instead and let
// the test pick the exact match. Resolution is lazy and failure-tolerant: a machine missing a JDK
// simply does not get that property, and the row skips rather than fails.
val paperFeatureReleases = listOf(17, 21, 25)
fun launcherPathOrNull(feature: Int): String? = runCatching {
    javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(feature)) }
        .get().executablePath.asFile.absolutePath
}.getOrNull()

tasks.test {
    // Each row in this matrix forks gradle + boots MC - too expensive to run as part of
    // `./gradlew build`/`check`. Skip unless invoked explicitly (`./gradlew :smoke:test`,
    // an IDE test run that targets this task, or `-Psmoke.run`).
    val explicitlyRequested = project.hasProperty("smoke.run")
            || gradle.startParameter.taskNames.any {
                it == "test" || it.endsWith(":test") || it.endsWith(":smoke:test")
            }
    onlyIf("Smoke matrix is opt-in: pass -Psmoke.run or invoke :smoke:test directly") {
        explicitlyRequested
    }
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.timeout.test.default", "10m")
    // Show test logs in IDE/CI output instead of swallowing them.
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    listOf(
        "smoke.only", "smoke.exclude", "smoke.compat", "smoke.phase", "smoke.delay.ms",
        // Opt-in switch for PaperE2ESmokeTest (boots a Paper server + forks runClientGametest).
        "smoke.paper.e2e"
    ).forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    // Repo root → tests fork ./gradlew from here.
    systemProperty("armorhider.repo.root", rootProject.projectDir.absolutePath)

    // The Paper smoke test boots `paper/build/libs/armor-hider-paper-*.jar` in place via
    // --add-plugin, so the jar has to exist and be current.
    dependsOn(":paper:shadowJar")
    systemProperty("smoke.paper.mc", paperMinecraftVersion)
    listOf(
        "smoke.paper.cache",
        // Minecraft version FoliaServerSmokeTest boots (the test defaults to 26.2). Folia ships a
        // strict subset of Paper's versions - 1.21.9 / 1.21.10 / 26.1.1 do not exist at all - and
        // the test skips rather than fails on those.
        "smoke.folia.mc",
        // Points PaperE2ESmokeTest at `folia` instead of `paper`, so the full client<->server row
        // exercises the regionised-threading paths. See PaperE2ESmokeTest#serverProject.
        "smoke.paper.project"
    ).forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    // Resolved in doFirst so a skipped/cached run never provisions a JDK it will not use.
    doFirst {
        systemProperty("smoke.paper.java",
            System.getProperty("smoke.paper.java")
                ?: paperServerLauncher.get().executablePath.asFile.absolutePath)
        // Per-feature-release launchers for the multi-version E2E matrix (see above).
        paperFeatureReleases.forEach { feature ->
            val key = "smoke.paper.java.$feature"
            (System.getProperty(key) ?: launcherPathOrNull(feature))?.let { systemProperty(key, it) }
        }
    }
}
