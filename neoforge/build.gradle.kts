plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val sc = project.stonecutterBuild

val neoforgeVersion = findProperty("neoforge.version")?.toString()
    ?: error("No neoforge.version for ${sc.current.project}")
val neoforgeVersionRange = findProperty("neoforge.minecraft_version_range")?.toString()
    ?: error("No neoforge.minecraft_version_range for ${sc.current.project}")

val javaVersion = findProperty("java.version")?.toString()
    ?: error("No java.version for ${sc.current.project}")

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
}

val expandResourcesForIdea = registerExpandResourcesForIdea(
    tasks.named<ProcessResources>("processResources") to "out/production/resources"
)
// Ensure Gradle fully compiles before IntelliJ runs — IntelliJ's "Make" doesn't trigger
// Stonecutter generation, so without this, generated sources can be stale.
expandResourcesForIdea.configure { dependsOn(tasks.classes, tasks.named("clientClasses")) }
patchIdeRunConfigsAllowParallel()

neoForge {
    version = neoforgeVersion

    runs {
        register("client") {
            client()
            taskBefore(expandResourcesForIdea)
        }
        register("server") {
            server()
            taskBefore(expandResourcesForIdea)
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
    "minecraft_version" to neoforgeVersionRange,
    "neoforge_version" to neoforgeVersion,
    "java_version" to javaVersion
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}
