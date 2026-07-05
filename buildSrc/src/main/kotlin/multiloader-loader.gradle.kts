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
    "elytratrims", "iris", "emf", "etf", "modmenu"
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
val activeLoader: String? = when {
    sc.current.project.contains("fabric") -> "fabric"
    sc.current.project.contains("neoforge") -> "neoforge"
    else -> null
}

val fetchCompatJars = tasks.register<FetchCompatJars>("fetchCompatJars") {
    group = "verification"
    description = "Fetch Modrinth compat jars (controlled by -Pcompat) into run/mods/ for smoke runs"
    modsDir.set(project.layout.projectDirectory.dir("run/mods"))
    versionHashes.set(availableHashes)
    include.set(selectedKeys)
    activeMcVersion?.let { mcGameVersion.set(it) }
    activeLoader?.let { loader.set(it) }
    // Never cache — the action wipes run/mods/ before populating it. If Gradle skips us
    // on a transitively-cached call, a prior smoke row's mods can leak into the next.
    outputs.upToDateWhen { false }
}

val fetchFcgtCompatJars = tasks.register<FetchCompatJars>("fetchFcgtCompatJars") {
    group = "verification"
    description = "Like fetchCompatJars but always includes fabric-api (required for FCGT runtime activation)"
    modsDir.set(project.layout.projectDirectory.dir("run/mods"))
    versionHashes.set(availableHashes)
    if (availableHashes.containsKey("fabricapi")) {
        include.set(selectedKeys + "fabricapi")
    } else {
        include.set(selectedKeys)
    }
    activeMcVersion?.let { mcGameVersion.set(it) }
    activeLoader?.let { loader.set(it) }
    // Same caching note as fetchCompatJars — never up-to-date.
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
    if (hasProperty("geckolib.version")) {
        add("compileOnly", "maven.modrinth:geckolib:${findProperty("geckolib.version")}")
    }
    if (hasProperty("iris.version")) {
        add("compileOnly", "maven.modrinth:iris:${findProperty("iris.version")}")
    }
    if (hasProperty("emf.version")) {
        add("compileOnly", "maven.modrinth:entity-model-features:${findProperty("emf.version")}")
    }
    if (hasProperty("etf.version")) {
        add("compileOnly", "maven.modrinth:entitytexturefeatures:${findProperty("etf.version")}")
    }
    if (hasProperty("mekanism.version")) {
        add("compileOnly", "maven.modrinth:mekanism:${findProperty("mekanism.version")}")
    }
    if (hasProperty("waveycapes.version")) {
        add("compileOnly", "maven.modrinth:wavey-capes:${findProperty("waveycapes.version")}")
    }
    if (hasProperty("figura.version")) {
        add("compileOnly", "maven.modrinth:figura:${findProperty("figura.version")}")
    }
    if (hasProperty("gender.version")) {
        add("compileOnly", "maven.modrinth:female-gender:${findProperty("gender.version")}")
    }
}

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
