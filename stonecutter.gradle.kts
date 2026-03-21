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

    // Resolve everything at configuration time to stay config-cache-safe
    val staging = rootProject.file("staging")
    val versionDirs = listOf("fabric/versions", "neoforge/versions").map { rootProject.file(it) }

    // Build the version map from gradle.properties: { loader: { displayVersion: [mcVersion, ...] } }
    val versionMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
    for (loaderDir in versionDirs) {
        loaderDir.listFiles()?.filter { it.isDirectory }?.forEach { versionDir ->
            val props = java.util.Properties()
            val propsFile = versionDir.resolve("gradle.properties")
            if (propsFile.exists()) {
                propsFile.reader().use { props.load(it) }
            }
            val displayVersion = props.getProperty("display_version") ?: return@forEach
            val dirName = versionDir.name // e.g. "fabric-1.20"
            val loader = dirName.substringBefore("-")
            val mcVersion = dirName.substringAfter("$loader-")
            versionMap.getOrPut(loader) { mutableMapOf() }
                .getOrPut(displayVersion) { mutableListOf() }
                .add(mcVersion)
        }
    }
    val versionMapJson = versionMap.entries.sortedBy { it.key }.joinToString(",\n  ", "{\n  ", "\n}") { (loader, groups) ->
        val groupsJson = groups.entries.sortedBy { it.key }.joinToString(",\n    ", "{\n    ", "\n  }") { (display, versions) ->
            val versionsJson = if (versions.size == 1 && versions[0] == display) "null"
            else versions.sorted().joinToString("\", \"", "[\"", "\"]")
            "\"$display\": $versionsJson"
        }
        "\"$loader\": $groupsJson"
    }

    doLast {
        staging.deleteRecursively()
        staging.mkdirs()

        versionDirs.forEach { dir ->
            dir.walkTopDown()
                .filter { it.extension == "jar" && it.parentFile.name == "libs" && it.parentFile.parentFile.name == "build" && !it.name.endsWith("-sources.jar") }
                .forEach { jar ->
                    val target = staging.resolve(jar.name)
                    if (!target.exists()) {
                        jar.copyTo(target)
                    }
                }
        }

        staging.resolve("versions.json").writeText(versionMapJson)

        val files = staging.listFiles()?.filter { it.extension == "jar" }?.sortedBy { it.name } ?: emptyList()
        println("Staged ${files.size} artifacts:")
        files.forEach { println("  ${it.name} (${it.length() / 1024} KB)") }
    }
}
