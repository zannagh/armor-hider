plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val sc = project.stonecutterBuild

val neoforgeVersion = findProperty("neoforge.version")?.toString()
    ?: error("No NeoForge version mapping for Minecraft ${project.mcVersion}")

val clientSourceSet = sourceSets.create("client") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

// NeoForge doesn't split environments — main has the full MC jar, so common client sources
// must also be in main for NeoForge-specific code that references common client classes.
val commonSourceSets = extra["commonSourceSets"] as SourceSetContainer
sourceSets.main {
    java { commonSourceSets["client"].java.srcDirs.forEach { srcDir(it) } }
    resources { commonSourceSets["client"].resources.srcDirs.forEach { srcDir(it) } }
}

stonecutter {
    constants["neoforge"] = true
    constants["quilt"] = false
    constants["paper"] = false
}

neoForge {
    version = neoforgeVersion

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
        }
    }

    mods {
        register("armor_hider") {
            sourceSet(sourceSets.main.get())
            sourceSet(clientSourceSet)
        }
    }
}

tasks.jar {
    from(clientSourceSet.output)
    // Common client sources are in both main and client (main needs them for compile visibility,
    // client gets them from multiloader-loader). Exclude duplicates in the jar.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val expandProps = mapOf(
    "version" to project.version,
    "minecraft_version" to project.prop("neoforge.minecraft_version_range")!!,
    "neoforge_version" to neoforgeVersion,
    "java_version" to project.prop("java.version")!!
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(expandProps))
}
