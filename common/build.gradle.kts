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
    
    replacements.string(current.parsed <= "1.21.8") {
        replace("AvatarRenderState", "PlayerRenderState")
    }
    
    replacements.string(current.parsed < "1.21.11") {
        replace("net.minecraft.client.renderer.rendertype.RenderType", "net.minecraft.client.renderer.RenderType")
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

// Read version-specific access widener from resources
val awVersion = findProperty("accesswidener.version")?.toString() ?: "current"
val awSource = rootProject.file("common/accesswideners/armorhider.$awVersion.accesswideners")
val awFile = layout.buildDirectory.file("generated/armor-hider.accesswidener").get().asFile.also { it.parentFile.mkdirs() }
run {
    val awNamespace = if (project.isDeobf) "official" else "named"
    awFile.writeText(awSource.readText().replace("classTweaker v1 named", "classTweaker v1 $awNamespace"))
}

configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
    splitEnvironmentSourceSets()
    accessWidenerPath.set(awFile)

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

val accessWidener = findProperty("accesswidener.version")?.toString() ?: error("No access widener version specified")

val javaVersionProp = mapOf("java_version" to javaVersion)
val accessWidenerProp = mapOf("accesswidener.version" to accessWidener)

tasks.processResources {
    inputs.properties(javaVersionProp)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(javaVersionProp))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(javaVersionProp)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(javaVersionProp))
    
    inputs.properties(accessWidenerProp)
    filesMatching("fabric.mod.json", ExpandPropertiesAction(accessWidenerProp))
    
    // TODO: Copy matchin access widener (resolved via armorhider.${accesswidener.version}.accesswidener to META-INF/accesstransformer.cfg ?
}
