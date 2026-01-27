plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.8.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}