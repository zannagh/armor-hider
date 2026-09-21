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
// only compiles against eunomia-core. The dependency is declared side="BOTH", so every run - client AND
// dedicated server - must have the eunomia NeoForge mod jar in run/mods, or armor-hider fails its
// dependency and the game aborts at boot. Resolved from MODRINTH, via a version id derived from
// `eunomia.version` + `display_version` rather than a hand-maintained pin - see the comment on
// `eunomiaModrinthVersion` below. CurseForge remains only as a fallback branch for the case where that id
// cannot be derived; its `eunomia.cf.file` pins are frozen and are not re-pinned on a version bump.
// This mirrors the Fabric copyEunomiaToMods in multiloader-loom.gradle.kts. The gameDir for a run is the
// variant's `run/` dir, so run/mods is where NeoForge/FML loads it from - the same dir fetchCompatJars uses.
// Lenient by design: a variant that can resolve neither source simply gets no copy task rather than failing
// configuration, so adding a new NeoForge variant never breaks the build - a client or server run on such a
// variant fails loudly at boot on the missing eunomia dependency, which is the signal to fix it.
val eunomiaCfFile = findProperty("eunomia.cf.file")?.toString()
// Modrinth version id for this variant, DERIVED rather than pinned - "neo-<display_version>-<semver>",
// e.g. "neo-26.2-0.3.13". Mirrors the Fabric side in multiloader-loom.gradle.kts; see the long comment
// there for why Modrinth is preferred (publishes in minutes vs CurseForge moderation, and no per-pageSize
// stale-cache hazard in its API). A bump is one `eunomia.version` edit, not 21 opaque file ids.
val eunomiaModrinthVersion = findProperty("eunomia.version")?.toString()
    ?.let { semVer ->
        findProperty("display_version")?.toString()?.let { display -> "neo-$display-$semVer" }
    }
val copyEunomiaToMods = if (eunomiaModrinthVersion != null) {
    val eunomiaRuntimeMod = configurations.create("eunomiaRuntimeMod") {
        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false
        isTransitive = false
    }
    dependencies.add("eunomiaRuntimeMod", "maven.modrinth:eunomia:$eunomiaModrinthVersion")
    tasks.register<Copy>("copyEunomiaToMods") {
        group = "verification"
        description = "Drop the eunomia NeoForge mod jar (Modrinth $eunomiaModrinthVersion) into run/mods/."
        from(eunomiaRuntimeMod)
        into(project.layout.projectDirectory.dir("run/mods"))
        outputs.upToDateWhen { false }
        doFirst {
            delete(fileTree(project.layout.projectDirectory.dir("run/mods")) { include("eunomia*.jar") })
        }
    }
} else if (eunomiaCfFile != null) {
    // FALLBACK ONLY - unreachable while `eunomia.version` and `display_version` are both set, which is every
    // configured variant. Kept for the case where the Modrinth id cannot be derived. Its pins are frozen:
    // see the note above `eunomia.cf.project` in stonecutter.properties.toml.
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
        "[armor-hider] could not resolve the eunomia mod jar for ${sc.current.project}: neither a Modrinth " +
            "id (needs `eunomia.version` + `display_version`) nor a `eunomia.cf.file` fallback pin is " +
            "available. The eunomia mod will NOT be placed in run/mods, so a client or server run on this " +
            "variant fails its required eunomia dependency at boot. Set `display_version` for this variant."
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
            // The server needs eunomia too: neoforge.mods.toml declares the dependency side="BOTH" and the
            // server code uses eunomia's transport, so a dedicated-server run without the jar in run/mods
            // fails mod resolution at boot exactly like the client does. Same null-safe wiring as the
            // client run above - null on a variant that doesn't pin eunomia.cf.file.
            copyEunomiaToMods?.let { taskBefore(it) }
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
