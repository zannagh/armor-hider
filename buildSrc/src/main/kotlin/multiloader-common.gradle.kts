plugins {
    id("java")
    id("java-library")
}

val sc = project.stonecutterBuild
sc.constants["fabric"] = sc.current.project.contains("fabric")
sc.constants["neoforge"] = sc.current.project.contains("neoforge")
val loader = findProperty("mod_loader")!!.toString()
val supportedVersions = SupportedVersions(rootProject.file("supportedVersions.json"), loader)
val javaVersion = findProperty("java.version")!!.toString()

version = Versioning.getModVersion(::findProperty) + "+" + supportedVersions.getDisplayVersion(project.mcVersion)
group = property("maven_group").toString()

base {
    archivesName.set("${property("archives_base_name")}-$loader")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Tests use shared, version-independent code — only run for the active version
    enabled = sc.current.isActive
}
