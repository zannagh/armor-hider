plugins {
    java
    `jvm-test-suite`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
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

// This module holds Tier-3 tests ONLY: each forks `./gradlew` and boots a real Minecraft client
// and/or a PaperMC server. They live in their OWN `smokeTest` suite (src/smokeTest/java), never the
// default `test` suite - so `./gradlew test`/`check` cannot ever pull them in and spawn a client.
// Run them explicitly with `./gradlew smokeTest` (or the root aggregate of the same name), or run an
// individual case from the IDE. See CONTRIBUTING.md > Testing for the three test tiers.
testing {
    suites {
        // Keep the default `test` suite present (IDE/tooling expect it) but source-less: a green no-op.
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("6.0.1")
        }

        val smokeTest by registering(JvmTestSuite::class) {
            useJUnitJupiter("6.0.1")
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-params")
            }
            targets.configureEach {
                testTask.configure {
                    systemProperty("junit.jupiter.execution.timeout.test.default", "10m")
                    // Opt-in parallelism: `-Dsmoke.parallel=N` runs the matrix rows across N JUnit
                    // worker threads. Each row still forks its own `./gradlew`, so the threads mostly
                    // block on their child; different variants are independent gradle subprojects and
                    // run truly concurrently, while SmokeMatrixTest's per-variant lock keeps a single
                    // variant's rows serialised. Default (unset) stays fully sequential.
                    // Only enable for a valid positive integer, so a blank/non-numeric value
                    // (e.g. `-Dsmoke.parallel=`) is ignored rather than forwarded to JUnit and
                    // failing the task at startup.
                    System.getProperty("smoke.parallel")?.trim()?.toIntOrNull()?.takeIf { it > 0 }?.let { n ->
                        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
                        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                        systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
                        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", n.toString())
                        systemProperty("junit.jupiter.execution.parallel.config.fixed.max-pool-size", n.toString())
                    }
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
            }
        }
    }
}
