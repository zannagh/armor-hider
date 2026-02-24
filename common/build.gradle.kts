plugins {
    id("multiloader-common")
}

apply(plugin = if (project.isDeobf) "loom-deobfuscated" else "loom-obfuscated")

val sc = project.stonecutterBuild

stonecutter {
    constants["neoforge"] = sc.current.project.contains("neoforge")
    constants["fabric"] = sc.current.project.contains("fabric")
}

configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
    splitEnvironmentSourceSets()

    mixin {
        useLegacyMixinAp = false
    }

    // Shared run directory for all versions
    runConfigs.configureEach {
        runDir = "run"
    }
}

dependencies {
    if (!project.isDeobf) {
        add("modCompileOnly", "net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")
    }

    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val expandProps = mapOf("java_version" to project.prop("java.version")!!)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json") { expand(expandProps) }
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.properties(expandProps)
    filesMatching("**/*.mixins.json") { expand(expandProps) }
}
