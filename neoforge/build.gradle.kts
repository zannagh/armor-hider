plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val sc = project.stonecutterBuild

// NeoForge version per Minecraft version (latest available; -beta where no stable exists)
// 1.21/1.21.1 excluded: NeoForge's Mixin version is too old for @Inject(order=...)
val neoforgeVersionMap = mapOf(
    "1.21.4" to "21.4.156",
    "1.21.5" to "21.5.96",
    "1.21.6" to "21.6.20-beta",
    "1.21.7" to "21.7.25-beta",
    "1.21.8" to "21.8.52",
    "1.21.9" to "21.9.16-beta",
    "1.21.10" to "21.10.64",
    "1.21.11" to "21.11.38-beta"
)

val neoforgeVersion = neoforgeVersionMap[project.mcVersion]
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
    "minecraft_version" to project.prop("neoforge.minecraft_version")!!,
    "neoforge_version" to neoforgeVersion,
    "java_version" to project.prop("java.version")!!
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "**/*.mixins.json")) {
        expand(expandProps)
    }
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json") {
        expand(expandProps)
    }
}
