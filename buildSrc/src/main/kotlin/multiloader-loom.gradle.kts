import dev.kikugie.stonecutter.build.StonecutterBuildExtension

val isDeobf = extra.has("loom.deobf") && extra.get("loom.deobf") as Boolean
val sc = project.stonecutterBuild
val branch = sc.branch.id
val mcVersion = sc.current.project.substringAfter('-')

// ── Base setup ──
if (branch == "common") {
    apply(plugin = "multiloader-common")
} else {
    apply(plugin = "multiloader-loader")
}

// ── Loom ──
if (isDeobf) {
    extra.set("fabric.loom.disableObfuscation", "true")
}
apply(plugin = "fabric-loom")

val loom = the<net.fabricmc.loom.api.LoomGradleExtensionAPI>()

dependencies {
    "minecraft"("com.mojang:minecraft:$mcVersion")
    if (isDeobf) {
        "implementation"("net.fabricmc:fabric-loader:${property("loader_version")}")
    } else {
        "mappings"(loom.officialMojangMappings())
    }
}

repositories {
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

// ── Stonecutter constants ──
with(sc) {
    constants["fabric"] = current.project.contains("fabric")
    constants["neoforge"] = current.project.contains("neoforge")
}

// ── Common branch ──
if (branch == "common") {
    with(sc) {
        replacements.string(current.parsed >= "26.1-0.snapshot.11") {
            replace("software.bernie.geckolib", "com.geckolib")
        }
    }

    val awVersion = findProperty("accesswidener.version")?.toString() ?: "current"
    val awSource = rootProject.file("common/accesswideners/armorhider.$awVersion.accesswideners")
    val awFile = layout.buildDirectory.file("generated/armor-hider.accesswidener").get().asFile.also { it.parentFile.mkdirs() }
    run {
        val awNamespace = if (isDeobf) "official" else "named"
        awFile.writeText(awSource.readText().replace("classTweaker v1 named", "classTweaker v1 $awNamespace"))
    }

    loom.apply {
        splitEnvironmentSourceSets()
        accessWidenerPath.set(awFile)
        mixin { useLegacyMixinAp = false }
        runConfigs.configureEach { runDir = "run" }
    }

    dependencies {
        if (!isDeobf) {
            add("modCompileOnly", "net.fabricmc:fabric-loader:${property("loader_version")}")
        }
        val modDep = if (isDeobf) "compileOnly" else "modCompileOnly"
        val modClientDep = if (isDeobf) "clientCompileOnly" else "modClientCompileOnly"
        if (hasProperty("geckolib.version")) {
            add(modDep, "maven.modrinth:geckolib:${findProperty("geckolib.version")}")
            add(modClientDep, "maven.modrinth:geckolib:${findProperty("geckolib.version")}")
        }
        if (hasProperty("elytratrims.version")) {
            add(modDep, "maven.modrinth:elytra-trims:${findProperty("elytratrims.version")}")
        }
        add("compileOnly", "net.luckperms:api:5.4")
        add("compileOnly", "org.jspecify:jspecify:1.0.0")
        add("testImplementation", platform("org.junit:junit-bom:6.0.1"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    val javaVersionStr = findProperty("java.version")?.toString() ?: error("No Java version specified")
    val awVersionStr = findProperty("accesswidener.version")?.toString() ?: error("No access widener version specified")
    val javaVersionProp = mapOf("java_version" to javaVersionStr)

    tasks.named<ProcessResources>("processResources") {
        inputs.properties(javaVersionProp)
        filesMatching("**/*.mixins.json", ExpandPropertiesAction(javaVersionProp))
    }
    tasks.named<ProcessResources>("processClientResources") {
        inputs.properties(javaVersionProp)
        filesMatching("**/*.mixins.json", ExpandPropertiesAction(javaVersionProp))
        inputs.properties(mapOf("accesswidener.version" to awVersionStr))
        filesMatching("fabric.mod.json", ExpandPropertiesAction(mapOf("accesswidener.version" to awVersionStr)))
    }
}

// ── Fabric branch ──
if (branch == "fabric") {
    val fabricVersion = findProperty("fabric.minecraft_version")?.toString()
        ?: error("No Fabric version mapping for Minecraft $mcVersion")

    val commonProj = extra["commonProject"] as Project
    val commonLoom = commonProj.extensions.getByType(net.fabricmc.loom.api.LoomGradleExtensionAPI::class.java)

    loom.apply {
        splitEnvironmentSourceSets()
        accessWidenerPath.set(commonLoom.accessWidenerPath)
        mods {
            register("armor-hider") {
                sourceSet(project.extensions.getByType(SourceSetContainer::class.java).getByName("main"))
                sourceSet(project.extensions.getByType(SourceSetContainer::class.java).getByName("client"))
            }
        }
        runConfigs.configureEach {
            runDir = "run"
            ideConfigGenerated(true)
            if (isDeobf) {
                vmArg("-Dfabric.gameVersion=${fabricVersion}")
            }
        }
    }

    dependencies {
        if (!isDeobf) {
            add("modImplementation", "net.fabricmc:fabric-loader:${property("loader_version")}")
        }
        if (isDeobf) {
            add("compileOnly", "maven.modrinth:elytra-trims:q7SmWLkn")
        } else if (mcVersion.let {
            it.startsWith("1.21.") && (it.removePrefix("1.21.").toIntOrNull() ?: 0) >= 9
        }) {
            add("modCompileOnly", "maven.modrinth:elytra-trims:iLC0LP3D")
        }
    }

    val expandProps = mapOf(
        "version" to project.version,
        "java_version" to (findProperty("java.version")?.toString() ?: error("No Java version")),
        "fabric_minecraft_version" to (findProperty("fabric.minecraft_version_range")?.toString() ?: error("No Fabric version range")),
        "accesswidener" to (findProperty("accesswidener.version")?.toString() ?: "current")
    )

    tasks.named<ProcessResources>("processResources") {
        inputs.properties(expandProps)
        filesMatching(listOf("fabric.mod.json", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
        val awNamespace = if (isDeobf) "official" else "named"
        from(rootProject.file("common/accesswideners"), Action<org.gradle.api.file.CopySpec> {
            include("armorhider.${expandProps["accesswidener"]}.accesswideners")
            filter { it.replace("classTweaker v1 named", "classTweaker v1 $awNamespace") }
        })
    }
    tasks.named<ProcessResources>("processClientResources") {
        inputs.properties(expandProps)
        filesMatching("**/*.mixins.json", ExpandPropertiesAction(expandProps))
    }

    val expandTask = registerExpandResourcesForIdea(
        tasks.named<ProcessResources>("processResources") to "out/production/resources",
        tasks.named<ProcessResources>("processClientResources") to "out/client/resources"
    )
    expandTask.configure { dependsOn(tasks.named("classes"), tasks.named("clientClasses")) }
    patchLoomIdeRunConfigs(expandTask)
}
