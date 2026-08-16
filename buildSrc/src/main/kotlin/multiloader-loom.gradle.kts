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
    // Deeper and Darker renamed the warden-helmet horn RenderLayer between mod versions:
    // 1.3.x ships `HelmetHornRenderer`, 1.4.x renamed it `WardenHelmetRenderer`. The version does NOT
    // track the MC version (e.g. D&D's Fabric 1.21.1 build is still 1.3.3), so a per-variant flag
    // `deeperdarker.warden_class` selects which compat mixin (and thus which class) compiles.
    constants["deeperdarker_warden"] = hasProperty("deeperdarker.version") && findProperty("deeperdarker.warden_class") == "true"
    constants["deeperdarker_horn"] = hasProperty("deeperdarker.version") && findProperty("deeperdarker.warden_class") != "true"
    // Uranus lib (iafenvoy, e.g. Ice and Fire: CE) - custom armor rendering via IArmorRendererBase.
    constants["uranus"] = hasProperty("uranus.version")
    // Immersive Armors (Conczin) cancels the vanilla equipment layer and draws its armor as its own
    // `Piece` list. It only ships for 1.20.1, 1.21.1, 26.1.2 and 26.2, and its render path changed
    // shape twice across those (float rgb → packed argb → SubmitNodeCollector), so the compat mixins
    // only compile on the variants where the two mods actually overlap.
    constants["immersivearmors"] = hasProperty("immersivearmors.version")
    // `gender` activates the modern GenderArmorLayer-based mixin.
    // `gender_legacy` activates GenderLegacyLayerMixin for the older FGM builds
    // (e.g. female-gender NeoForge 1.21/1.21.1/1.21.2/1.21.3, hash kKffHCGl) whose
    // GenderLayer.render draws the breast body AND armor inline with no separate
    // renderBreastArmor hook. That mixin hides only the inline breast armor (by
    // substituting FGM's non-covering "no armor" config when the chest is hidden),
    // leaving the breast body intact - it does not touch the modern GenderArmorLayer.
    constants["gender"] = hasProperty("gender.version") && findProperty("gender_legacy_api") != "true"
    constants["gender_legacy"] = hasProperty("gender.version") && findProperty("gender_legacy_api") == "true"
    // First Person Model (tr7zw) renders the local player's body in first person, so layers we hook
    // (head, wings, held item) submit for the camera entity - and FPM cancels several of them at
    // their submit HEAD. `firstperson` compiles the typed guard that keeps our render scopes from
    // leaking past those cancels. Fabric-only: the property is pinned on fabric variants only.
    constants["firstperson"] = hasProperty("firstperson.version")
    // `fcgt` activates the Phase 2 smoke test (fabric-client-gametest-api-v1) - true on
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
        runConfigs.configureEach { runDirectory.set(layout.projectDirectory.dir("run")) }
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
        if (hasProperty("deeperdarker.version")) {
            add(modClientDep, "maven.modrinth:deeperdarker:${findProperty("deeperdarker.version")}")
        }
        if (hasProperty("uranus.version")) {
            add(modClientDep, "maven.modrinth:uranus:${findProperty("uranus.version")}")
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
        // Accessory providers (issue #246), Fabric side. Curios is NeoForge-only (added on the neoforge
        // project). Compat is @Pseudo/@Coerce, so these are compile-only parity deps + smoke-fetch sources.
        if (hasProperty("trinkets.version")) {
            add(modClientDep, "maven.modrinth:trinkets:${findProperty("trinkets.version")}")
        }
        if (hasProperty("accessories.version")) {
            add(modClientDep, "maven.modrinth:accessories:${findProperty("accessories.version")}")
        }
        // Fabric-only. Declared here for the remapped common compile; the loader project compiles common's
        // sources too, so multiloader-loader.gradle.kts declares the same coordinate unremapped. That pairing
        // only works because FirstPersonCompat avoids every FPM member whose signature names a Minecraft type.
        if (hasProperty("firstperson.version")) {
            add(modClientDep, "maven.modrinth:first-person-model:${findProperty("firstperson.version")}")
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
        // :paper's compiled classes, for PaperSchemaContractTest - the Paper plugin re-declares the
        // parts of the wire schema it has to understand (the serverWideSettings block, the channel
        // names), and nothing else would notice if the mod's side moved. The classes it asserts on
        // are Bukkit-free, so no paper-api is needed here.
        //
        // A raw classes dir rather than `project(":paper")`: :paper disables its thin `jar` task in
        // favour of the shaded one, so there is no default artifact to resolve, and re-enabling it
        // would put a second armor-hider-paper-*.jar in reach of the publish globs.
        add("testImplementation",
            files(rootProject.layout.projectDirectory.dir("paper/build/classes/java/main")))
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

    // ── Paper end-to-end smoke identity ──────────────────────────────────────────────
    // PaperHandshakeSmokeTest connects to an externally-started PaperMC server that has already
    // OP'd the test player BY NAME before the client launches. The dev client otherwise generates
    // `Player<millis%1000>` - a different name every launch - so the seeded OP entry would never
    // match and the PermissionPacket would come back as level 0, failing the run for the wrong
    // reason. Pin a fixed name (and its offline-mode UUID) whenever the Paper port is supplied.
    // This deliberately REPLACES any dev-profile identity rather than adding to it: MC's arg
    // parser cannot take `--username` twice.
    val paperSmokePort = findProperty("smoke.paper.port")?.toString()
    val paperSmokeUsername = "ArmorHiderSmoke"
    val paperSmokeUuid = java.util.UUID
        .nameUUIDFromBytes("OfflinePlayer:$paperSmokeUsername".toByteArray(Charsets.UTF_8))
        .toString()
    val runProfile = if (paperSmokePort != null) {
        DevProfile(paperSmokeUsername, paperSmokeUuid)
    } else {
        devProfile
    }

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
            runDirectory.set(layout.projectDirectory.dir("run"))
            generateRunConfig.set(true)
            // Dev-run safety net: halt this game JVM if the launcher (gradle/IDE) that spawned it dies,
            // so an interrupted runClient/runServer never orphans a multi-GB Minecraft JVM. Dev-only;
            // production jars never see this property. See DevRunWatchdog.
            jvmArguments.add("-Darmorhider.devRun.watchdog=true")
            if (isDeobf) {
                jvmArguments.add("-Dfabric.gameVersion=${fabricVersion}")
            }
            if (project.hasProperty("smoke")) {
                // Smoke scenes are tiny synthetic worlds (boot-to-title, or a one-player FCGT world),
                // so the client needs nowhere near a real session's heap. Capping it keeps the box light
                // and - the real payoff - lets many more clients run concurrently when the matrix is
                // parallelised (~2g each vs the uncapped ~default lets ~10 fit in 23g instead of ~5).
                jvmArguments.add("-Xmx2g")
                jvmArguments.add("-Darmorhider.smoke.exit=true")
                val delayMs = project.findProperty("smoke.delay.ms")?.toString() ?: "15000"
                jvmArguments.add("-Darmorhider.smoke.delay.ms=${delayMs}")
                // With compat mods fetched into run/mods, assert the mixin-safe resource probe actually
                // detected every present mod (a present-but-unprobed mod = silent compat gating failure).
                val compat = project.findProperty("compat")?.toString() ?: "none"
                if (compat != "none") {
                    jvmArguments.add("-Darmorhider.smoke.assertCompat=true")
                }
            }
            // Dev/UI testing: seed N fake players into the head bar of the per-player screen so the
            // horizontal scroll can be exercised without spawning real clients. Read by
            // IndividualPlayerConfigurationsScreen via Integer.getInteger("armorhider.demo.players").
            // Enable with e.g. -Pdemo.players=30 on any runClient invocation.
            if (project.hasProperty("demo.players")) {
                jvmArguments.add("-Darmorhider.demo.players=${project.findProperty("demo.players")}")
            }
            // Port of an externally-started PaperMC server for the end-to-end handshake smoke.
            // PaperHandshakeSmokeTest skips itself when this is absent, so normal runs are unaffected.
            if (paperSmokePort != null) {
                jvmArguments.add("-Darmorhider.smoke.paper.port=${paperSmokePort}")
            }
            if (runProfile != null) {
                programArguments.add("--username")
                programArguments.add(runProfile.username)
                programArguments.add("--uuid")
                programArguments.add(runProfile.uuid)
                if (runProfile.skinTexturesValue != null) {
                    jvmArguments.add("-Darmorhider.dev.skin.textures=${runProfile.skinTexturesValue}")
                }
                if (runProfile.skinTexturesSignature != null) {
                    jvmArguments.add("-Darmorhider.dev.skin.signature=${runProfile.skinTexturesSignature}")
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
        // FCGT module - multiloader-loader adds common's src as srcDirs, so the test class
        // compiles here too, AND it must be on the dev runtime classpath because the
        // upstream Modrinth fabric-api jar (the one in run/mods/) does not bundle the
        // experimental FCGT module. Without this loom-side runtime entry the FCGT mixin
        // plugin's lifecycle hooks never load, MC boots vanilla and idles at the title.
        // Phase 2 smoke: FCGT (fabric-client-gametest-api-v1) compile classpath on the fabric
        // loader. The runtime side is handled via a copy-to-run/mods task below - the
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
    // (short id, class name) so `-Psmoke.fcgt.only=` can select a subset by a stable, typo-proof
    // name. The id is part of the build contract - PaperE2ESmokeTest passes `paper-handshake`.
    val fcgtTestCatalog = buildList {
        add("entity-render" to "de.zannagh.armorhider.smoke.EntityRenderSmokeTest")
        add("individual-config" to "de.zannagh.armorhider.smoke.IndividualConfigSmokeTest")
        add("keybind" to "de.zannagh.armorhider.smoke.KeybindSmokeTest")
        add("combat-detection" to "de.zannagh.armorhider.smoke.CombatDetectionSmokeTest")
        // Paper end-to-end handshake smoke. Gated only on `fcgt` like the class itself: it no-ops
        // unless -Psmoke.paper.port is supplied, so registering it everywhere is harmless.
        add("paper-handshake" to "de.zannagh.armorhider.smoke.PaperHandshakeSmokeTest")
        // WaterTransparencySmokeTest drives the after-terrain feature phase (the fix), which only
        // exists >= 26.2-1.pre - its class is stonecutter-gated to the same floor, so only register
        // the entrypoint there or fabric-loader would fail to find the commented-out class.
        if (sc.current.parsed >= "26.2-1.pre") {
            add("water-transparency" to "de.zannagh.armorhider.smoke.WaterTransparencySmokeTest")
            // GlintTransparencySmokeTest (issue #324) shares the same FCGT-API + render-architecture
            // floor as the water smoke; its class is stonecutter-gated to the same range.
            add("glint-transparency" to "de.zannagh.armorhider.smoke.GlintTransparencySmokeTest")
            // OpaqueGlintOffSmokeTest (Iris shader bleed regression) shares the same floor.
            add("opaque-glint-off" to "de.zannagh.armorhider.smoke.OpaqueGlintOffSmokeTest")
            // Female Gender Mod breast-armor render + physics smoke. Needs the FGM jar present
            // (pulled in on the gender smoke row) and the after-terrain render architecture, so it
            // shares WaterTransparency's floor. Class is stonecutter-gated to the same range.
            add("gender-breast-armor" to "de.zannagh.armorhider.smoke.GenderBreastArmorSmokeTest")
            add("armored-elytra-gender" to "de.zannagh.armorhider.smoke.ArmoredElytraGenderSmokeTest")
            // Iris translucent-body repro (#342 follow-up). Real-GPU only; run in isolation with
            // -Psmoke.fcgt.only=iris-translucency on a dev machine with the run/ Iris shaderpack.
            add("iris-translucency" to "de.zannagh.armorhider.smoke.IrisTranslucencySmokeTest")
        }
        // ElytraTrims transparency smoke. Its class (and the ETElytraTrimSubmitMixin it exercises) is
        // stonecutter-gated to `>= 1.21.9 && < 26.3-0.snapshot.2`, so register the entrypoint on exactly
        // that range or fabric-loader would try to resolve a commented-out class. Self-detects ET, so
        // it's harmless without the jar; run it in isolation with
        // `-Psmoke.fcgt.only=elytra-trims -Pcompat=elytratrims`.
        if (sc.current.parsed >= "1.21.9" && sc.current.parsed < "26.3-0.snapshot.2") {
            add("elytra-trims" to "de.zannagh.armorhider.smoke.ElytraTrimsSmokeTest")
        }
        // First Person Model compat smoke. Guard must stay identical to the test class's own
        // `//? if fcgt && firstperson {` gate, or fabric-loader tries to resolve a commented-out class.
        if (hasProperty("firstperson.version")) {
            add("first-person" to "de.zannagh.armorhider.smoke.FirstPersonSmokeTest")
        }
        // EMF / Fresh Animations arm-detachment repro (issue #217). The class is `//? if fcgt` only
        // (no FA-pin gate) so it always compiles on fcgt variants; it self-detects whether EMF/FA are
        // actually present at runtime. Registered everywhere fcgt is on - run it in isolation with
        // `-Psmoke.fcgt.only=emf-fa -Pcompat=emf,etf,fa`.
        add("emf-fa" to "de.zannagh.armorhider.smoke.EmfFreshAnimationsSmokeTest")
    }

    // `runClientGametest` runs EVERY registered entrypoint in ONE client launch, so an unrelated
    // sibling failure reds the whole run. `-Psmoke.fcgt.only=a,b` narrows the registered set, which
    // is what makes a Paper E2E row report on its own merits instead of inheriting the health of
    // the render/water/gender tests (and makes it far faster - no world build, no screenshots).
    // Filtering here rather than self-skipping inside each test keeps the knowledge in one place.
    val fcgtOnly = findProperty("smoke.fcgt.only")?.toString()
        ?.split(",")?.map(String::trim)?.filter(String::isNotEmpty)?.toSet()
    if (fcgtOnly != null) {
        val known = fcgtTestCatalog.map { it.first }.toSet()
        val unknown = fcgtOnly - known
        // A typo would otherwise silently register nothing and "pass" - fail loudly instead.
        require(unknown.isEmpty()) {
            "-Psmoke.fcgt.only contains unknown test id(s) $unknown; known ids on " +
                "${sc.current.project}: $known"
        }
    }
    val fcgtTests = fcgtTestCatalog
        .filter { fcgtOnly == null || it.first in fcgtOnly }
        .map { it.second }
    val fcgtEntries = if (hasProperty("fabricapi.semver"))
        fcgtTests.joinToString(", ", "[", "]") { "\"$it\"" }
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
                runDirectory.set(layout.projectDirectory.dir("run"))
                displayName.set("Client GameTest")
                generateRunConfig.set(true)
                // FCGT activates via TWO properties (verified by decompiling the runner):
                //  - `fabric.client.gametest` (any value) → ClientGameTestMixinConfigPlugin
                //     applies the lifecycle/threading mixins that hand control to the runner.
                //  - `fabric.client.gametest.modid` → FabricClientGameTestRunner uses this to
                //     filter `fabric-client-gametest` entrypoints to dispatch. Without it,
                //     the mixins fire but no test class runs and MC sits at the title screen.
                jvmArguments.add("-Dfabric.client.gametest=true")
                jvmArguments.add("-Dfabric.client.gametest.modid=armor-hider")
                // Phase 1's exit timer would race FCGT's own shutdown - disable on this run.
                jvmArguments.add("-Darmorhider.smoke.exit=false")
                // The mod injects its payload types directly into the ClientboundCustomPayloadPacket
                // codec from a netty thread (ClientPacketSender / the codec-injection mixin), which
                // FCGT's NetworkSynchronizer detects as "interfacing with packets at a lower level"
                // and turns into a hard AssertionError the moment we connect to a real server.
                // FCGT names this property in that very error message. Required for any gametest
                // that joins a server - the codec injection is load-bearing and cannot be dropped.
                jvmArguments.add("-Dfabric.client.gametest.disableNetworkSynchronizer=true")

                // Opt-in JaCoCo coverage of the FCGT run (`-PfcgtCoverage`). The in-game tests exercise
                // client render code (render interceptors, compat mixins, …) that the Tier-1 unit JVM
                // never touches, so instrument this launch and emit an exec that fcgtCoverageReport
                // (root project) turns into an XML for Codecov's `fcgt` flag. Scoped to our own package
                // so we don't instrument (and slow) all of Minecraft. Off by default - it only matters
                // in the smoke/coverage CI job, and would otherwise add agent overhead to every dev run.
                if (project.hasProperty("fcgtCoverage")) {
                    // The `jacocoAgent` config resolves to the wrapper jar (no Premain-Class); the
                    // runnable agent is the `runtime`-classifier artifact, so resolve that explicitly.
                    val jacocoVersion = extensions
                        .getByType(org.gradle.testing.jacoco.plugins.JacocoPluginExtension::class.java).toolVersion
                    val agentJar = configurations
                        .detachedConfiguration(dependencies.create("org.jacoco:org.jacoco.agent:$jacocoVersion:runtime"))
                        .singleFile
                    val execFile = layout.buildDirectory.file("jacoco/fcgt.exec").get().asFile
                    jvmArguments.add("-javaagent:${agentJar.absolutePath}=destfile=${execFile.absolutePath}"
                            + ",append=false,includes=de.zannagh.armorhider.*")
                }
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
            // fetchFcgtCompatJars wipes run/mods first - make sure that runs before this copy.
            mustRunAfter("fetchFcgtCompatJars")
            // Never cache: pair task is also non-cached, and we want the FCGT jar to land
            // every time runClientGametest fires so cross-row leaks can't strand us with a
            // stale mods/ dir between BOOT and ENTITY_RENDER rows.
            outputs.upToDateWhen { false }
        }

        tasks.named("runClientGametest") {
            // NOT gated on -Psmoke. The FCGT module jar is what makes this task do anything at all:
            // without it on the runtime classpath fabric-loader never loads FCGT's mixin plugin, MC
            // boots vanilla, idles at the title screen and exits ZERO. The task therefore reports
            // success while running no test. Before this, `runClientGametest` only worked when a
            // previous -Psmoke run happened to have left the jar in run/mods - fabric-26.2 had that
            // leftover and every other variant did not, so the Paper E2E matrix "passed" on 26.2 and
            // silently ran nothing elsewhere.
            dependsOn(copyFcgtToMods)
        }
        if (project.hasProperty("smoke")) {
            // Compat-mod fetching stays smoke-only: it wipes run/mods and pulls the full
            // third-party stack, which is the compat matrix's concern, not FCGT's.
            tasks.named("runClientGametest") {
                dependsOn("fetchFcgtCompatJars")
                // Fresh Animations (issue #217) lands in run/resourcepacks/ when `fa` is in
                // -Pcompat; the reproduction test enables it at runtime. Separate dir from the
                // mod fetch, so ordering between the two is irrelevant.
                dependsOn("fetchFaResourcePack")
            }
        }
    }
}
