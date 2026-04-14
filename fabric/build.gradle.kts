plugins {
    id("multiloader-loader")
}

apply(plugin = if (project.isDeobf) "loom-deobfuscated" else "loom-obfuscated")

val sc = project.stonecutterBuild
val fabricVersion = findProperty("fabric.minecraft_version")?.toString()
    ?: error("No Fabric version mapping for Minecraft ${project.mcVersion}")

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
        ideConfigGenerated(true)
        if (project.isDeobf) {
            vmArg("-Dfabric.gameVersion=${fabricVersion}")
        }
    }
}

repositories {
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

dependencies {
    if (!project.isDeobf) {
        add("modImplementation", "net.fabricmc:fabric-loader:${property("loader_version")}")
    }

    // ElytraTrims compat — only for versions where compat class compiles (>= 1.21.9)
    if (project.isDeobf) {
        compileOnly("maven.modrinth:elytra-trims:q7SmWLkn")           // ET 4.7.0 for 26.1
    } else if (project.mcVersion.let {
        it.startsWith("1.21.") && (it.removePrefix("1.21.").toIntOrNull() ?: 0) >= 9
    }) {
        add("modCompileOnly", "maven.modrinth:elytra-trims:iLC0LP3D") // ET 4.5.7 Fabric 1.21.9+
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

val expandTask = registerExpandResourcesForIdea(
    tasks.named<ProcessResources>("processResources") to "out/production/resources",
    tasks.named<ProcessResources>("processClientResources") to "out/client/resources"
)
// Ensure Gradle fully compiles before IntelliJ runs — IntelliJ's "Make" doesn't trigger
// Stonecutter generation, so without this, generated sources can be stale.
expandTask.configure { dependsOn(tasks.classes, tasks.named("clientClasses")) }
patchLoomIdeRunConfigs(expandTask)
