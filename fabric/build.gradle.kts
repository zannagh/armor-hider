plugins {
    id("multiloader-loader")
}

apply(plugin = if (project.isDeobf) "loom-deobfuscated" else "loom-obfuscated")

val sc = project.stonecutterBuild
val fabricVersion = findProperty("fabric.minecraft_version")!!.toString()

stonecutter {
    constants["fabric"] = true
}

configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
    splitEnvironmentSourceSets()

    mods {
        register("armor-hider") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }

    // Shared run directory for all versions
    runConfigs.configureEach {
        runDir = "run"
        ideConfigGenerated(true)
        if (project.isDeobf) {
            vmArg("-Dfabric.gameVersion=${fabricVersion}")
        }
    }
}

dependencies {
    if (!project.isDeobf) {
        add("modImplementation", "net.fabricmc:fabric-loader:${property("loader_version")}")
    }
}

val expandProps = mapOf(
    "version" to project.version,
    "java_version" to project.prop("java.version")!!,
    "fabric_minecraft_version" to project.prop("fabric.minecraft_version_range")!!
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching(listOf("fabric.mod.json", "**/*.mixins.json"), ExpandPropertiesAction(expandProps))
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json", ExpandPropertiesAction(expandProps))
}

// IntelliJ's native builder copies resources without Gradle's property expansion,
// so ${version} placeholders in fabric.mod.json and mixin JSONs stay unexpanded.
// These tasks overlay the expanded output into IDEA's output dirs.
val processClientResources = tasks.named<ProcessResources>("processClientResources")

val expandResourcesForIdea by tasks.registering {
    dependsOn(tasks.processResources, processClientResources)
    doLast {
        copy {
            from(tasks.processResources.map { (it as ProcessResources).destinationDir })
            into(layout.projectDirectory.dir("out/production/resources"))
        }
        copy {
            from(processClientResources.map { it.destinationDir })
            into(layout.projectDirectory.dir("out/client/resources"))
        }
    }
}

// Make Loom's run tasks depend on the expansion so Gradle-based runs also work.
tasks.matching { it.name == "runClient" || it.name == "runServer" }.configureEach {
    dependsOn(expandResourcesForIdea)
}

// Patch Loom-generated IntelliJ run configs to add expandResourcesForIdea as a
// before-launch Gradle task, so IDEA runs also get expanded resources.
// Loom hardcodes only "Make" in its template with no API to add custom tasks.
tasks.matching { it.name == "ideaSyncTask" }.configureEach {
    doLast {
        val configDir = rootProject.file(".idea/runConfigurations")
        if (!configDir.isDirectory) return@doLast
        val relPath = project.projectDir.toRelativeString(rootProject.projectDir)
        configDir.listFiles()?.filter {
            it.extension == "xml" && it.name.contains(project.name)
        }?.forEach { xmlFile ->
            var content = xmlFile.readText()
            if (content.contains("expandResourcesForIdea")) return@forEach
            val gradleStep = """<option enabled="true" name="Gradle.BeforeRunTask" tasks="expandResourcesForIdea" externalProjectPath="${'$'}PROJECT_DIR${'$'}/$relPath" vmOptions="" scriptParameters="" />"""
            content = content.replace(
                """<option enabled="true" name="Make"/>""",
                """<option enabled="true" name="Make"/>
      $gradleStep"""
            )
            xmlFile.writeText(content)
        }
    }
}
