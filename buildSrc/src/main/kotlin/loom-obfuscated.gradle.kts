plugins {
    id("fabric-loom")
}

dependencies {
    "minecraft"("com.mojang:minecraft:${project.mcVersion}")
    "mappings"(loom.officialMojangMappings())
}
