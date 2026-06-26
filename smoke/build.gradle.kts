plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    // Each row in this matrix forks gradle + boots MC — too expensive to run as part of
    // `./gradlew build`/`check`. Skip unless invoked explicitly (`./gradlew :smoke:test`,
    // an IDE test run that targets this task, or `-Psmoke.run`).
    val explicitlyRequested = project.hasProperty("smoke.run")
            || gradle.startParameter.taskNames.any {
                it == "test" || it.endsWith(":test") || it.endsWith(":smoke:test")
            }
    onlyIf("Smoke matrix is opt-in: pass -Psmoke.run or invoke :smoke:test directly") {
        explicitlyRequested
    }
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.timeout.test.default", "10m")
    // Show test logs in IDE/CI output instead of swallowing them.
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    listOf("smoke.only", "smoke.exclude", "smoke.compat", "smoke.phase", "smoke.delay.ms").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    // Repo root → tests fork ./gradlew from here.
    systemProperty("armorhider.repo.root", rootProject.projectDir.absolutePath)
}
