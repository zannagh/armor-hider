plugins {
    id("java")
    id("multiloader-common")
}

val sc = project.stonecutterBuild
sc.constants["fabric"] = sc.current.project.contains("fabric")
sc.constants["neoforge"] = sc.current.project.contains("neoforge")

// ── Smoke-test compat fetcher ────────────────────────────────────────────────
val compatKeys = listOf(
    "fabricapi",
    "gender", "geckolib", "waveycapes", "mekanism", "figura",
    "elytratrims", "iris", "emf", "etf", "modmenu", "deeperdarker", "uranus", "firstperson",
    "immersivearmors", "armoredelytra",
    // Nycto (MoriyaShiine) - registers its vampire/hunter armor through Fabric API's ArmorRenderer, the
    // reproduction case for the fabric-rendering-v1 armor compat (issue #348). Fabric-only, and only
    // fetched on the variants that pin nycto.version.
    "nycto",
    // Fresh Animations (issue #217). Not a mod - a resource pack fetched into run/resourcepacks/
    // by fetchFaResourcePack, not run/mods/. Requires emf + etf to actually animate.
    "fa",
    // Fresh Animations: Player Extension (the add-on that actually animates the player model).
    "faplayer",
    // Accessory providers (issue #246). trinkets + accessories are Fabric; curios is NeoForge-only.
    "trinkets", "accessories", "curios"
)
val availableHashes = compatKeys.mapNotNull { key ->
    findProperty("$key.version")?.toString()?.let { hash -> key to hash }
}.toMap()
val compatSel = (findProperty("compat")?.toString() ?: "all").trim()
val selectedKeys: Set<String> = when (compatSel.lowercase()) {
    "all" -> availableHashes.keys
    "none", "clean" -> emptySet()
    else -> compatSel.split(",").map { it.trim() }.toSet()
}
val activeMcVersion: String? = listOf("fabric.minecraft_version", "neoforge.minecraft_version")
    .firstNotNullOfOrNull { findProperty(it)?.toString() }
    ?.substringBefore("-pre")?.substringBefore("-rc")?.substringBefore("-alpha")
// Keys that resolve to a mod jar (run/mods). "fa" (Fresh Animations) and "faplayer" (its Player
// Extension) are resource packs handled separately by fetchFaResourcePack into run/resourcepacks,
// so they must never be dropped into run/mods even when listed in -Pcompat.
val modHashes = availableHashes
    .filterKeys { it != "fa" && it != "faplayer" }
    // On 1.20.1, Iris pulls a Sodium whose EarlyDriverScanner rejects loom's dev-runtime LWJGL
    // (caffeine gh-2561), hard-failing the boot - and 1.20.1 is not an FCGT variant, so iris/sodium
    // exercise nothing here. Drop iris from the FETCH only (the compileOnly stays, so IrisCompat still
    // builds); it detects iris absent at runtime and no-ops. Real launchers ship a matching LWJGL.
    .filterKeys { !(it == "iris" && activeMcVersion == "1.20.1") }
val activeLoader: String? = when {
    sc.current.project.contains("fabric") -> "fabric"
    sc.current.project.contains("neoforge") -> "neoforge"
    else -> null
}
// CurseForge pins - a keyless Cursemaven fallback for compat mods not (yet) on Modrinth. Per-variant
// `<key>.cf.project` + `<key>.cf.file` -> "<projectId>:<fileId>"; FetchCompatJars uses a pin only for a
// selected key with no Modrinth hash. The variant sections are already loader-specific, so `<key>.cf.file`
// is the fabric-or-neoforge file for this exact variant. Empty today (every compat mod resolves from
// Modrinth); this lets a CF-only compat mod be added by pinning those two properties in its section.
val curseForgeModPins: Map<String, String> = modHashes.keys
    .plus(compatKeys)
    .distinct()
    .mapNotNull { key ->
        val proj = findProperty("$key.cf.project")?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val file = findProperty("$key.cf.file")?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        key to "$proj:$file"
    }.toMap()

val fetchCompatJars = tasks.register<FetchCompatJars>("fetchCompatJars") {
    group = "verification"
    description = "Fetch Modrinth + CurseForge compat jars (controlled by -Pcompat) into run/mods/ for smoke runs"
    modsDir.set(project.layout.projectDirectory.dir("run/mods"))
    versionHashes.set(modHashes)
    curseForgePins.set(curseForgeModPins)
    include.set(selectedKeys)
    activeMcVersion?.let { mcGameVersion.set(it) }
    activeLoader?.let { loader.set(it) }
    // Never cache - the action wipes run/mods/ before populating it. If Gradle skips us
    // on a transitively-cached call, a prior smoke row's mods can leak into the next.
    outputs.upToDateWhen { false }
}

