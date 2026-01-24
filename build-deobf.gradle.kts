plugins {
    id("net.fabricmc.fabric-loom") version "1.14-SNAPSHOT"
    id("maven-publish")
}

// Convert Maven version (e.g., 26.1-snapshot-4) to Fabric version (e.g., 26.1-alpha.4)
// Mojang uses "snapshot" in Maven artifacts but "alpha" in the internal version ID
val fabricGameVersion: String = stonecutter.current.project.replace("-snapshot-", "-alpha.")

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
        vmArg("-Dfabric.gameVersion=$fabricGameVersion")
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
    inputs.property("minecraft_version", fabricGameVersion)
    inputs.property("java_version", 25)

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to fabricGameVersion,
            "java_version" to 25
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

// Server startup test - verifies the mod loads without crashing
tasks.register("serverStartupTest") {
    group = "verification"
    description = "Starts a Minecraft server with the mod to verify it loads correctly"
    dependsOn(tasks.named("jar"))

    doLast {
        val serverDir = layout.buildDirectory.dir("server-test").get().asFile
        serverDir.mkdirs()

        // Accept EULA
        File(serverDir, "eula.txt").writeText("eula=true")

        // Get the server jar from Loom cache
        val minecraftVersion = stonecutter.current.project
        val serverJar = File(System.getProperty("user.home"), ".gradle/caches/fabric-loom/$minecraftVersion/minecraft-server.jar")

        if (!serverJar.exists()) {
            throw GradleException("Server jar not found at $serverJar. Run './gradlew downloadAssets' first.")
        }

        // Copy mod jar to mods folder
        val modsDir = File(serverDir, "mods")
        modsDir.mkdirs()
        val modJar = tasks.named<org.gradle.jvm.tasks.Jar>("jar").get().archiveFile.get().asFile
        modJar.copyTo(File(modsDir, modJar.name), overwrite = true)

        // Copy Fabric Loader
        val fabricLoaderJar = configurations.named("implementation").get().files.find { it.name.contains("fabric-loader") }
        if (fabricLoaderJar != null) {
            fabricLoaderJar.copyTo(File(modsDir, fabricLoaderJar.name), overwrite = true)
        }

        // Start server with timeout
        val process = ProcessBuilder(
            "java", "-Xmx512M",
            "-Dfabric.skipMcProvider=true",
            "-Dfabric.gameVersion=$fabricGameVersion",
            "-jar", serverJar.absolutePath,
            "--nogui"
        )
            .directory(serverDir)
            .redirectErrorStream(true)
            .start()

        val timeout = 120_000L // 2 minutes
        var serverStarted = false
        var error: String? = null

        Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                println("[Server] $line")
                if (line.contains("Done") && line.contains("For help, type")) {
                    serverStarted = true
                    // Send stop command
                    process.outputStream.bufferedWriter().apply {
                        write("stop\n")
                        flush()
                    }
                }
                if (line.contains("Exception") || line.contains("Error loading mod")) {
                    error = line
                }
            }
        }.start()

        // Wait for completion or timeout
        val completed = process.waitFor(timeout, TimeUnit.MILLISECONDS)

        if (!completed) {
            process.destroyForcibly()
            throw GradleException("Server startup test timed out after ${timeout / 1000} seconds")
        }

        if (error != null) {
            throw GradleException("Server startup failed: $error")
        }

        if (!serverStarted) {
            throw GradleException("Server did not start successfully. Exit code: ${process.exitValue()}")
        }

        println("Server startup test passed!")
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
