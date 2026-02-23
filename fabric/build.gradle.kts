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

tasks.processResources {
    val minecraftConstraint = findProperty("fabric.minecraft_version")!!.toString()
    val javaVersion = findProperty("java.version")!!.toString()
    inputs.property("version", project.version)
    inputs.property("fabric.minecraft_version", minecraftConstraint)
    inputs.property("java_version", javaVersion)
    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to minecraftConstraint,
            "java_version" to javaVersion
        )
    }
}
