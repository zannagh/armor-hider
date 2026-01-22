plugins {
    id("net.fabricmc.fabric-loom") version "1.14-SNAPSHOT"
    id("maven-publish")
}

fun getGitVersion(): String {
    var isPreRelease = true
    val preReleaseProperty = findProperty("prerelease")?.toString() ?: ""
    if (preReleaseProperty.isNotEmpty() && preReleaseProperty.lowercase() == "false") {
        isPreRelease = false
    }
    val ciVersionProperty = findProperty("semVer")?.toString() ?: ""
    val modLoader = findProperty("mod_loader")?.toString() ?: "fabric"
    val gameVersion = stonecutter.current.project

    val buildVersion = if (ciVersionProperty.isNotEmpty()) {
        val ciVersion = "$gameVersion-$ciVersionProperty"
        "$modLoader-$ciVersion"
    } else {
        "$modLoader-${getVersionFromGitVersionOrTag(gameVersion)}"
    }

    return if (isPreRelease) "$buildVersion-preview" else buildVersion
}

version = getGitVersion()
group = property("maven_group").toString()

base {
    archivesName.set(property("archives_base_name").toString())
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // Loom adds the essential maven repositories automatically.
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("armor-hider") {
            sourceSet("main")
            sourceSet("client")
        }
    }

    // Shared run directory for all versions
    runConfigs.configureEach {
        runDir = "run"
        // Explicitly set game version for Fabric Loader (26.1 snapshots use non-semantic versioning)
        vmArg("-Dfabric.gameVersion=${stonecutter.current.project}")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.project}")
    // No mappings needed for unobfuscated Minecraft 26.1+
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", "26.1-alpha.4")

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to "26.1-alpha.4"
        )
    }
}

// Minecraft 26.1+ requires Java 25
val javaVersion = JavaVersion.VERSION_25
val javaVersionAsInt = 25

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersionAsInt)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersionAsInt))
    }
    withSourcesJar()
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.jar {
    inputs.property("archivesName", base.archivesName)

    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name").toString()
            from(components["java"])
        }
    }
    repositories {
        // Add repositories to publish to here.
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

fun getVersionFromGitVersionOrTag(gameVersion: String): String {
    try {
        val semVer: String
        val commitsSinceSource: String
        val uncommittedChanges: String
        try {
            semVer = Runtime.getRuntime().exec(arrayOf("gitversion", "/output", "json", "/showVariable", "majorMinorPatch"))
                .inputStream.bufferedReader().readText().trim()
            commitsSinceSource = Runtime.getRuntime().exec(arrayOf("gitversion", "/output", "json", "/showVariable", "CommitsSinceVersionSource"))
                .inputStream.bufferedReader().readText().trim()
            uncommittedChanges = Runtime.getRuntime().exec(arrayOf("gitversion", "/output", "json", "/showVariable", "UncommittedChanges"))
                .inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
            val semVerProc = Runtime.getRuntime().exec(arrayOf("dotnet", "gitversion", "/output", "json", "/showVariable", "majorMinorPatch"))
            val commitsSinceProc = Runtime.getRuntime().exec(arrayOf("dotnet", "gitversion", "/output", "json", "/showVariable", "CommitsSinceVersionSource"))
            val uncommittedProc = Runtime.getRuntime().exec(arrayOf("dotnet", "gitversion", "/output", "json", "/showVariable", "UncommittedChanges"))
            return getVersionFromGitVersionOrTagFallback(
                semVerProc.inputStream.bufferedReader().readText().trim(),
                commitsSinceProc.inputStream.bufferedReader().readText().trim(),
                uncommittedProc.inputStream.bufferedReader().readText().trim(),
                gameVersion
            )
        }
        return getVersionFromGitVersionOrTagFallback(semVer, commitsSinceSource, uncommittedChanges, gameVersion)
    } catch (_: Exception) {
        val lastKnownTag = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags"))
            .inputStream.bufferedReader().readText().trim()
        return if (lastKnownTag.contains(gameVersion)) {
            lastKnownTag
        } else {
            "$gameVersion-$lastKnownTag"
        }
    }
}

fun getVersionFromGitVersionOrTagFallback(semVer: String, commitsSinceSource: String, uncommittedChanges: String, gameVersion: String): String {
    if (semVer.isEmpty() || !semVer.contains(Regex("\\d")) || !commitsSinceSource.matches(Regex("\\d.*"))) {
        throw Exception("Invalid version info")
    }
    return when (commitsSinceSource) {
        "0" -> if (uncommittedChanges == "0") "$gameVersion-$semVer" else "$gameVersion-$semVer-$uncommittedChanges"
        else -> "$gameVersion-$semVer-$commitsSinceSource"
    }
}
