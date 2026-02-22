import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Project

val Project.stonecutterBuild: StonecutterBuildExtension
    get() = extensions.getByType(StonecutterBuildExtension::class.java)
