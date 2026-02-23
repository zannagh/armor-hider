plugins {
    id("java")
    id("multiloader-common")
}

val sc = project.stonecutterBuild
sc.constants["fabric"] = findProperty("mod_loader")!!.toString() == "fabric"
sc.constants["neoforge"] = findProperty("mod_loader")!!.toString() == "neoforge"
val commonNode = sc.node.sibling("common")
    ?: error("Could not find common branch for version ${sc.current.version}")
val commonPath = commonNode.hierarchy.toString()

// Ensure common project is fully evaluated (including splitEnvironmentSourceSets)
// before we access its source sets
evaluationDependsOn(commonPath)

val commonProject = project(commonPath)
val commonSourceSets = commonProject.extensions.getByType(SourceSetContainer::class.java)

// Expose common source sets for loader build scripts that need additional wiring
extra["commonSourceSets"] = commonSourceSets

// Carry over compile-only dependencies from common that are needed when compiling common sources
dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
}

// Include common's sources in the loader's source sets so the IDE can resolve them
sourceSets.main {
    java { commonSourceSets["main"].java.srcDirs.forEach { srcDir(it) } }
    resources { commonSourceSets["main"].resources.srcDirs.forEach { srcDir(it) } }
}

// Wire common client sources into the loader's client source set (created later by
// splitEnvironmentSourceSets or sourceSets.create in the loader's build script)
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
