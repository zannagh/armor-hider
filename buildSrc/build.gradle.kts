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
    implementation("dev.kikugie:stonecutter:0.8.3")
    implementation("com.google.code.gson:gson:2.13.1")
}