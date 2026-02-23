plugins {
    id("multiloader-common")
    id("fabric-loom")
}

val sc = project.stonecutterBuild
val isDeobf = project.mcVersion.startsWith("26.")

stonecutter{
    constants["neoforge"] = sc.current.project.contains("neoforge")
    constants["fabric"] = sc.current.project.contains("fabric")
}

loom {
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
    minecraft("com.mojang:minecraft:${project.mcVersion}")
    if (!isDeobf) {
        // String-based calls: these Loom configurations don't exist when obfuscation is disabled
        add("mappings", loom.officialMojangMappings())
        add("modCompileOnly", "net.fabricmc:fabric-loader:${property("loader_version")}")
    } else {
        implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    }

    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

