plugins {
    id("multiloader-loader")
    id("fabric-loom")
}

val sc = project.stonecutterBuild
val supportedVersions = SupportedVersions(rootProject.file("supportedVersions.json"))
val isDeobf = sc.current.project.startsWith("26.")
// Fabric Loader needs "26.1-alpha.X" not "26.1-snapshot-X"
val versionTransform: (String) -> String = if (isDeobf) {
    { it.replace("-snapshot-", "-alpha.") }
} else {
    { it }
}

loom {
    mods {
        register("armor-hider") {
            sourceSet(sourceSets.main.get())
        }
    }

    // Shared run directory for all versions
    runConfigs.configureEach {
        runDir = "run"
        if (isDeobf) {
            vmArg("-Dfabric.gameVersion=${versionTransform(sc.current.project)}")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.project}")
    if (!isDeobf) {
        // String-based calls: these Loom configurations don't exist when obfuscation is disabled
        add("mappings", loom.officialMojangMappings())
        add("modImplementation", "net.fabricmc:fabric-loader:${property("loader_version")}")
    } else {
        implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    }
}

tasks.processResources {
    val minecraftConstraint = supportedVersions.getFabricVersionConstraint(sc.current.project, versionTransform)
    val javaVersionConverter = JavaVersionConverter(sc.current.parsed)
    inputs.property("version", project.version)
    inputs.property("minecraft_version", minecraftConstraint)
    inputs.property("java_version", javaVersionConverter.getJavaVersionInt())

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to minecraftConstraint,
            "java_version" to javaVersionConverter.getJavaVersionInt()
        )
    }
}
