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
    constants["mekanism"] = hasProperty("mekanism.version")
    constants["waveycapes"] = hasProperty("waveycapes.version")
    // `gender` activates the modern GenderArmorLayer-based mixin.
    // `gender_legacy` activates the GenderLayer.render() coarse mixin for older
    // mod builds (e.g. female-gender NeoForge 1.21/1.21.1, hash kKffHCGl) whose
    // jar ships only the legacy GenderLayer API.
    constants["gender"] = hasProperty("gender.version") && findProperty("gender_legacy_api") != "true"
    constants["gender_legacy"] = hasProperty("gender.version") && findProperty("gender_legacy_api") == "true"
    // `fcgt` activates the Phase 2 smoke test (fabric-client-gametest-api-v1) — true on
    // Fabric variants that pin `fabricapi.semver` so the FCGT module classpath wiring,
    // entrypoint, run task and stonecutter-gated test class line up consistently.
    constants["fcgt"] = hasProperty("fabricapi.semver") && current.project.contains("fabric")
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
        if (hasProperty("iris.version")) {
            add(modClientDep, "maven.modrinth:iris:${findProperty("iris.version")}")
        }
        if (hasProperty("emf.version")) {
            add(modClientDep, "maven.modrinth:entity-model-features:${findProperty("emf.version")}")
        }
        if (hasProperty("etf.version")) {
            add(modClientDep, "maven.modrinth:entitytexturefeatures:${findProperty("etf.version")}")
        }
        if (hasProperty("mekanism.version")) {
            add(modClientDep, "maven.modrinth:mekanism:${findProperty("mekanism.version")}")
        }
        if (hasProperty("waveycapes.version")) {
            add(modClientDep, "maven.modrinth:wavey-capes:${findProperty("waveycapes.version")}")
        }
        if (hasProperty("figura.version")) {
            add(modClientDep, "maven.modrinth:figura:${findProperty("figura.version")}")
        }
        if (hasProperty("modmenu.version")) {
            add(modClientDep, "maven.modrinth:modmenu:${findProperty("modmenu.version")}")
        }
        if (hasProperty("gender.version")) {
            add(modClientDep, "maven.modrinth:female-gender:${findProperty("gender.version")}")
        }
        // Phase 2 smoke: FCGT (fabric-client-gametest-api-v1) compile-time dep on common.
        if (sc.current.project.contains("fabric") && hasProperty("fabricapi.semver")) {
            val fabricApiSemver = findProperty("fabricapi.semver")!!.toString()
            val fabricApi = project.extensions.getByType(net.fabricmc.loom.api.fabricapi.FabricApiExtension::class.java)
            add(modClientDep, fabricApi.module("fabric-client-gametest-api-v1", fabricApiSemver))
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

    val shouldLoadDevProfile = !gradle.startParameter.isOffline && gradle.startParameter.taskNames.any { taskName ->
        val simple = taskName.substringAfterLast(':')
        simple.startsWith("run") || simple == "genIntellijRuns"
    }
    val devProfile = if (shouldLoadDevProfile) loadDevProfile() else null

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
            if (project.hasProperty("smoke")) {
                vmArg("-Darmorhider.smoke.exit=true")
                val delayMs = project.findProperty("smoke.delay.ms")?.toString() ?: "15000"
                vmArg("-Darmorhider.smoke.delay.ms=${delayMs}")
            }
            // Dev/UI testing: seed N fake players into the head bar of the per-player screen so the
            // horizontal scroll can be exercised without spawning real clients. Read by
            // IndividualPlayerConfigurationsScreen via Integer.getInteger("armorhider.demo.players").
            // Enable with e.g. -Pdemo.players=30 on any runClient invocation.
            if (project.hasProperty("demo.players")) {
                vmArg("-Darmorhider.demo.players=${project.findProperty("demo.players")}")
            }
            if (devProfile != null) {
                programArg("--username")
                programArg(devProfile.username)
                programArg("--uuid")
                programArg(devProfile.uuid)
                if (devProfile.skinTexturesValue != null) {
                    vmArg("-Darmorhider.dev.skin.textures=${devProfile.skinTexturesValue}")
                }
                if (devProfile.skinTexturesSignature != null) {
                    vmArg("-Darmorhider.dev.skin.signature=${devProfile.skinTexturesSignature}")
                }
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
        if (hasProperty("modmenu.version")) {
            val modMenuDep = if (isDeobf) "compileOnly" else "modCompileOnly"
            add(modMenuDep, "maven.modrinth:modmenu:${findProperty("modmenu.version")}")
        }
        // FCGT module — multiloader-loader adds common's src as srcDirs, so the test class
        // compiles here too, AND it must be on the dev runtime classpath because the
        // upstream Modrinth fabric-api jar (the one in run/mods/) does not bundle the
        // experimental FCGT module. Without this loom-side runtime entry the FCGT mixin
        // plugin's lifecycle hooks never load, MC boots vanilla and idles at the title.
        // Phase 2 smoke: FCGT (fabric-client-gametest-api-v1) compile classpath on the fabric
        // loader. The runtime side is handled via a copy-to-run/mods task below — the
        // upstream Modrinth fabric-api jar is the experimental-stripped umbrella and doesn't
        // include the FCGT module, so even with fabric-api in run/mods FCGT's mixin plugin
        // doesn't load.
        if (hasProperty("fabricapi.semver")) {
            val fabricApiSemver = findProperty("fabricapi.semver")!!.toString()
            val fabricApi = project.extensions.getByType(net.fabricmc.loom.api.fabricapi.FabricApiExtension::class.java)
            val fcgtModule = fabricApi.module("fabric-client-gametest-api-v1", fabricApiSemver)
            val compileDep = if (isDeobf) "clientCompileOnly" else "modClientCompileOnly"
            add(compileDep, fcgtModule)
        }
    }

    // FCGT (fabric-client-gametest-api-v1) entrypoint registered only on Fabric variants
    // that pin `fabricapi.semver` (currently fabric-26.2). Other variants emit "[]" so the
    // JSON stays valid and fabric-loader simply ignores it.
    val fcgtEntries = if (hasProperty("fabricapi.semver"))
        "[\"de.zannagh.armorhider.smoke.EntityRenderSmokeTest\", \"de.zannagh.armorhider.smoke.IndividualConfigSmokeTest\"]"
    else
        "[]"

    // The declared fabricloader floor must track the access-widener FORMAT, not the loader we
    // build against: a classTweaker v1 header requires Fabric Loader >= 0.18.0, while the older
    // accessWidener v2 format is understood since >= 0.15.0. Deriving it from the file (rather
    // than hardcoding it in fabric.mod.json) keeps the floor honest if a bucket's format changes,
    // so a user on a too-old loader gets a clean "update Fabric Loader" dependency error instead
    // of a hard, unhandled accessWidener parse crash during Knot.init.
    val awFloorVersion = findProperty("accesswidener.version")?.toString() ?: "current"
    val awFloorSource = rootProject.file("common/accesswideners/armorhider.$awFloorVersion.accesswideners")
    val fabricLoaderMin = if (awFloorSource.readText().trimStart().startsWith("classTweaker")) "0.18.0" else "0.15.0"

    val expandProps = mapOf(
        "version" to project.version,
        "java_version" to (findProperty("java.version")?.toString() ?: error("No Java version")),
        "fabric_minecraft_version" to (findProperty("fabric.minecraft_version_range")?.toString() ?: error("No Fabric version range")),
        "accesswidener" to (findProperty("accesswidener.version")?.toString() ?: "current"),
        "fabricloader_min" to fabricLoaderMin,
        "fcgt_entries" to fcgtEntries
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

    // When -Psmoke is set, populate run/mods with the configured compat jars before launching.
    if (project.hasProperty("smoke")) {
        tasks.named("runClient") { dependsOn("fetchCompatJars") }
    }

    // ── Phase 2 smoke: FCGT-driven entity render run config ──────────────────────────
    // Registers `runClientGametest` on Fabric variants that pin `fabricapi.semver`.
    // FCGT discovers the `fabric-client-gametest` entrypoint, swaps the main loop for the
    // test driver, runs EntityRenderSmokeTest.runTest, exits cleanly.
    if (sc.current.project.contains("fabric") && hasProperty("fabricapi.semver")) {
        loom.apply {
            runConfigs.create("clientGametest") {
                client()
                runDir = "run"
                name = "Client GameTest"
                ideConfigGenerated(true)
                // FCGT activates via TWO properties (verified by decompiling the runner):
                //  - `fabric.client.gametest` (any value) → ClientGameTestMixinConfigPlugin
                //     applies the lifecycle/threading mixins that hand control to the runner.
                //  - `fabric.client.gametest.modid` → FabricClientGameTestRunner uses this to
                //     filter `fabric-client-gametest` entrypoints to dispatch. Without it,
                //     the mixins fire but no test class runs and MC sits at the title screen.
                vmArg("-Dfabric.client.gametest=true")
                vmArg("-Dfabric.client.gametest.modid=armor-hider")
                // Phase 1's exit timer would race FCGT's own shutdown — disable on this run.
                vmArg("-Darmorhider.smoke.exit=false")
            }
        }
        // Resolve the FCGT module artifact via a dedicated configuration so we can copy the
        // resolved (already named-mapped) jar into run/mods. fabric-api's umbrella jar
        // doesn't include FCGT (it's marked experimental upstream), so this is the only path
        // that actually puts the module classes on fabric-loader's runtime classpath.
        val fcgtRuntimeMod = configurations.create("fcgtRuntimeMod") {
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
        }
        val fabricApiExt = project.extensions.getByType(net.fabricmc.loom.api.fabricapi.FabricApiExtension::class.java)
        dependencies.add(
            "fcgtRuntimeMod",
            fabricApiExt.module("fabric-client-gametest-api-v1", findProperty("fabricapi.semver")!!.toString())
        )
        val copyFcgtToMods = tasks.register<Copy>("copyFcgtToMods") {
            group = "verification"
            description = "Drop the FCGT module jar into run/mods/ so its mixin plugin loads at runtime"
            from(fcgtRuntimeMod)
            into(project.layout.projectDirectory.dir("run/mods"))
            // fetchFcgtCompatJars wipes run/mods first — make sure that runs before this copy.
            mustRunAfter("fetchFcgtCompatJars")
            // Never cache: pair task is also non-cached, and we want the FCGT jar to land
            // every time runClientGametest fires so cross-row leaks can't strand us with a
            // stale mods/ dir between BOOT and ENTITY_RENDER rows.
            outputs.upToDateWhen { false }
        }

        if (project.hasProperty("smoke")) {
            tasks.named("runClientGametest") {
                dependsOn("fetchFcgtCompatJars")
                dependsOn(copyFcgtToMods)
            }
        }
    }
}
