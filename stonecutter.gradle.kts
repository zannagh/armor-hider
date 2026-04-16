plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.140" apply false
}

stonecutter active "fabric-26.1.2" /* [SC] DO NOT EDIT */

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

    // Build the version map from gradle.properties: { loader: { displayVersion: [gameVersion, ...] } }
    val versionMap = mutableMapOf<String, MutableMap<String, List<String>>>()
    for (loaderDir in versionDirs) {
        loaderDir.listFiles()?.filter { it.isDirectory }?.forEach { versionDir ->
            val props = java.util.Properties()
            val propsFile = versionDir.resolve("gradle.properties")
            if (propsFile.exists()) {
                propsFile.reader().use { props.load(it) }
            }
            val displayVersion = props.getProperty("display_version") ?: return@forEach
            val gameVersions = props.getProperty("game_versions")
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: error("Missing game_versions in ${versionDir.name}/gradle.properties")
            val dirName = versionDir.name // e.g. "fabric-1.20.1"
            val loader = dirName.substringBefore("-")
            val existing = versionMap.getOrPut(loader) { mutableMapOf() }
                .putIfAbsent(displayVersion, gameVersions)
            if (existing != null && existing.sorted() != gameVersions.sorted()) {
                error("Conflicting game_versions for $loader/$displayVersion: $existing vs $gameVersions")
            }
        }
    }
    val versionMapJson = versionMap.entries.sortedBy { it.key }.joinToString(",\n  ", "{\n  ", "\n}") { (loader, groups) ->
        val groupsJson = groups.entries.sortedBy { it.key }.joinToString(",\n    ", "{\n    ", "\n  }") { (display, versions) ->
            val versionsJson = versions.sorted().joinToString("\", \"", "[\"", "\"]")
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
