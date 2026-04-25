import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URI
import java.util.Properties

val Project.stonecutterBuild: StonecutterBuildExtension
    get() = extensions.getByType(StonecutterBuildExtension::class.java)

class ExpandPropertiesAction(private val props: Map<String, Any>) : Action<FileCopyDetails>, Serializable {
    override fun execute(details: FileCopyDetails) {
        details.expand(props)
    }
}

fun Jar.includeLicense(archivesName: String) {
    from("LICENSE") {
        rename("LICENSE", "LICENSE_$archivesName")
    }
}

fun Project.registerExpandResourcesForIdea(
    vararg resourceOutputMappings: Pair<TaskProvider<ProcessResources>, String>
): TaskProvider<Task> {
    val mappings: List<Pair<TaskProvider<ProcessResources>, String>> = resourceOutputMappings.toList()
    return tasks.register("expandResourcesForIdea") {
        mappings.forEach { dependsOn(it.first) }
        val proj = project
        doLast {
            for (m in mappings) {
                val files = (m.first.get() as Task).outputs.files
                val dir = proj.layout.projectDirectory.dir(m.second)
                proj.copy {
                    from(files)
                    into(dir)
                }
            }
        }
    }
}

fun Project.patchLoomIdeRunConfigs(expandTask: TaskProvider<Task>) {
    tasks.matching { it.name == "runClient" || it.name == "runServer" }.configureEach {
        dependsOn(expandTask)
    }

    tasks.matching { it.name == "ideaSyncTask" }.configureEach {
        doLast {
            val configDir = rootProject.file(".idea/runConfigurations")
            if (!configDir.isDirectory) return@doLast
            val relPath = project.projectDir.toRelativeString(rootProject.projectDir)
            configDir.listFiles()?.filter {
                it.extension == "xml" && it.name.contains(project.name)
            }?.forEach { xmlFile ->
                var content = xmlFile.readText()
                content = allowParallelRun(content)
                if (content.contains("expandResourcesForIdea")) {
                    xmlFile.writeText(content)
                    return@forEach
                }
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
}

fun Project.patchIdeRunConfigsAllowParallel() {
    tasks.matching { it.name == "ideaSyncTask" }.configureEach {
        doLast {
            val configDir = rootProject.file(".idea/runConfigurations")
            if (!configDir.isDirectory) return@doLast
            configDir.listFiles()?.filter {
                it.extension == "xml" && it.name.contains(project.name)
            }?.forEach { xmlFile ->
                val content = xmlFile.readText()
                val patched = allowParallelRun(content)
                if (patched != content) xmlFile.writeText(patched)
            }
        }
    }
}

private fun allowParallelRun(content: String): String {
    if (content.contains("allow-running-in-parallel")) return content
    return content.replace("<configuration ", "<configuration allow-running-in-parallel=\"true\" ")
}

data class DevProfile(
    val username: String,
    val uuid: String,
    val skinTexturesValue: String? = null,
    val skinTexturesSignature: String? = null
)

fun Project.loadDevProfile(): DevProfile? {
    val file = rootProject.file("dev-profile.properties")
    if (!file.exists()) {
        return null
    }

    val props = try {
        Properties().apply { file.inputStream().use { load(it) } }
    } catch (e: Exception) {
        logger.warn("[ArmorHider] Failed to load ${file.path}; dev profile will be ignored.", e)
        return null
    }

    val username = props.getProperty("username")
    if (username == null) {
        logger.warn("[ArmorHider] ${file.path} exists but is missing required property 'username'; dev profile will be ignored.")
        return null
    }

    val offline = gradle.startParameter.isOffline

    val uuid = props.getProperty("uuid") ?: run {
        if (offline) {
            logger.warn("[ArmorHider] Gradle is offline and no 'uuid' in dev-profile.properties; dev profile will be ignored.")
            return null
        }
        val resolved = resolveUuid(username)
        if (resolved == null) {
            logger.warn("[ArmorHider] Could not resolve UUID for '$username'; dev profile will be ignored.")
            return null
        }
        resolved
    }

    val (texValue, texSignature) = if (offline) {
        logger.info("[ArmorHider] Gradle is offline; skipping texture resolution for dev profile '$username'.")
        null to null
    } else {
        resolveTextures(uuid.replace("-", ""))
    }

    logger.info("[ArmorHider] Dev profile: username=$username, uuid=$uuid, textures=${if (texValue != null) "present" else "null"}, signature=${if (texSignature != null) "present" else "null"}")
    return DevProfile(username, uuid, texValue, texSignature)
}

private fun fetchJson(uri: String): String? {
    val conn = URI(uri).toURL().openConnection() as HttpURLConnection
    conn.connectTimeout = 5_000
    conn.readTimeout = 5_000
    conn.requestMethod = "GET"
    return try {
        if (conn.responseCode != 200) return null
        conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
    } finally {
        conn.disconnect()
    }
}

private fun resolveUuid(username: String): String? {
    return try {
        val json = fetchJson("https://api.mojang.com/users/profiles/minecraft/$username") ?: return null
        val idRaw = Regex(""""id"\s*:\s*"([0-9a-f]+)"""").find(json)?.groupValues?.get(1) ?: return null
        "${idRaw.substring(0, 8)}-${idRaw.substring(8, 12)}-${idRaw.substring(12, 16)}-${idRaw.substring(16, 20)}-${idRaw.substring(20)}"
    } catch (e: Exception) {
        null
    }
}

private fun resolveTextures(uuidNoDashes: String): Pair<String?, String?> {
    return try {
        val json = fetchJson("https://sessionserver.mojang.com/session/minecraft/profile/$uuidNoDashes?unsigned=false") ?: return null to null
        val propsMatch = Regex(""""properties"\s*:\s*\[([^]]+)]""").find(json)?.groupValues?.get(1) ?: return null to null
        val value = Regex(""""value"\s*:\s*"([^"]+)"""").find(propsMatch)?.groupValues?.get(1)
        val signature = Regex(""""signature"\s*:\s*"([^"]+)"""").find(propsMatch)?.groupValues?.get(1)
        value to signature
    } catch (e: Exception) {
        null to null
    }
}
