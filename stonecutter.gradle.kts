plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.140" apply false
    // Supplies the JaCoCo tooling classpath used by the `aggregatedCoverage` JacocoReport below.
    id("jacoco")
}

// The root project defines no repositories of its own (subprojects do), but the jacoco plugin's
// `:jacocoAnt` tooling configuration must resolve org.jacoco.ant from somewhere.
repositories {
    mavenCentral()
}

stonecutter active "fabric-26.2" /* [SC] DO NOT EDIT */

stonecutter parameters {
    replacements.string(current.parsed >= "1.21.11") { replace("ResourceLocation", "Identifier") }
    replacements.string(false) { replace("packet.getIdentifier()", "packet.getResourceLocation()") }
    replacements.string(current.parsed <= "1.21.8") { replace("AvatarRenderState", "PlayerRenderState") }
    replacements.string(current.parsed < "1.21.11") { replace("net.minecraft.client.renderer.rendertype.RenderType", "net.minecraft.client.renderer.RenderType") }
    replacements.string(current.parsed < "1.21.11") { replace("Lnet/minecraft/client/renderer/rendertype/RenderType", "Lnet/minecraft/client/renderer/RenderType")}
    replacements.string(current.parsed < "1.21.11") { replace("net.minecraft.client.model.player.PlayerModel", "net.minecraft.client.model.PlayerModel") }
    replacements.string(current.parsed < "26.1-0.snapshot.11") { replace("net.minecraft.client.renderer.state.level.CameraRenderState", "net.minecraft.client.renderer.state.CameraRenderState") }
    replacements.string(current.parsed <= "26.1-1.pre.1") { replace("net.minecraft.client.gui.GuiGraphicsExtractor", "net.minecraft.client.gui.GuiGraphics") }
    replacements.string(current.parsed < "1.21") { replace("net.minecraft.client.gui.screens.options.OptionsSubScreen", "net.minecraft.client.gui.screens.OptionsSubScreen") }
    replacements.string(current.parsed < "1.21.9") { replace(".setScreenAndShow(", ".setScreen(") }
    replacements.string(current.parsed <= "1.21.1") { replace("WingsLayer", "ElytraLayer") }
    replacements.string(current.parsed < "1.21") { replace("net.minecraft.client.gui.screens.options.SkinCustomizationScreen", "net.minecraft.client.gui.screens.SkinCustomizationScreen")}
    replacements.string(current.parsed < "1.21") { replace("net.minecraft.client.gui.screens.options.OptionsScreen", "net.minecraft.client.gui.screens.OptionsScreen")}
    // 26.3-snapshot-3 extracted the render pipeline API out of blaze3d into the new
    // com.mojang.renderpearl module (same types/methods, new package) and changed
    // BakedQuad.MaterialInfo's boolean shade() accessor to Direction shadeDirectionOverride()
    // (same constructor slot, value passed straight through).
    replacements.string(current.parsed >= "26.3-0.snapshot.3") { replace("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline") }
    replacements.string(current.parsed >= "26.3-0.snapshot.3") { replace("com.mojang.blaze3d.pipeline.DepthStencilState", "com.mojang.renderpearl.api.pipeline.DepthStencilState") }
    replacements.string(current.parsed >= "26.3-0.snapshot.3") { replace("com.mojang.blaze3d.pipeline.ColorTargetState", "com.mojang.renderpearl.api.pipeline.ColorTargetState") }
    replacements.string(current.parsed >= "26.3-0.snapshot.3") { replace("info.shade()", "info.shadeDirectionOverride()") }

}

