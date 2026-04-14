plugins {
    id("multiloader-common")
}

apply(plugin = if (project.isDeobf) "loom-deobfuscated" else "loom-obfuscated")

val sc = project.stonecutterBuild

stonecutter {
    constants["fabric"] = sc.current.project.contains("fabric")
    constants["neoforge"] = sc.current.project.contains("neoforge")

    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
    }
    replacements.string(false) {
        replace("packet.getIdentifier()", "packet.getResourceLocation()")
    }
    
    replacements.string(current.parsed <= "1.21.8"){
        replace("AvatarRenderState", "PlayerRenderState")
    }

    replacements.string(current.parsed < "1.21.11"){
        replace("net.minecraft.client.model.player.PlayerModel", "net.minecraft.client.model.PlayerModel")
    }

    replacements.string(current.parsed < "26.1-0.snapshot.11"){
        replace("net.minecraft.client.renderer.state.level.CameraRenderState", "net.minecraft.client.renderer.state.CameraRenderState")
    }
    
    replacements.string(current.parsed <= "26.1-1.pre.1") {
        replace("net.minecraft.client.gui.GuiGraphicsExtractor","net.minecraft.client.gui.GuiGraphics")
    }
    
    replacements.string(current.parsed < "1.21") {
        replace("net.minecraft.client.gui.screens.options.OptionsSubScreen", "net.minecraft.client.gui.screens.OptionsSubScreen")
    }
    
    replacements.string(current.parsed < "1.21.9") {
        replace(".setScreenAndShow(", ".setScreen(")
    }
}

configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
    splitEnvironmentSourceSets()

    mixin {
        useLegacyMixinAp = false
    }

    // Shared run directory for all versions
    runConfigs.configureEach {
        runDir = "run"
    }
}

repositories {
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

dependencies {
    if (!project.isDeobf) {
        add("modCompileOnly", "net.fabricmc:fabric-loader:${property("loader_version")}")
    }

    if (!project.isDeobf) {
        val mcMinor = project.mcVersion.removePrefix("1.21.").toIntOrNull()
        if (mcMinor != null && mcMinor >= 9) {
            add("modCompileOnly", "maven.modrinth:geckolib:7qjQQSWv") // GL 5.3-alpha-3 Fabric 1.21.10
        } else if (project.mcVersion.startsWith("1.21")) {
            add("modCompileOnly", "maven.modrinth:geckolib:3GjkJptS") // GL 4.8.4 Fabric 1.21.1
        } else if (project.mcVersion.startsWith("1.20")) {
            add("modCompileOnly", "maven.modrinth:geckolib:PdrSPr53") // GL 4.8.3 Fabric 1.20.1
        }
    }

    // ElytraTrims compat — only needed for versions where the compat class compiles (>= 1.21.9).
    // Must use version-matched artifacts: MC types move packages across major versions.
    if (project.isDeobf) {
        compileOnly("maven.modrinth:elytra-trims:q7SmWLkn")       // ET 4.7.0 for 26.1
    } else if (project.mcVersion.let {
        it.startsWith("1.21.") && (it.removePrefix("1.21.").toIntOrNull() ?: 0) >= 9
    }) {
        add("modCompileOnly", "maven.modrinth:elytra-trims:iLC0LP3D") // ET 4.5.7 for 1.21.9+
    }

    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val javaVersion = findProperty("java.version")?.toString() ?: error("No Java version specified")

val expandProps = mapOf("java_version" to javaVersion)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(expandProps))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(expandProps))
}
