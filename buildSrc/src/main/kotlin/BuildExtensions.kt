import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import java.io.Serializable

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
