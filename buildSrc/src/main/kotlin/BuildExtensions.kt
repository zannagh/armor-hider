import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCopyDetails
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
