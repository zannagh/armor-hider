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

    val modDep = if (project.isDeobf) "compileOnly" else "modCompileOnly"
    if (hasProperty("geckolib.version")) {
        add(modDep, "maven.modrinth:geckolib:${prop("geckolib.version")}")
    }
    if (hasProperty("elytratrims.version")) {
        add(modDep, "maven.modrinth:elytra-trims:${prop("elytratrims.version")}")
    }

    compileOnly("net.luckperms:api:5.4")
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
