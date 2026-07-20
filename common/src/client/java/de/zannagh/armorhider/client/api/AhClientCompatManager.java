package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.compat.EmfCompat;
import de.zannagh.armorhider.client.compat.IrisCompat;

public final class AhClientCompatManager {

    public static void init() {
        // Safe to load compats again with classloading here.
        var inits = CompatManager.runInitializationRoutine(new IrisCompat(), new EmfCompat());
        for (var init : inits.entrySet()) {
            var results = init.getValue();
            var failedResults = results.stream().filter(result -> !result.success()).toList();
            for (var failure : failedResults) {
                ArmorHider.LOGGER.warn("Failed to initialize compat for {}: {}", init.getKey(), failure.message());
                if (failure.isMissingInitializerResult()) {
                    throw new IllegalStateException("Compat requires initialization but it's missing.");
                }
            }
        }
    }
}
