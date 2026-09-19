plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val sc = project.stonecutterBuild

val neoforgeVersion = findProperty("neoforge.version")?.toString()
    ?: error("No neoforge.version for ${sc.current.project}")
val neoforgeVersionRange = findProperty("neoforge.minecraft_version_range")?.toString()
    ?: error("No neoforge.minecraft_version_range for ${sc.current.project}")

val javaVersion = findProperty("java.version")?.toString()
    ?: error("No java.version for ${sc.current.project}")

val clientSourceSet = sourceSets.create("client") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

// NeoForge doesn't split environments - main has the full MC jar, so common client sources
// must also be in main for NeoForge-specific code that references common client classes.
val commonSourceSets = extra["commonSourceSets"] as SourceSetContainer
sourceSets.main {
    java { commonSourceSets["client"].java.srcDirs.forEach { srcDir(it) } }
    resources { commonSourceSets["client"].resources.srcDirs.forEach { srcDir(it) } }
}

stonecutter {
    constants["neoforge"] = true
}

val expandResourcesForIdea = registerExpandResourcesForIdea(
    tasks.named<ProcessResources>("processResources") to "out/production/resources"
)
// Ensure Gradle fully compiles before IntelliJ runs - IntelliJ's "Make" doesn't trigger
// Stonecutter generation, so without this, generated sources can be stale.
expandResourcesForIdea.configure { dependsOn(tasks.classes, tasks.named("clientClasses")) }
patchIdeRunConfigsAllowParallel()

val requestedTasks = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val devProfile = if (!gradle.startParameter.isOffline && requestedTasks.any {
    it.equals("runClient", ignoreCase = true) || it.equals("client", ignoreCase = true)
}) loadDevProfile() else null

// eunomia is a REQUIRED runtime dependency (neoforge.mods.toml): the eunomia mod supplies the
// networking transports + codec injection + capability handshake at game runtime, while armor-hider
// only compiles against eunomia-core. So every client run must have the eunomia NeoForge mod jar in
// run/mods, or armor-hider fails its dependency and the client aborts at boot. Resolved from CurseForge
// (project `eunomia.cf.project`) for this variant's MC version via the pinned file id `eunomia.cf.file`.
// This mirrors the Fabric copyEunomiaToMods in multiloader-loom.gradle.kts. The gameDir for a run is the
// variant's `run/` dir, so run/mods is where NeoForge/FML loads it from - the same dir fetchCompatJars uses.
// Registered only when the variant pins `eunomia.cf.file` (all current NeoForge variants do). Lenient by
// design: an unpinned variant simply gets no copy task rather than failing configuration, so adding a new
// NeoForge variant never breaks the build - a client run on an unpinned variant fails loudly at boot on the
// missing eunomia dependency, which is the signal to pin it.
val eunomiaCfFile = findProperty("eunomia.cf.file")?.toString()
val copyEunomiaToMods = if (eunomiaCfFile != null) {
    val eunomiaCfProject = findProperty("eunomia.cf.project")?.toString()
        ?: error("eunomia.cf.project is not set; cannot resolve the eunomia mod jar from CurseForge")
    val eunomiaRuntimeMod = configurations.create("eunomiaRuntimeMod") {
        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false
        isTransitive = false
    }
    dependencies.add("eunomiaRuntimeMod", "curse.maven:eunomia-$eunomiaCfProject:$eunomiaCfFile")
    tasks.register<Copy>("copyEunomiaToMods") {
        group = "verification"
        description = "Drop the eunomia NeoForge mod jar (CurseForge $eunomiaCfProject/$eunomiaCfFile) into run/mods/."
        from(eunomiaRuntimeMod)
        into(project.layout.projectDirectory.dir("run/mods"))
        // fetchCompatJars wipes run/mods first on smoke runs; land after it so the eunomia jar survives.
        mustRunAfter("fetchCompatJars")
        outputs.upToDateWhen { false }
        // Clear any previously-copied eunomia jar first: a version bump changes the CF file id (and thus the
        // jar name), which would otherwise leave TWO eunomia mods in run/mods and load both.
        doFirst {
            delete(fileTree(project.layout.projectDirectory.dir("run/mods")) { include("eunomia*.jar") })
        }
    }
} else {
    logger.warn(
        "[armor-hider] eunomia.cf.file is not pinned for ${sc.current.project}; the eunomia mod will NOT be " +
            "placed in run/mods, so a client run on this variant fails its required eunomia dependency at boot."
    )
    null
}

neoForge {
    version = neoforgeVersion

    runs {
        register("client") {
            client()
            taskBefore(expandResourcesForIdea)
            // eunomia mod into run/mods before boot (required runtime dependency). Not gated on -Psmoke:
            // a dev client launch needs it too. copyEunomiaToMods mustRunAfter fetchCompatJars, so on smoke
            // runs the eunomia jar lands after that task wipes run/mods. Null on an unpinned variant.
            copyEunomiaToMods?.let { taskBefore(it) }
            // Halt the game JVM if the gradle/IDE launcher dies, so an interrupted run never orphans
            // a multi-GB Minecraft JVM. Dev-only; production jars never see this property. See DevRunWatchdog.
            jvmArgument("-Darmorhider.devRun.watchdog=true")

            if (findProperty("smoke") != null) {
                taskBefore(tasks.named("fetchCompatJars"))
                // Cap the smoke client heap (tiny synthetic worlds) - see multiloader-loom.gradle.kts.
                jvmArgument("-Xmx2g")
                jvmArgument("-Darmorhider.smoke.exit=true")
                val delayMs = findProperty("smoke.delay.ms")?.toString() ?: "15000"
                jvmArgument("-Darmorhider.smoke.delay.ms=${delayMs}")
            }
            if (devProfile != null) {
                programArguments.addAll("--username", devProfile.username, "--uuid", devProfile.uuid)
                if (devProfile.skinTexturesValue != null) {
                    jvmArgument("-Darmorhider.dev.skin.textures=${devProfile.skinTexturesValue}")
                }
                if (devProfile.skinTexturesSignature != null) {
                    jvmArgument("-Darmorhider.dev.skin.signature=${devProfile.skinTexturesSignature}")
                }
            }
        }
        register("server") {
            server()
            taskBefore(expandResourcesForIdea)
            jvmArgument("-Darmorhider.devRun.watchdog=true")
        }
    }

    mods {
        register("armor_hider") {
            sourceSet(sourceSets.main.get())
            sourceSet(clientSourceSet)
        }
    }
}

tasks.jar {
    from(clientSourceSet.output)
    // Common client sources are in both main and client (main needs them for compile visibility,
    // client gets them from multiloader-loader). Exclude duplicates in the jar.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val expandProps = mapOf(
    "version" to project.version,
    "minecraft_version" to neoforgeVersionRange,
    "neoforge_version" to neoforgeVersion,
    "java_version" to javaVersion
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}
