plugins {
    id("multiloader-loader")
}

apply(plugin = if (project.isDeobf) "loom-deobfuscated" else "loom-obfuscated")

val sc = project.stonecutterBuild

stonecutter {
    constants["quilt"] = true
    constants["fabric"] = false
    constants["paper"] = false
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
            vmArg("-Dfabric.gameVersion=${findProperty("quilt.minecraft_version")}")
        }
    }
}

repositories {
    maven("https://maven.quiltmc.org/repository/release/")
}

dependencies {
    if (!project.isDeobf) {
        // Fabric Loader for compile-time Mixin/ASM APIs; Quilt Loader for runtime
        add("modCompileOnly", "net.fabricmc:fabric-loader:${property("loader_version")}")
        add("modRuntimeOnly", "org.quiltmc:quilt-loader:${property("quilt_loader_version")}")
    }
}

val expandProps = mapOf(
    "version" to project.version,
    "java_version" to project.prop("java.version")!!,
    "quilt_minecraft_version" to project.prop("quilt.minecraft_version_range")!!
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("quilt.mod.json", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(expandProps))
}
