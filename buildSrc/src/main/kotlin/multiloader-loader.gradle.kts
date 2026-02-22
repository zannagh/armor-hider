plugins {
    id("java")
    id("multiloader-common")
}

val sc = project.stonecutterBuild
val commonNode = sc.node.sibling("common")
    ?: error("Could not find common branch for version ${sc.current.project}")
val commonPath = commonNode.hierarchy.toString()

// Ensure common project is fully evaluated (including splitEnvironmentSourceSets)
// before we access its source sets
evaluationDependsOn(commonPath)

val commonProject = project(commonPath)
val commonSourceSets = commonProject.extensions.getByType(SourceSetContainer::class.java)

// Carry over compile-only dependencies from common that are needed when compiling common sources
dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
}

// Include common's sources in the loader's source sets so the IDE can resolve them
sourceSets.main {
    java { commonSourceSets["main"].java.srcDirs.forEach { srcDir(it) } }
    java { commonSourceSets["client"].java.srcDirs.forEach { srcDir(it) } }
    resources { commonSourceSets["main"].resources.srcDirs.forEach { srcDir(it) } }
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

    jar {
        inputs.property("archivesName", base.archivesName)
    }
}
