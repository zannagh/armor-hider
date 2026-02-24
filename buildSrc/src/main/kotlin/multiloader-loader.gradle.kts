plugins {
    id("java")
    id("multiloader-common")
}

val sc = project.stonecutterBuild
val loaderName = sc.current.project.substringBefore("-")
sc.constants["fabric"] = loaderName == "fabric"
sc.constants["neoforge"] = loaderName == "neoforge"
sc.constants["quilt"] = loaderName == "quilt"
sc.constants["paper"] = loaderName == "paper"
val commonNode = sc.node.sibling("common")
    ?: error("Could not find common branch sibling for ${sc.current.project}")
val commonPath = commonNode.hierarchy.toString()

// Ensure common project is fully evaluated before accessing its source sets
evaluationDependsOn(commonPath)

val commonProject = project(commonPath)
val commonSourceSets = commonProject.extensions.getByType(SourceSetContainer::class.java)

// Expose common source sets for loader build scripts that need additional wiring
extra["commonSourceSets"] = commonSourceSets

// Carry over compile-only dependencies from common that are needed when compiling common sources
dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
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
