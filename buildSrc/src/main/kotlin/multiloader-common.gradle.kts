plugins {
    id("java")
    id("java-library")
}

val sc = project.stonecutterBuild
val loader = sc.branch.id
val loaderName = sc.current.project.substringBefore("-")
sc.constants["fabric"] = loaderName == "fabric"
sc.constants["neoforge"] = loaderName == "neoforge"
sc.constants["quilt"] = loaderName == "quilt"
sc.constants["paper"] = loaderName == "paper"

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
}
