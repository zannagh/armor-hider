plugins {
    id("java")
    id("java-library")
}

val sc = project.stonecutterBuild
val javaVersionConverter = JavaVersionConverter(sc.current.parsed)
val supportedVersions = SupportedVersions(rootProject.file("supportedVersions.json"))

// 26.x is already deobfuscated — tell Loom to skip mappings and remapping.
// Must be set here (before fabric-loom is applied) because Loom reads this eagerly
// during plugin application and finalizes it on first read.
if (sc.current.project.startsWith("26.")) {
    project.extra.set("fabric.loom.disableObfuscation", "true")
}

version = Versioning.getModVersion(::findProperty) + "+" + supportedVersions.getDisplayVersion(sc.current.project)
group = property("maven_group").toString()

base {
    archivesName.set("${property("archives_base_name")}-${property("mod_loader")}")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersionConverter.getJavaVersionInt())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersionConverter.getJavaVersionInt()))
    }
    withSourcesJar()
    sourceCompatibility = javaVersionConverter.getJavaVersion()
    targetCompatibility = javaVersionConverter.getJavaVersion()
}

// Mixin JSON expansion — applied to all processResources tasks
tasks.withType<ProcessResources>().configureEach {
    val loader = project.property("mod_loader").toString()
    val mainMixin = MainMixins(sc.current.parsed, loader)
    val clientMixin = ClientMixins(sc.current.parsed, loader)

    inputs.property("java_version_string", javaVersionConverter.getJavaVersionString())
    inputs.property("java_version_int", javaVersionConverter.getJavaVersionInt())
    inputs.property("main_mixin_string", mainMixin.toString())
    inputs.property("client_mixin_string", clientMixin.toString())
    inputs.property("options_screen_mixin_string", clientMixin.getScreenMixinString())

    filesMatching("armor-hider.mixins.json") {
        expand(
            "java_version" to javaVersionConverter.getJavaVersionString(),
            "mixin_string" to mainMixin.toString()
        )
    }
    filesMatching("armor-hider.client.mixins.json") {
        expand(
            "java_version" to javaVersionConverter.getJavaVersionString(),
            "mixin_string" to clientMixin.toString(),
            "options_screen_mixin_string" to clientMixin.getScreenMixinString()
        )
    }
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