val fetchFcgtCompatJars = tasks.register<FetchCompatJars>("fetchFcgtCompatJars") {
    group = "verification"
    description = "Like fetchCompatJars but always includes fabric-api (required for FCGT runtime activation)"
    modsDir.set(project.layout.projectDirectory.dir("run/mods"))
    versionHashes.set(modHashes)
    curseForgePins.set(curseForgeModPins)
    if (modHashes.containsKey("fabricapi")) {
        include.set(selectedKeys + "fabricapi")
    } else {
        include.set(selectedKeys)
    }
    activeMcVersion?.let { mcGameVersion.set(it) }
    activeLoader?.let { loader.set(it) }
    // Same caching note as fetchCompatJars - never up-to-date.
    outputs.upToDateWhen { false }
}
// Fresh Animations resource pack (issue #217). Dropped into run/resourcepacks/ - NOT run/mods/ -
// and enabled at runtime by the reproduction FCGT test. Dependency-following is off so FA's
// required emf/etf deps aren't pulled into the pack dir (they go into run/mods/ via the mod fetch).
val fetchFaResourcePack = tasks.register<FetchCompatJars>("fetchFaResourcePack") {
    group = "verification"
    description = "Fetch the Fresh Animations resource pack into run/resourcepacks/ for smoke runs"
    modsDir.set(project.layout.projectDirectory.dir("run/resourcepacks"))
    versionHashes.set(availableHashes.filterKeys { it == "fa" || it == "faplayer" })
    include.set(selectedKeys)
    followDependencies.set(false)
    activeMcVersion?.let { mcGameVersion.set(it) }
    activeLoader?.let { loader.set(it) }
    outputs.upToDateWhen { false }
}

val commonNode = sc.node.sibling("common")
    ?: error("Could not find common branch sibling for ${sc.current.project}")
val commonPath = commonNode.hierarchy.toString()

// Ensure common project is fully evaluated before accessing its source sets
evaluationDependsOn(commonPath)

val commonProject = project(commonPath)
val commonSourceSets = commonProject.extensions.getByType(SourceSetContainer::class.java)

// Expose common source sets and project for loader build scripts that need additional wiring
extra["commonSourceSets"] = commonSourceSets
extra["commonProject"] = commonProject

// Carry over compile-only dependencies from common that are needed when compiling common sources
dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("net.luckperms:api:5.4")
}

// eunomia-core, unremapped - the loader recompiles common's sources, so it needs the same
// version-agnostic API on its compile classpath (see the compileOnly in multiloader-loom).
addCompileOnlyDependency("eunomia.version", "de.zannagh.eunomia:eunomia-core")
addCompileOnlyDependency("geckolib.version", "maven.modrinth:geckolib")
addCompileOnlyDependency("iris.version", "maven.modrinth:iris")
addCompileOnlyDependency("emf.version", "maven.modrinth:entity-model-features")
addCompileOnlyDependency("etf.version", "maven.modrinth:entitytexturefeatures")
addCompileOnlyDependency("mekanism.version", "maven.modrinth:mekanism")
addCompileOnlyDependency("waveycapes.version", "maven.modrinth:wavey-capes")
addCompileOnlyDependency("deeperdarker.version", "maven.modrinth:deeperdarker")
addCompileOnlyDependency("uranus.version", "maven.modrinth:uranus")
addCompileOnlyDependency("figura.version", "maven.modrinth:figura")
addCompileOnlyDependency("gender.version", "maven.modrinth:female-gender")
// Accessory providers (issue #246). Fabric: trinkets + accessories; NeoForge: curios (added on the
// neoforge project). Compat is @Pseudo/@Coerce so these are compileOnly parity deps + smoke-fetch sources.
addCompileOnlyDependency("trinkets.version", "maven.modrinth:trinkets")
addCompileOnlyDependency("accessories.version", "maven.modrinth:accessories")
addCompileOnlyDependency("curios.version", "maven.modrinth:curios")
// First Person Model is Fabric-only, but the loader project compiles common's sources too, so the
// unremapped jar has to be here as well. That is usable only because FirstPersonCompat never touches an
// FPM member whose signature names a Minecraft type - FPM's own types (LogicHandler and friends) are
// fine, since those resolve identically either way; it is the MC types that differ between namespaces.
addCompileOnlyDependency("firstperson.version", "maven.modrinth:first-person-model")

// Include common's sources in the loader's source sets for IntelliJ
sourceSets.main {
    java { commonSourceSets["main"].java.srcDirs.forEach { srcDir(it) } }
    resources { commonSourceSets["main"].resources.srcDirs.forEach { srcDir(it) } }
}

// Source sets to be available in loader specific projects
sourceSets.matching { it.name == "client" }.configureEach {
    java { commonSourceSets["client"].java.srcDirs.forEach { srcDir(it) } }
    resources { commonSourceSets["client"].resources.srcDirs.forEach { srcDir(it) } }
}

// Declare dependency on common's Stonecutter generation tasks so sources are ready
val commonStonecutterGenerate = commonProject.tasks.named("stonecutterGenerate")
val commonStonecutterGenerateClient = commonProject.tasks.named("stonecutterGenerateClient")

// All tasks that consume common's source/resource dirs must depend on Stonecutter generation
val commonStonecutterTasks = listOf(commonStonecutterGenerate, commonStonecutterGenerateClient)

tasks {
    compileJava { dependsOn(commonStonecutterTasks) }
    processResources { dependsOn(commonStonecutterTasks) }
    named("sourcesJar") { dependsOn(commonStonecutterTasks) }

    // When a client source set exists, its tasks also need common's Stonecutter output
    matching { it.name in listOf("compileClientJava", "processClientResources") }.configureEach {
        dependsOn(commonStonecutterTasks)
    }

    jar {
        inputs.property("archivesName", base.archivesName)
    }
}
