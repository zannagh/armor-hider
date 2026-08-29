import org.gradle.api.Project

/**
 * Adds `dependencyNotation:<propertyValue>` to [configuration] iff [propertyName] is set on the
 * project. [dependencyNotation] is the group:artifact coordinate (a trailing ':' is optional); the
 * property supplies the version. No-op when the property is absent, so version-gated compat deps
 * drop out cleanly on the MC variants that don't pin them.
 */
fun Project.addDependency(configuration: String, propertyName: String, dependencyNotation: String) {
    var not = dependencyNotation
    if (!not.endsWith(":")) {
        not += ":"
    }
    if (hasProperty(propertyName)) {
        dependencies.add(configuration, "${not}${findProperty(propertyName)}")
    }
}

fun Project.addCompileOnlyDependency(propertyName: String, dependencyNotation: String) {
    addDependency("compileOnly", propertyName, dependencyNotation)
}
