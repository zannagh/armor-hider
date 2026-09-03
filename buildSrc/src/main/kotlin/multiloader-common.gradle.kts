plugins {
    id("java")
    id("java-library")
    id("jacoco")
    id("maven-publish")
}

repositories {
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
    // Cursemaven (https://cursemaven.com) - keyless CurseForge artifact proxy. Resolves CF-hosted mod
    // jars by `curse.maven:<slug>-<projectId>:<fileId>` (the slug is cosmetic; only the numeric ids
    // matter). Used for mods not (yet) on Modrinth - e.g. the eunomia mod jar while it awaits Modrinth
    // approval. Group-scoped so it never intercepts a Modrinth/Mojang/GitHub artifact.
    maven("https://cursemaven.com") {
        content { includeGroup("curse.maven") }
    }
    // eunomia (de.zannagh.eunomia:eunomia-core) - the MC-free, game-version-agnostic API surface
    // armor-hider compiles against; the eunomia mod supplies the live transport at runtime. Consumed
    // compileOnly (see multiloader-loom / multiloader-loader), so this is a compile-time-only source.
    // GitHub Packages is the portable remote: the repo is public, so ANY valid GitHub token can read it
    // (not just repo collaborators), but GitHub still requires *a* token for maven artifacts - there is
    // no anonymous access - so the repo is added only when credentials are present. mavenLocal is the
    // credential-free local fallback (publish with `./gradlew :core:publishToMavenLocal` in the eunomia
    // repo), so a build without a read:packages token still resolves. Property/env names mirror eunomia's
    // own publish convention, so an existing credential setup carries straight over. For fully tokenless
    // resolution eunomia would need Maven Central.
    mavenLocal {
        content { includeGroup("de.zannagh.eunomia") }
    }
    // The TOKEN is the only thing GitHub actually validates on a Packages read - the Basic-auth username
    // field is ignored, so it is not worth gating on. Requiring a username here used to make a
    // token-only environment (CI, or a shell with GITHUB_TOKEN but no GITHUB_ACTOR) skip this repo
    // *silently*, and the resulting "Could not find de.zannagh.eunomia:eunomia-core:<v>" listed every
    // searched location EXCEPT this one - which reads as a missing artifact rather than missing config.
    // So: gate on the token alone and send a placeholder username when none was supplied.
    val eunomiaGprToken = providers.gradleProperty("gpr.token")
        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
    val eunomiaGprUser = providers.gradleProperty("gpr.user")
        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        .orElse("x-access-token")
    if (eunomiaGprToken.isPresent) {
        maven {
            name = "EunomiaGitHubPackages"
            url = uri("https://maven.pkg.github.com/zannagh/eunomia")
            credentials {
                username = eunomiaGprUser.get()
                password = eunomiaGprToken.get()
            }
            content { includeGroup("de.zannagh.eunomia") }
        }
    }
}

// 26.x variants compile to Java 25 bytecode (class file major 69); only JaCoCo >= 0.8.14 can read it, so
// pin the tooling rather than rely on the Gradle-bundled default.
jacoco {
    toolVersion = providers.gradleProperty("jacoco.version").getOrElse("0.8.14")
}

val sc = project.stonecutterBuild
val loader = sc.branch.id
sc.constants["fabric"] = sc.current.project.contains("fabric")
sc.constants["neoforge"] = sc.current.project.contains("neoforge")

// Register the MC version part as a property tag so version-shared sections
// in stonecutter.properties.toml (e.g. ["1.20.1"]) resolve correctly.
sc.properties.tags(sc.current.project.substringAfter('-'))

val javaVersion = findProperty("java.version")?.toString() ?: error("No Java version specified")
val displayVersion = findProperty("display_version")?.toString() ?: error("No display version specified")

val isPreRelease = findProperty("prerelease")?.toString()?.lowercase() != "false"
val semVer = findProperty("semVer")?.toString()?.takeIf { it.isNotEmpty() } ?: "0.0.1-preview.0"

version = "$semVer+$displayVersion"
group = property("maven_group").toString()

base {
    archivesName.set("${property("archives_base_name")}-$loader")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

tasks.jar {
    includeLicense(base.archivesName.get())
}

// Local-only publishing so third-party mods can develop against Armor Hider's API before a Modrinth
// release exists: `./gradlew publishToMavenLocal` drops the variant into ~/.m2/repository under
// <maven_group>:<archives_base_name>-<loader>:<semVer>+<display_version>. Deliberately no remote
// repository and no credentials - CI ships to Modrinth/CurseForge, not to a Maven host.
//
// On the loom (fabric/common) variants the `java` component's outgoing artifact is loom's `remapJar`,
// not the dev-namespace `jar`, so the published artifact is the same remapped jar that ships to
// Modrinth. NeoForge builds are already in official Mojang names, so its plain `jar` is correct.
publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Forward the opt-in switches for the HTTP/WebSocket fallback E2E (HttpFallbackE2ETest) from the Gradle
    // invocation into the forked test JVM. Absent by default, so a normal `./gradlew test` stays hermetic and
    // the E2E self-skips; set `-Darmorhider.fallback.e2e` to run it against the in-JVM stub relay, and
    // `-Darmorhider.relay.dotnet=<eunomia/csharp>` to run the same assertions against the real C# relay.
    // LiveRelayContractTest is NOT opt-in - it self-skips on an unreachable relay - so these two only override
    // its defaults: `armorhider.relay.live` points it at a relay other than the public instance, and
    // `armorhider.relay.live.uuid` supplies a real Minecraft account uuid to unlock the Mojang-gated full flow.
    listOf(
        "armorhider.fallback.e2e",
        "armorhider.relay.dotnet",
        "armorhider.relay.live",
        "armorhider.relay.live.uuid",
    ).forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    // only run tests once
    enabled = sc.current.isActive
    // Emit JaCoCo coverage (XML for CI/PR comment, HTML for humans + the IDE) right after the tests.
    finalizedBy(tasks.named("jacocoTestReport"))
}

// Coverage is only meaningful on the active variant (the only one whose `test` runs); the inactive
// branches have no exec data, so skip their reports rather than emit empty ones.
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    onlyIf { sc.current.isActive }
    // Mixins only execute inside a live client/server (Tier 2/3), never in the JVM unit tests, so they
    // would sit at 0% and drag the denominator down. Excluded from coverage for now - to be covered
    // later. Rebuilt from the source set (rather than filtering the convention value) so ordering with
    // the jacoco plugin's own wiring can't clobber it.
    classDirectories.setFrom(
        sourceSets["main"].output.classesDirs.asFileTree.matching { exclude("**/mixin/**") }
    )
    // JaCoCo's HTML formatter overwrites but never deletes, so a package that drops out of the report
    // (e.g. the now-excluded mixins) leaves a stale page behind until a clean. Wipe the dir first.
    doFirst { delete(reports.html.outputLocation) }
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// PaperSchemaContractTest reads :paper's compiled constants off the classpath (see the
// testImplementation files(...) entry in multiloader-loom), so they must be built first - on the
// compile task too, not just `test`, or Gradle rejects the undeclared cross-project input.
tasks.named("compileTestJava") {
    dependsOn(":paper:classes")
}
