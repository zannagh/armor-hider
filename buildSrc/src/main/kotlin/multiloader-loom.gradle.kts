val isDeobf = extra.has("loom.deobf") && extra.get("loom.deobf") as Boolean
val sc = project.stonecutterBuild
val branch = sc.branch.id
val mcVersion = sc.current.project.substringAfter('-')

// ── Base setup ──
if (branch == "common") {
    plugins.apply("multiloader-common")
} else {
    plugins.apply("multiloader-loader")
}

// ── Loom ──
if (isDeobf) {
    extra.set("fabric.loom.disableObfuscation", "true")
}
plugins.apply("fabric-loom")

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
    // Cursemaven (https://cursemaven.com) - keyless CurseForge proxy for CF-hosted mod jars declared as
    // `curse.maven:<slug>-<projectId>:<fileId>`. Backs the eunomia runtime mod jar (project 1654849) the
    // FCGT smoke run drops into run/mods. Group-scoped so it only handles `curse.maven` coordinates.
    maven("https://cursemaven.com") {
        content { includeGroup("curse.maven") }
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

    // Remapped (production) vs. deobfuscated builds want different configuration names for the mod
    // compat deps: modCompileOnly/modClientCompileOnly go through Loom's remapper, the deobf variants
    // (compileOnly/clientCompileOnly) skip it. Pick once, reuse for every version-gated dep below.
    val modDep = if (isDeobf) "compileOnly" else "modCompileOnly"
    val modClientDep = if (isDeobf) "clientCompileOnly" else "modClientCompileOnly"

    dependencies {
        if (!isDeobf) {
            add("modCompileOnly", "net.fabricmc:fabric-loader:${property("loader_version")}")
        }
        // Phase 2 smoke: FCGT (fabric-client-gametest-api-v1) compile-time dep on common.
        if (sc.current.project.contains("fabric") && hasProperty("fabricapi.semver")) {
            val fabricApiSemver = findProperty("fabricapi.semver")!!.toString()
            val fabricApi = project.extensions.getByType(net.fabricmc.loom.api.fabricapi.FabricApiExtension::class.java)
            add(modClientDep, fabricApi.module("fabric-client-gametest-api-v1", fabricApiSemver))
        }
        add("compileOnly", "net.luckperms:api:5.4")
        add("compileOnly", "org.jspecify:jspecify:1.0.0")
        // eunomia-core: the MC-free, version-agnostic API surface (CommunicationManager, PacketType,
        // the transport interfaces). Plain compileOnly - it is a normal Java library, NOT a remapped
        // mod jar, so it never goes through loom. One coordinate resolves on every variant because the
        // core artifact carries no MC version. The runtime implementation ships in the eunomia mod
        // (declared as a required dependency in fabric.mod.json / neoforge.mods.toml), so eunomia is
        // never bundled here. Mirrored unremapped in multiloader-loader for the loader compile.
        if (hasProperty("eunomia.version")) {
            add("compileOnly", "de.zannagh.eunomia:eunomia-core:${findProperty("eunomia.version")}")
            // eunomia-core is compileOnly for the mod (the eunomia mod supplies it at game runtime), but
            // the JUnit tests load the config POJOs in a plain JVM and those implement eunomia's
            // NetworkHealable / encode via its PayloadCodec - and the HTTP/WebSocket fallback E2E
            // (HttpFallbackE2ETest) drives eunomia's ExternalServerClient / ReplicatedClientStore /
            // ReplicatedPlayerConfigStore directly - so the classes must be on the test COMPILE classpath,
            // not just runtime. Test scope only - never bundled into the shipped mod jar.
            add("testImplementation", "de.zannagh.eunomia:eunomia-core:${findProperty("eunomia.version")}")
        }
        add("testImplementation", platform("org.junit:junit-bom:6.0.1"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        // The fallback E2E's embedded stub relay needs to serve HTTP (/health, PUT /api/packets/*) AND a
        // WebSocket (/ws) on the SAME port, because eunomia's ExternalServerClient derives the ws URL from
        // the same base host:port as its REST calls. NanoHTTPD-websocket (NanoWSD) does exactly that in one
        // tiny, dependency-free server. Test scope only; the real relay is the C# server. Gated at runtime,
        // so a normal `./gradlew test` never opens a socket - it just needs the class on the test classpath.
        add("testImplementation", "org.nanohttpd:nanohttpd-websocket:2.3.1")
        // eunomia-core logs via slf4j-api, which it declares compileOnly (the game/Paper supply a binding at
        // runtime), so it is not transitive onto the plain-JVM test classpath. The fallback E2E constructs
        // eunomia's ExternalServerClient (which takes an slf4j Logger), so the API + a simple binding are
        // needed for the test JVM only.
        add("testImplementation", "org.slf4j:slf4j-api:2.0.16")
        add("testRuntimeOnly", "org.slf4j:slf4j-simple:2.0.16")
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

    // Version-gated compat deps: each drops out on MC variants that don't pin the property. geckolib
    // lands on the common (server) config as well as client; everything else is client-only.
    addDependency(modDep, "geckolib.version", "maven.modrinth:geckolib")
    addDependency(modClientDep, "geckolib.version", "maven.modrinth:geckolib")
    addDependency(modDep, "elytratrims.version", "maven.modrinth:elytra-trims")
    addDependency(modClientDep, "iris.version", "maven.modrinth:iris")
    addDependency(modClientDep, "emf.version", "maven.modrinth:entity-model-features")
    addDependency(modClientDep, "etf.version", "maven.modrinth:entitytexturefeatures")
    addDependency(modClientDep, "mekanism.version", "maven.modrinth:mekanism")
    addDependency(modClientDep, "waveycapes.version", "maven.modrinth:wavey-capes")
    addDependency(modClientDep, "deeperdarker.version", "maven.modrinth:deeperdarker")
    addDependency(modClientDep, "uranus.version", "maven.modrinth:uranus")
    addDependency(modClientDep, "figura.version", "maven.modrinth:figura")
    addDependency(modClientDep, "modmenu.version", "maven.modrinth:modmenu")
    addDependency(modClientDep, "gender.version", "maven.modrinth:female-gender")
    // Accessory providers (issue #246), Fabric side. Curios is NeoForge-only (added on the neoforge
    // project). Compat is @Pseudo/@Coerce, so these are compile-only parity deps + smoke-fetch sources.
    addDependency(modClientDep, "trinkets.version", "maven.modrinth:trinkets")
    addDependency(modClientDep, "accessories.version", "maven.modrinth:accessories")
    // Fabric-only. Declared here for the remapped common compile; the loader project compiles common's
    // sources too, so multiloader-loader.gradle.kts declares the same coordinate unremapped. That pairing
    // only works because FirstPersonCompat avoids every FPM member whose signature names a Minecraft type.
    addDependency(modClientDep, "firstperson.version", "maven.modrinth:first-person-model")

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
    // Defaults to ArmorHiderSmoke (the single-client handshake row); overridable so a multi-client row
    // (e.g. the two-client config-propagation E2E) can fork the same variant twice under distinct
    // identities. The offline UUID is derived from the name, matching Paper's offline-mode hashing.
    val paperSmokeUsername = findProperty("smoke.paper.username")?.toString() ?: "ArmorHiderSmoke"
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
            // Two-client config-propagation E2E (TwoClientConfigPropagationSmokeTest): the PaperE2E row
            // forks the same variant twice, once as the sender and once as the reader, forwarding the
            // role, the peer's name and the marker opacity. Absent on every normal run - the test no-ops.
            listOf("role", "peer", "marker").forEach { key ->
                findProperty("smoke.twoclient.$key")?.toString()?.let {
                    jvmArguments.add("-Darmorhider.smoke.twoclient.$key=$it")
                }
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

    // Mod Menu: loader-side compile dep on the base config, remapped or deobf per isDeobf.
    addDependency(if (isDeobf) "compileOnly" else "modCompileOnly", "modmenu.version", "maven.modrinth:modmenu")

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
        // Public ArmorHiderRenderApi end-to-end smoke. Asserts on SlotModification + the translucent
        // armor path rather than on a version-specific render architecture, so it is `//? if fcgt`
        // only and registers on every fcgt variant.
        add("render-api" to "de.zannagh.armorhider.smoke.RenderApiSmokeTest")
        // Paper end-to-end handshake smoke. Gated only on `fcgt` like the class itself: it no-ops
        // unless -Psmoke.paper.port is supplied, so registering it everywhere is harmless.
        add("paper-handshake" to "de.zannagh.armorhider.smoke.PaperHandshakeSmokeTest")
        // Two-client config-propagation E2E: one client changes its config, a second observes it via the
        // server. Role-dispatched by -Darmorhider.smoke.twoclient.role; no-ops unless a Paper port is set.
        add("two-client-propagation" to "de.zannagh.armorhider.smoke.TwoClientConfigPropagationSmokeTest")
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
        // Fabric API ArmorRenderer compat repro (issue #348). `//? if fcgt` only - it searches the item
        // registry for whatever item has a custom ArmorRenderer registered and self-skips when the run
        // has none, so it is safe to register on every fcgt variant. Nycto supplies one on the rows that
        // pin nycto.version; run it in isolation with
        // `-Psmoke.fcgt.only=fabric-armor-renderer -Pcompat=fabricapi,nycto`.
        add("fabric-armor-renderer" to "de.zannagh.armorhider.smoke.FabricArmorRendererSmokeTest")
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
        from(rootProject.file("common/accesswideners"), Action {
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
                // Keep the gametest window from stealing focus on macOS (it otherwise pops to the
                // foreground and kicks the developer out of any fullscreen app every FCGT loop). The
                // GLFW era (26.1.2 / 26.2) is handled by WindowFocusMixin, which sets GLFW's focus hints
                // before window creation (the process-level -Dapple.awt.UIElement hint does NOT work -
                // GLFW forces its own Regular activation policy). 26.3 uses SDL, which reads these hint
                // env vars before creating the window; "0" tells it not to activate/raise-to-front on
                // show. Harmless off macOS / when the backend isn't in use.
                environmentVars.put("SDL_WINDOW_ACTIVATE_WHEN_SHOWN", "0")
                environmentVars.put("SDL_WINDOW_ACTIVATE_WHEN_RAISED", "0")

                // ── E2E line coverage (opt-in: -Psmoke.coverage) ─────────────────────────────
                // The FCGT client is a real, mod-loaded Minecraft JVM, so it exercises code the Tier-1
                // unit tests never can - the render pipeline (RenderModifications, AhRenderManagementApi,
                // the feature-phase interceptors) and the mod's client/config/net logic.
                //
                // On-the-fly JaCoCo (-javaagent) CANNOT see these: Fabric's KnotClassLoader loads the mod
                // (and Minecraft) classes through a path the agent's ClassFileLoadHook never covers, so the
                // recorded .exec contains 3500+ library classes but ZERO de.zannagh.armorhider ones
                // (verified empirically). The reliable route is OFFLINE instrumentation: the class files on
                // disk are pre-instrumented, so Knot loads already-probed bytecode and no runtime transform
                // is needed. The runtime then only needs the JaCoCo RT jar on the classpath and a destfile.
                // See offlineInstrumentForCoverage below. The mixin package is deliberately NOT instrumented
                // (Mixin reads raw class bytes to apply them - probes would corrupt that - and @Inject
                // handlers execute on the vanilla target, so they are not attributable anyway).
                if (project.hasProperty("smoke.coverage")) {
                    val execFile = project.rootProject.file("build/jacoco/e2e-client.exec")
                    jvmArguments.add("-Djacoco-agent.destfile=${execFile.absolutePath}")
                    // append=true so per-scenario launches of one run accumulate; CI wipes build/jacoco first.
                    jvmArguments.add("-Djacoco-agent.append=true")
                }
            }
        }
        // ── JaCoCo OFFLINE instrumentation for E2E coverage (opt-in: -Psmoke.coverage) ──────────
        if (project.hasProperty("smoke.coverage")) {
            val jacocoVersion = (findProperty("jacoco.version")?.toString()) ?: "0.8.14"
            // Offline-instrumented classes carry a hard dependency on the JaCoCo runtime, so the RT jar
            // must be on the game JVM classpath. runtimeOnly puts it on the mod runtime classpath; Knot
            // delegates the non-mod org.jacoco.agent.rt.* package to its parent loader, which has it.
            dependencies.add("runtimeOnly", "org.jacoco:org.jacoco.agent:$jacocoVersion:runtime")
            // Standalone JaCoCo CLI (nodeps) + its runtime deps, used to instrument class files offline.
            val jacocoCli = configurations.create("ahJacocoCli") {
                isCanBeResolved = true
                isCanBeConsumed = false
            }
            dependencies.add("ahJacocoCli", "org.jacoco:org.jacoco.cli:$jacocoVersion:nodeps")
            dependencies.add("ahJacocoCli", "org.jacoco:org.jacoco.core:$jacocoVersion")
            dependencies.add("ahJacocoCli", "org.jacoco:org.jacoco.report:$jacocoVersion")

            val ssc = project.extensions.getByType(SourceSetContainer::class.java)
            val classOutputs: List<File> = listOf("main", "client")
                .mapNotNull { ssc.findByName(it) }
                .flatMap { it.output.classesDirs.files.toList() }
            val backupRoot = project.rootProject.file("build/jacoco/classes-orig")
            val pristineRoot = project.rootProject.file("build/jacoco/classes-pristine")
            val cliCfg = jacocoCli

            // Instrument the client+main class outputs IN PLACE so Knot loads probed bytecode (on-the-fly
            // can't reach Knot-loaded classes). Idempotent across the two forked compat rows of one variant:
            // a pristine copy is kept and restored before each instrument pass, so re-running never double-
            // instruments. Originals are also mirrored to classes-orig for e2eCoverage to analyse the clean
            // bytecode. Strictly opt-in (-Psmoke.coverage), so a normal build/jar is never instrumented.
            val instrumentTask = tasks.register("offlineInstrumentForCoverage") {
                group = "verification"
                description = "JaCoCo offline-instrument the FCGT client classes in place (-Psmoke.coverage)"
                dependsOn("classes", "clientClasses")
                outputs.upToDateWhen { false }
                doLast {
                    backupRoot.deleteRecursively(); backupRoot.mkdirs()
                    pristineRoot.mkdirs()
                    classOutputs.forEachIndexed { idx, dir ->
                        if (!dir.isDirectory) {
                            return@forEachIndexed
                        }
                        val pristine = File(pristineRoot, "$idx-${dir.name}")
                        if (pristine.isDirectory) {
                            // A previous row already instrumented this dir - restore clean bytes first.
                            dir.deleteRecursively()
                            pristine.copyRecursively(dir, overwrite = true)
                        } else {
                            dir.copyRecursively(pristine, overwrite = true)
                        }
                        // Mirror the clean bytes for the report.
                        pristine.copyRecursively(File(backupRoot, "$idx-${dir.name}"), overwrite = true)

                        val instrDir = File(dir.parentFile, "${dir.name}-ahInstr")
                        instrDir.deleteRecursively()
                        // Run the JaCoCo CLI out-of-process (Gradle 9 removed Project.javaexec, and the CLI
                        // needs no Gradle wiring). java from the build JVM; the CLI runs on any recent JDK.
                        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
                        val cliClasspath = cliCfg.files.joinToString(File.pathSeparator) { it.absolutePath }
                        val process = ProcessBuilder(
                            javaBin, "-cp", cliClasspath, "org.jacoco.cli.internal.Main",
                            "instrument", dir.absolutePath, "--dest", instrDir.absolutePath
                        ).redirectErrorStream(true).start()
                        val cliOut = process.inputStream.bufferedReader().readText()
                        val code = process.waitFor()
                        if (code != 0) {
                            throw GradleException(
                                "JaCoCo offline instrumentation failed (exit $code):\n$cliOut")
                        }
                        // Copy probed classes back over the originals, EXCEPT the mixin package (keep raw
                        // bytes there so Mixin can still apply them and @Inject handlers are not miscounted).
                        instrDir.walkTopDown().filter { it.isFile }.forEach { src ->
                            // Normalize separators once so the mixin-package exclusion holds on Windows too.
                            val rel = src.relativeTo(instrDir).path.replace('\\', '/')
                            if (!rel.contains("/mixin/") && !rel.startsWith("mixin/")) {
                                val dest = File(dir, rel)
                                dest.parentFile?.mkdirs()
                                src.copyTo(dest, overwrite = true)
                            }
                        }
                        instrDir.deleteRecursively()
                    }
                }
            }
            tasks.named("runClientGametest") { dependsOn(instrumentTask) }
        }
        // Resolve the FCGT module artifact via a dedicated configuration so we can copy the
        // resolved (already named-mapped) jar into run/mods. fabric-api's umbrella jar
        // doesn't include FCGT (it's marked experimental upstream), so this is the only path
        // that actually puts the module classes on fabric-loader's runtime classpath.
        val fcgtRuntimeMod = configurations.create("fcgtRuntimeMod") {
            isCanBeResolved = true
            isCanBeConsumed = false
        }
        val fabricApiExt = project.extensions.getByType(net.fabricmc.loom.api.fabricapi.FabricApiExtension::class.java)
        val fabricApiSemver = findProperty("fabricapi.semver")!!.toString()
        dependencies.add(
            "fcgtRuntimeMod",
            fabricApiExt.module("fabric-client-gametest-api-v1", fabricApiSemver)
        )
        // FCGT 6.x (26.3+) hard-depends on fabric-resource-loader-v1, but that's a runtime (fabric.mod.json)
        // dependency, not a Gradle-transitive one, so copying only the FCGT module leaves it missing and the
        // client aborts at boot ("requires fabric-resource-loader-v1, which is missing"). Older nodes only
        // booted because a prior -Psmoke run happened to leave the full fabric-api umbrella (which bundles
        // it) in run/mods. Provision it explicitly so FCGT boots on a clean run/mods where it's needed.
        // Fabric-loader deduplicates it against any umbrella-bundled copy, so this is safe where one exists.
        // fabricApiExt.module resolves the submodule version from the pinned fabric-api's module list, so on
        // older fabric-api lines (1.21.4..1.21.11) that predate the resource-loader-v1 module it throws
        // "Failed to find module version" - those run FCGT 5.x, which doesn't need it, so just skip there.
        runCatching {
            dependencies.add(
                "fcgtRuntimeMod",
                fabricApiExt.module("fabric-resource-loader-v1", fabricApiSemver)
            )
        }
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

        // armor-hider now consumes eunomia-core at compile time only; the eunomia MOD supplies the
        // networking transports + codec injection + capability handshake at game runtime and is a
        // REQUIRED dependency (fabric.mod.json). So every FCGT client launch must have the eunomia
        // fabric mod jar in run/mods, or armor-hider fails its dependency, MC boots vanilla and the
        // gametest exits ZERO having run nothing (a false green - blueprint risk R4).
        //
        // The jar is pulled from CurseForge (project 1654849) via Cursemaven by default, resolved for this
        // variant's MC version from the pinned file id `eunomia.cf.file`. Pass -Peunomia.fabric.jar=<path>
        // to smoke-test a locally-built eunomia instead (e.g. an unreleased change). The CF configuration is
        // non-transitive, so only eunomia's own jar lands - never its CF-declared deps.
        //
        // Clear any previously-copied eunomia jar before copying the current one. Without this, a filename
        // change - an eunomia version bump changes the CurseForge file id (and thus the jar name), or a run
        // switches between the CF jar and a -Peunomia.fabric.jar override - leaves TWO eunomia mods in
        // run/mods. fabric-loader then loads both, the codec-injection mixins apply twice and the handshake
        // S2C payloads fail to decode (the client disconnects at join). fetchFcgtCompatJars wipes run/mods on
        // -Psmoke runs, but the FCGT/E2E path does not run it, so this copy must clean up after itself.
        fun deleteStaleEunomiaJars() {
            delete(fileTree(project.layout.projectDirectory.dir("run/mods")) { include("eunomia*.jar") })
        }
        val eunomiaOverrideJar = findProperty("eunomia.fabric.jar")?.toString()
        val copyEunomiaToMods = if (eunomiaOverrideJar != null) {
            tasks.register<Copy>("copyEunomiaToMods") {
                group = "verification"
                description = "Drop the local eunomia fabric mod jar into run/mods/ (armor-hider's required runtime dependency)."
                from(eunomiaOverrideJar)
                into(project.layout.projectDirectory.dir("run/mods"))
                // Both wipe run/mods first: fetchFcgtCompatJars on the runClientGametest (ENTITY_RENDER)
                // path, fetchCompatJars on the runClient (BOOT) path. Land after whichever is in the graph,
                // or a wipe deletes the eunomia jar we just dropped and the client fails its required dep.
                mustRunAfter("fetchFcgtCompatJars", "fetchCompatJars")
                outputs.upToDateWhen { false }
                doFirst {
                    if (!file(eunomiaOverrideJar).exists()) {
                        throw GradleException(
                            "eunomia fabric mod jar (-Peunomia.fabric.jar) not found at:\n  $eunomiaOverrideJar"
                        )
                    }
                    deleteStaleEunomiaJars()
                }
            }
        } else {
            val cfProject = findProperty("eunomia.cf.project")?.toString()
                ?: error("eunomia.cf.project is not set; cannot resolve the eunomia mod jar from CurseForge")
            val cfFile = findProperty("eunomia.cf.file")?.toString()
                ?: error(
                    "eunomia.cf.file is not set for ${sc.current.project}; pin the CurseForge fabric file id " +
                        "(https://www.curseforge.com/minecraft/mc-mods/eunomia/files/all) in that variant's " +
                        "section of stonecutter.properties.toml, or pass -Peunomia.fabric.jar=<path>."
                )
            val eunomiaRuntimeMod = configurations.create("eunomiaRuntimeMod") {
                isCanBeResolved = true
                isCanBeConsumed = false
                isVisible = false
                isTransitive = false
            }
            dependencies.add("eunomiaRuntimeMod", "curse.maven:eunomia-$cfProject:$cfFile")
            tasks.register<Copy>("copyEunomiaToMods") {
                group = "verification"
                description = "Drop the eunomia fabric mod jar (CurseForge $cfProject/$cfFile) into run/mods/."
                from(eunomiaRuntimeMod)
                into(project.layout.projectDirectory.dir("run/mods"))
                // Both wipe run/mods first: fetchFcgtCompatJars on the runClientGametest (ENTITY_RENDER)
                // path, fetchCompatJars on the runClient (BOOT) path. Land after whichever is in the graph,
                // or a wipe deletes the eunomia jar we just dropped and the client fails its required dep.
                mustRunAfter("fetchFcgtCompatJars", "fetchCompatJars")
                outputs.upToDateWhen { false }
                doFirst { deleteStaleEunomiaJars() }
            }
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
            // Same unconditional guarantee for the eunomia runtime dependency (see above).
            dependsOn(copyEunomiaToMods)
        }
        // A plain `runClient` boot (the smoke BOOT phase, and any dev client launch) also needs the
        // eunomia mod in run/mods - armor-hider hard-requires it, so without this the client fails mod
        // resolution and never boots. Only runClientGametest was wired before, so the BOOT smoke rows
        // (and dev runClient) had no eunomia. The FCGT jar is NOT needed for a plain boot, so only the
        // eunomia copy is added here.
        tasks.named("runClient") {
            dependsOn(copyEunomiaToMods)
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
