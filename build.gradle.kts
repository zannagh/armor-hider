plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT"
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

    return if (isPreRelease) {
        val preReleaseVersion = findProperty("preReleaseVersion")?.toString() ?: ""
        if (preReleaseVersion.isNotEmpty()) {
            "$buildVersion-preview.$preReleaseVersion"
        } else {
            "$buildVersion-preview"
        }
    } 
    else {
        buildVersion
    }
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
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.project}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", stonecutter.current.project)
    inputs.property("java_version", javaVersionAsInt)

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to stonecutter.current.project,
            "java_version" to javaVersionAsInt
        )
    }
    filesMatching("armor-hider.mixins.json") {
        expand(
            "java_version" to javaVersionForMixin,
            "mixin_string" to getMainMixinString()
        )
    }
}

tasks.named<ProcessResources>("processClientResources") {
    inputs.property("java_version", javaVersionForMixin)
    inputs.property("mixin_string", getClientMixinString())
    inputs.property("options_screen_mixin_string", getOptionsScreenMixinString())

    filesMatching("armor-hider.client.mixins.json") {
        expand(
            "java_version" to javaVersionForMixin,
            "mixin_string" to getClientMixinString(),
            "options_screen_mixin_string" to getOptionsScreenMixinString()
        )
    }
}

fun getClientMixinString(): String {
    var returnString = "";
    
    if (sc.current.parsed > "1.21.1"){
        returnString += "bodyKneesAndToes.EquipmentRenderMixin\",\n"
        returnString += "    \"bodyKneesAndToes.ArmorFeatureRenderMixin\",\n"
    }
    else {
        returnString += "bodyKneesAndToes.HumanoidArmorLayerMixin\",\n"
    }

    if (sc.current.parsed >= "1.20.5") {
        
        returnString += "    \"networking.ClientPacketListenerMixin"
    }
    else {
        returnString += "    \"networking.ClientPlayNetworkHandlerMixin"
    }
    return returnString;
}

fun getOptionsScreenMixinString(): String {
    // For 1.20.x: Use OptionsScreenMixin (injects into main options screen)
    // For 1.21+: Use SkinOptionsMixin (injects into skin options screen)
    return if (sc.current.parsed > "1.21.1") {
        "SkinOptionsMixin"
    } else {
        "OptionsScreenMixin"
    }
}

fun getMainMixinString(): String {
    var returnString =
        "networking.MinecraftServerMixin\",\n" +
        "    \"networking.ServerLoginMixin\",\n"
    if (sc.current.parsed >= "1.20.5") {
        returnString +=
        "    \"networking.CustomPayloadCodecMixin\",\n" +
        "    \"networking.ServerGamePacketListenerMixin"
    }
    else {
        returnString += 
        "    \"networking.ServerPlayNetworkHandlerMixin"
    }
    return returnString;
}

val javaVersion: JavaVersion
    get() {
        return when {
            sc.current.parsed >= "1.20.6" -> JavaVersion.VERSION_21
            sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
            sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
            else -> JavaVersion.VERSION_1_8
        }
    }

val javaVersionForMixin: String
    get() {
        return when {
            sc.current.parsed >= "1.20.6" -> "JAVA_21"
            sc.current.parsed >= "1.18" -> "JAVA_17"
            sc.current.parsed >= "1.17" -> "JAVA_16"
            else -> "JAVA_18"
        }
    }

val javaVersionAsInt: Int
    get() {
        return when {
            sc.current.parsed >= "1.20.6" -> 21
            sc.current.parsed >= "1.18" -> 17
            sc.current.parsed >= "1.17" -> 16
            else -> 18
        }
    }

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
