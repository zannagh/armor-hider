plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.140" apply false
}

stonecutter active "fabric-26.1.2" /* [SC] DO NOT EDIT */

tasks.register("stageArtifacts") {
    group = "build"
    description = "Builds all loader variants and copies unique artifacts to staging/"

    val loaderProjects = allprojects.filter {
        it.path.startsWith(":fabric:") || it.path.startsWith(":neoforge:")
    }
    loaderProjects.forEach { dependsOn("${it.path}:build") }

    val staging = rootProject.file("staging")

    doLast {
        staging.deleteRecursively()
        staging.mkdirs()

        val versionMap = mutableMapOf<String, MutableMap<String, List<String>>>()
        for (proj in loaderProjects) {
            val displayVersion = proj.findProperty("display_version")?.toString() ?: continue
            val gameVersions = proj.findProperty("game_versions")?.toString()
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: error("Missing game_versions for ${proj.name}")
            val loader = proj.name.substringBefore("-")
            val existing = versionMap.getOrPut(loader) { mutableMapOf() }
                .putIfAbsent(displayVersion, gameVersions)
            if (existing != null && existing.sorted() != gameVersions.sorted()) {
                error("Conflicting game_versions for $loader/$displayVersion: $existing vs $gameVersions")
            }

            proj.layout.buildDirectory.dir("libs").get().asFile.let { libsDir ->
                libsDir.listFiles()
                    ?.filter { it.extension == "jar" && !it.name.endsWith("-sources.jar") }
                    ?.forEach { jar ->
                        val target = staging.resolve(jar.name)
                        if (!target.exists()) jar.copyTo(target)
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
        staging.resolve("versions.json").writeText(versionMapJson)

        val files = staging.listFiles()?.filter { it.extension == "jar" }?.sortedBy { it.name } ?: emptyList()
        println("Staged ${files.size} artifacts:")
        files.forEach { println("  ${it.name} (${it.length() / 1024} KB)") }
    }
}
