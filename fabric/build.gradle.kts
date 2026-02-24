plugins {
    id("multiloader-loader")
}

apply(plugin = if (project.isDeobf) "loom-deobfuscated" else "loom-obfuscated")

val sc = project.stonecutterBuild
val fabricVersion = findProperty("fabric.minecraft_version")!!.toString()

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
            vmArg("-Dfabric.gameVersion=${fabricVersion}")
        }
    }
}

dependencies {
    if (!project.isDeobf) {
        add("modImplementation", "net.fabricmc:fabric-loader:${property("loader_version")}")
    }
}

val expandProps = mapOf(
    "version" to project.version,
    "java_version" to project.prop("java.version")!!,
    "fabric_minecraft_version" to project.prop("fabric.minecraft_version_range")!!
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("fabric.mod.json", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(expandProps))
}
