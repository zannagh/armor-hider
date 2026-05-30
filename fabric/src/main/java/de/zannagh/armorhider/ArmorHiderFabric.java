package de.zannagh.armorhider;

import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.api.ArmorHiderInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Comparator;

public class ArmorHiderFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ArmorHider.init();
        invokeEntrypoints();
    }

    private static void invokeEntrypoints() {
        FabricLoader.getInstance()
                .getEntrypoints(ArmorHiderInitializer.ENTRYPOINT_KEY, ArmorHiderInitializer.class)
                .stream()
                .sorted(Comparator.comparingInt(ArmorHiderInitializer::priority).reversed())
                .forEach(initializer -> initializer.onInitializeArmorHider(ArmorHiderApi.getInstance()));
    }
}
