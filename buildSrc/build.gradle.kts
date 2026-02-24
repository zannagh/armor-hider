plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.quiltmc.org/repository/release/")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.8.3")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("net.fabricmc:fabric-loom:${property("loom_version")}")
}