plugins {
    id("net.fabricmc.fabric-loom") version "1.14-SNAPSHOT"
    id("maven-publish")
}

// Convert Maven version (e.g., 26.1-snapshot-4) to Fabric version (e.g., 26.1-alpha.4)
// Mojang uses "snapshot" in Maven artifacts but "alpha" in the internal version ID
val fabricGameVersion: String = stonecutter.current.project.replace("-snapshot-", "-alpha.")

val javaVersionConverter: JavaVersionConverter = JavaVersionConverter(sc.current.parsed)
val supportedVersions = SupportedVersions(rootProject.file("supportedVersions.json"))

version = Versioning.getModVersion(::findProperty) + "+" + supportedVersions.getDisplayVersion(stonecutter.current.project)
group = property("maven_group").toString()

base {
    archivesName.set("${property("archives_base_name")}-${property("mod_loader")}")
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // Loom adds the essential maven repositories automatically.
}

sourceSets {
    create("common")
    named("main") {
        compileClasspath += sourceSets["common"].output
        runtimeClasspath += sourceSets["common"].output
    }
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

// Wire common into client (created by splitEnvironmentSourceSets above)
sourceSets.named("client") {
    compileClasspath += sourceSets["common"].output
    runtimeClasspath += sourceSets["common"].output
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.project}")
    // No mappings needed for unobfuscated Minecraft 26.1+
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // Common source set (version-independent shared code)
    "commonCompileOnly"("org.jspecify:jspecify:1.0.0")
    "commonCompileOnly"("org.slf4j:slf4j-api:2.0.16")
    "commonCompileOnly"("com.google.code.gson:gson:2.11.0")
    "commonCompileOnly"("com.google.guava:guava:33.4.0-jre")

    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    val minecraftConstraint = supportedVersions.getFabricVersionConstraint(stonecutter.current.project) {
        it.replace("-snapshot-", "-alpha.")
    }
    inputs.property("version", project.version)
    inputs.property("minecraft_version", minecraftConstraint)
    inputs.property("java_version", javaVersionConverter.getJavaVersionInt())

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to minecraftConstraint,
            "java_version" to javaVersionConverter.getJavaVersionInt()
        )
    }
    filesMatching("armor-hider.mixins.json") {
        expand(
            "java_version" to javaVersionConverter.getJavaVersionString(),
            "mixin_string" to MainMixins(sc.current.parsed).toString()
        )
    }
}

tasks.named<ProcessResources>("processClientResources") {
    val clientMixin = ClientMixins(sc.current.parsed)
    inputs.property("java_version", javaVersionConverter.getJavaVersionString())
    inputs.property("mixin_string", clientMixin.toString())
    inputs.property("options_screen_mixin_string", clientMixin.getScreenMixinString())

    filesMatching("armor-hider.client.mixins.json") {
        expand(
            "java_version" to javaVersionConverter.getJavaVersionString(),
            "mixin_string" to clientMixin.toString(),
            "options_screen_mixin_string" to clientMixin.getScreenMixinString()
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

    from(sourceSets["common"].output)

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

sourceSets.named("test") {
    compileClasspath += sourceSets["common"].output
    runtimeClasspath += sourceSets["common"].output
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
