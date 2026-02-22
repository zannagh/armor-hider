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

val neoforgeVersion = neoforgeVersionMap[sc.current.project]
    ?: error("No NeoForge version mapping for Minecraft ${sc.current.project}")

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
        }
    }
}

tasks.processResources {
    val supportedVersions = SupportedVersions(rootProject.file("supportedVersions.json"), "neoforge")
    val javaVersionConverter = JavaVersionConverter(sc.current.parsed)
    val minecraftConstraint = supportedVersions.getNeoForgeVersionConstraint(sc.current.project)

    inputs.property("version", project.version)
    inputs.property("minecraft_version", minecraftConstraint)
    inputs.property("neoforge_version", neoforgeVersion)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            "version" to project.version,
            "minecraft_version" to minecraftConstraint,
            "neoforge_version" to neoforgeVersion
        )
    }
}
