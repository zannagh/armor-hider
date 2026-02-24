plugins {
    id("multiloader-loader")
}

apply(plugin = if (project.isDeobf) "loom-deobfuscated" else "loom-obfuscated")

val sc = project.stonecutterBuild
val supportedVersions = SupportedVersions(rootProject.file("supportedVersions.json"), "fabric")
// Fabric Loader needs "26.1-alpha.X" not "26.1-snapshot-X"
val versionTransform: (String) -> String = if (project.isDeobf) {
    { it.replace("-snapshot-", "-alpha.") }
} else {
    { it }
}

stonecutter {
    constants["fabric"] = true
}

configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
    splitEnvironmentSourceSets()

    mods {
        register("armor-hider") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }

    // Shared run directory for all versions
    runConfigs.configureEach {
        runDir = "run"
        if (project.isDeobf) {
            vmArg("-Dfabric.gameVersion=${versionTransform(project.mcVersion)}")
        }
    }
}

dependencies {
    if (!project.isDeobf) {
        add("modImplementation", "net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")
    }
}

val expandProps = mapOf(
    "version" to project.version,
    "java_version" to project.prop("java.version")!!,
    "fabric_minecraft_version" to project.prop("fabric.minecraft_version_range")!!
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("fabric.mod.json", "**/*.mixins.json")) {
        expand(expandProps)
    }
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json") {
        expand(expandProps)
    }
}
