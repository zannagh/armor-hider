import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Project

const val FABRIC_LOADER_VERSION = "0.18.2"

fun Project.prop(key: String): String? = findProperty(key)?.toString()
val Project.stonecutterBuild: StonecutterBuildExtension
    get() = extensions.getByType(StonecutterBuildExtension::class.java)

/** The loader encoded in the Stonecutter project name, e.g. "fabric" from "fabric-1.21.11". */
val Project.loader: String get() = stonecutterBuild.current.project.substringBefore("-")

/** The Minecraft version from the Stonecutter project, e.g. "1.21.11" from "fabric-1.21.11". */
val Project.mcVersion: String get() = stonecutterBuild.current.version

/** Whether this version uses deobfuscated (unmapped) Minecraft jars. */
val Project.isDeobf: Boolean get() = mcVersion.startsWith("26.")
