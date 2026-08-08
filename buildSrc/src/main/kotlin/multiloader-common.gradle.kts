plugins {
    id("java")
    id("java-library")
    id("jacoco")
}

repositories {
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
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