tasks.register("stageArtifacts") {
    group = "build"
    description = "Builds all loader variants and copies unique artifacts to staging/"

    // Stonecutter loader variants live under :fabric:<mc> / :neoforge:<mc>. The Paper
    // plugin is NOT a stonecutter branch: it is a single sibling subproject (":paper",
    // the :smoke pattern) producing one jar that covers every supported MC version, so
    // it has to be matched by exact path rather than by prefix.
    val paperProjectPath = ":paper"
    val loaderProjects = allprojects.filter {
        it.path.startsWith(":fabric:") || it.path.startsWith(":neoforge:") || it.path == paperProjectPath
    }
    loaderProjects.forEach { dependsOn("${it.path}:build") }

    val staging = rootProject.file("staging")

    doLast {
        staging.deleteRecursively()
        staging.mkdirs()

        // Union of every game version any stonecutter variant supports. The Paper plugin
        // is version-agnostic, so this is what it publishes against.
        val allGameVersions = loaderProjects
            .filter { it.path != paperProjectPath }
            .flatMap {
                it.findProperty("game_versions")?.toString()
                    ?.split(",")?.map { v -> v.trim() }?.filter { v -> v.isNotEmpty() }
                    ?: emptyList()
            }
            .distinct()

        val versionMap = mutableMapOf<String, MutableMap<String, List<String>>>()
        for (proj in loaderProjects) {
            val isPaper = proj.path == paperProjectPath
            // A missing display_version on the Paper project would silently drop the
            // plugin from versions.json (empty publish matrix, no error) - fail loudly.
            val rawDisplayVersion = proj.findProperty("display_version")?.toString()
            if (rawDisplayVersion == null && isPaper) {
                error("Missing display_version for $paperProjectPath - expected the literal string \"paper\"")
            }
            val displayVersion = rawDisplayVersion ?: continue
            val declaredGameVersions = proj.findProperty("game_versions")?.toString()
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            if (declaredGameVersions == null && !isPaper) {
                error("Missing game_versions for ${proj.name}")
            }
            val gameVersions = declaredGameVersions ?: allGameVersions
            // ":paper" -> "paper"; ":fabric:1.21.4" -> "fabric" (variant names are "<loader>-<mc>").
            val loader = proj.name.substringBefore("-")
            val existing = versionMap.getOrPut(loader) { mutableMapOf() }
                .putIfAbsent(displayVersion, gameVersions)
            if (existing != null && existing.sorted() != gameVersions.sorted()) {
                error("Conflicting game_versions for $loader/$displayVersion: $existing vs $gameVersions")
            }

            proj.layout.buildDirectory.dir("libs").get().asFile.let { libsDir ->
                libsDir.listFiles()
                    ?.filter { it.extension == "jar" && !it.name.endsWith("-sources.jar") }
                    ?.forEach { jar ->
                        val target = staging.resolve(jar.name)
                        if (!target.exists()) jar.copyTo(target)
                    }
            }
        }

        val versionMapJson = versionMap.entries.sortedBy { it.key }.joinToString(",\n  ", "{\n  ", "\n}") { (loader, groups) ->
            val groupsJson = groups.entries.sortedBy { it.key }.joinToString(",\n    ", "{\n    ", "\n  }") { (display, versions) ->
                val versionsJson = versions.sorted().joinToString("\", \"", "[\"", "\"]")
                "\"$display\": $versionsJson"
            }
            "\"$loader\": $groupsJson"
        }
        staging.resolve("versions.json").writeText(versionMapJson)

        val files = staging.listFiles()?.filter { it.extension == "jar" }?.sortedBy { it.name } ?: emptyList()
        println("Staged ${files.size} artifacts:")
        files.forEach { println("  ${it.name} (${it.length() / 1024} KB)") }
    }
}

// Tier-3 client/server-spawning smoke suite (see CONTRIBUTING.md > Testing). Kept OUT of `test`/`check`
// so ordinary builds never boot a Minecraft client; invoke deliberately with `./gradlew smokeTest`.
// A distinct task name gives it its own Develocity build-scan timeline, separate from unit tests.
tasks.register("smokeTest") {
    group = "verification"
    description = "Client/server-spawning smoke + E2E suite (Tier 3, drives the in-game FCGT tests)."
    dependsOn(":smoke:smokeTest")
}

// Repo-wide Tier-1 coverage merged into ONE report (build/reports/jacoco/aggregate/). Sums the
// active common variant + paper. Only the ACTIVE common variant is included on purpose: every
// stonecutter variant carries an identical copy of the classes, so aggregating all of them would
// multiply the denominator and make the percentage meaningless.
run {
    val coverageProjects = listOfNotNull(
        project(":paper"),
        stonecutter.current?.project?.let { project(":common:$it") }
    )
    // Evaluate the target projects first so their `main` source sets + test tasks are resolvable here.
    coverageProjects.forEach { evaluationDependsOn(it.path) }

    tasks.register<JacocoReport>("aggregatedCoverage") {
        group = "verification"
        description = "Merged Tier-1 (unit) coverage across paper + the active common variant."
        coverageProjects.forEach { p ->
            val testTask = p.tasks.named<Test>("test")
            dependsOn(testTask)
            // executionData(Test) tolerates a run that produced no .exec (skipped/no-source).
            executionData(testTask.get())
            val main = p.extensions.getByType(SourceSetContainer::class.java).getByName("main")
            sourceDirectories.from(main.allSource.srcDirs)
            // Exclude mixin packages: they only execute in a live client/server, never in the JVM unit
            // tests, so counting them would just depress the denominator. Kept in sync with the per-module
            // exclude in multiloader-common.gradle.kts. To be covered later.
            classDirectories.from(main.output.classesDirs.asFileTree.matching { exclude("**/mixin/**") })
        }
        // JaCoCo's HTML formatter overwrites but never deletes, so a package dropped from the report
        // (e.g. the excluded mixins) would leave a stale page until a clean. Wipe the dir first.
        doFirst { delete(reports.html.outputLocation) }
        reports {
            xml.required.set(true)
            html.required.set(true)
            html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate/html"))
            xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregate/jacocoAggregate.xml"))
        }
    }
}
