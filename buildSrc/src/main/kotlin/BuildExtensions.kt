import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import java.io.Serializable

fun Project.prop(key: String): String? = findProperty(key)?.toString()

val Project.stonecutterBuild: StonecutterBuildExtension
    get() = extensions.getByType(StonecutterBuildExtension::class.java)

/** The Minecraft version from the Stonecutter project, e.g. "1.21.11" from "fabric-1.21.11".
 * For snapshots and pre-releases Stonecutter encodes versions as e.g. 26.1-0.snapshot.1 or 26.1-1.pre.1
 * to satisfy its versioning / semVer parsing. The corresponding Minecraft versions use a dash before
 * the prerelease number, so we convert them back to e.g. 26.1-snapshot-1 or 26.1-pre-1.
 */
val Project.mcVersion: String get() = stonecutterBuild.current.version
    .replace("0.snapshot.", "snapshot-")
    .replace("1.pre.", "pre-")
    .replace("2.rc.", "rc-")

/** Whether this version uses deobfuscated (unmapped) Minecraft jars. */
val Project.isDeobf: Boolean get() = mcVersion.startsWith("26.")

/**
 * An unfortunately required hack to get configuration-cache-safe expansion of properties into files.
 */
class ExpandPropertiesAction(private val props: Map<String, Any>) : Action<FileCopyDetails>, Serializable {
    override fun execute(details: FileCopyDetails) {
        details.expand(props)
    }
}

/** Configures the jar task to include LICENSE with a project-specific suffix. */
fun Jar.includeLicense(archivesName: String) {
    from("LICENSE") {
        rename("LICENSE", "LICENSE_$archivesName")
    }
}

/**
 * Registers an `expandResourcesForIdea` task that copies Gradle-processed resources into
 * IntelliJ's output directories. This is needed because IntelliJ's native "Make" copies raw
 * resources without Gradle's property expansion, leaving `${'$'}{version}` placeholders unexpanded.
 *
 * @param resourceOutputMappings pairs of (processResources task, IDEA output dir path)
 */
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

/**
 * Patches Fabric Loom's generated IntelliJ run configurations to add `expandResourcesForIdea`
 * as a Gradle before-launch step. Also wires Loom's `runClient`/`runServer` tasks to depend
 * on the expand task so Gradle-based runs work too.
 *
 * Loom hardcodes only "Make" in its run config template with no API to add custom steps,
 * so we post-process the generated XML files during `ideaSyncTask`.
 */
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
}
