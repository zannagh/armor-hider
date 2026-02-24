plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.140" apply false
}

stonecutter active "fabric-1.21.11" /* [SC] DO NOT EDIT */

tasks.register("stageArtifacts") {
    group = "build"
    description = "Builds all loader variants and copies unique artifacts to staging/"

    allprojects.filter {
        it.path.startsWith(":fabric:") || it.path.startsWith(":neoforge:")
    }.forEach {
        dependsOn("${it.path}:build")
    }

    val staging = rootProject.file("staging")
    val versionDirs = listOf("fabric/versions", "neoforge/versions").map { rootProject.file(it) }

    doLast {
        staging.deleteRecursively()
        staging.mkdirs()

        versionDirs.forEach { dir ->
            dir.walkTopDown()
                .filter { it.extension == "jar" && it.path.contains("/build/libs/") && !it.name.endsWith("-sources.jar") }
                .forEach { jar ->
                    val target = staging.resolve(jar.name)
                    if (!target.exists()) {
                        jar.copyTo(target)
                    }
                }
        }

        val files = staging.listFiles()?.sortedBy { it.name } ?: emptyList()
        println("Staged ${files.size} artifacts:")
        files.forEach { println("  ${it.name} (${it.length() / 1024} KB)") }
    }
}
