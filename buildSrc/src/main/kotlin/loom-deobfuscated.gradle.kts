// Must set BEFORE applying Loom — Loom reads this eagerly during plugin application.
project.extra.set("fabric.loom.disableObfuscation", "true")

apply(plugin = "fabric-loom")

dependencies {
    "minecraft"("com.mojang:minecraft:${project.mcVersion}")
    "implementation"("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")
}
