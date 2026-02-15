plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT"
    id("maven-publish")
}

val javaVersionConverter: JavaVersionConverter = JavaVersionConverter(sc.current.parsed)

version = Versioning.getGitVersion(::findProperty, stonecutter.current.project)
group = property("maven_group").toString()

base {
    archivesName.set(property("archives_base_name").toString())
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

afterEvaluate {
    sourceSets.named("common") {
        compileClasspath += configurations["minecraftCommonNamedCompile"]
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
    }
}

// Wire common into client (created by splitEnvironmentSourceSets above)
sourceSets.named("client") {
    compileClasspath += sourceSets["common"].output
    runtimeClasspath += sourceSets["common"].output
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
    val mainMixin = MainMixins(sc.current.parsed)
    inputs.property("version", project.version)
    inputs.property("minecraft_version", stonecutter.current.project)
    inputs.property("java_version", javaVersionConverter.getJavaVersionInt())

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to stonecutter.current.project,
            "java_version" to javaVersionConverter.getJavaVersionInt()
        )
    }
    filesMatching("armor-hider.mixins.json") {
        expand(
            "java_version" to javaVersionConverter.getJavaVersionString(),
            "mixin_string" to mainMixin.toString()
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

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersionConverter.getJavaVersionInt())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersionConverter.getJavaVersionInt()))
    }
    withSourcesJar()
    sourceCompatibility = javaVersionConverter.getJavaVersion()
    targetCompatibility = javaVersionConverter.getJavaVersion()
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