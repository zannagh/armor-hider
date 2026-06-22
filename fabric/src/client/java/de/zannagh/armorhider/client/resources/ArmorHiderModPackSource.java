package de.zannagh.armorhider.client.resources;

import de.zannagh.armorhider.ArmorHider;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Mounts the armor-hider mod jar as a built-in client resource pack when
 * fabric-resource-loader-v0 is absent. With Fabric API present this is a no-op.
 */
public final class ArmorHiderModPackSource implements RepositorySource {

    private static final String PACK_ID = "armor-hider:builtin";

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("armor-hider");
        if (container.isEmpty()) {
            return;
        }
        List<java.nio.file.Path> roots = container.get().getRootPaths();
        for (var root : roots) {
            if (!Files.exists(root.resolve("assets"))) {
                continue;
            }
            var locationInfo = new PackLocationInfo(
                    PACK_ID,
                    Component.literal("armor-hider"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );
            var resourcesSupplier = new PathPackResources.PathResourcesSupplier(root);
            var metadata = new Pack.Metadata(
                    Component.literal("armor-hider built-in resources"),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlagSet.of(),
                    List.of()
            );
            var selectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, true);
            consumer.accept(new Pack(locationInfo, resourcesSupplier, metadata, selectionConfig));
            ArmorHider.LOGGER.info("Mounted built-in resource pack from {}", root);
            return;
        }
    }
}
