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

    // Access widener for custom RenderType creation (arm re-rendering after GeckoLib).
    // Only needed for < 1.21.9 (pre-deferred pipeline) where CompositeState is protected.
    if (sc.current.parsed < dev.kikugie.stonecutter.data.ParsedVersion("1.21.9")) {
        val awFile = rootProject.file("common/src/main/resources/armor-hider.accesswidener")
        if (awFile.exists()) {
            accessWidenerPath.set(awFile)
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
