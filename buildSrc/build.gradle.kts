plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.9.1")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("net.fabricmc:fabric-loom:${property("loom_version")}")
}