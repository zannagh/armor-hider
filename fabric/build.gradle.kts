plugins {
    id("multiloader-loader")
    id("fabric-loom")
}

val sc = project.stonecutterBuild
val supportedVersions = SupportedVersions(rootProject.file("supportedVersions.json"), "fabric")
val isDeobf = project.mcVersion.startsWith("26.")
// Fabric Loader needs "26.1-alpha.X" not "26.1-snapshot-X"
val versionTransform: (String) -> String = if (isDeobf) {
    { it.replace("-snapshot-", "-alpha.") }
} else {
    { it }
}

stonecutter {
    constants["fabric"] = true
}

loom {
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
        if (isDeobf) {
            vmArg("-Dfabric.gameVersion=${versionTransform(project.mcVersion)}")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.mcVersion}")
    if (!isDeobf) {
        // String-based calls: these Loom configurations don't exist when obfuscation is disabled
        add("mappings", loom.officialMojangMappings())
        add("modImplementation", "net.fabricmc:fabric-loader:${property("loader_version")}")
    } else {
        implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
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
