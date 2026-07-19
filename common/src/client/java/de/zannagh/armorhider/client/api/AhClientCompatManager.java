package de.zannagh.armorhider.client.api;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.CompatManager;
import de.zannagh.armorhider.api.CompatFlags;
import de.zannagh.armorhider.client.compat.EmfCompat;
import de.zannagh.armorhider.client.compat.IrisCompat;

public final class AhClientCompatManager {

    public static void init() {
        // Safe to load compats again with classloading here.
        CompatManager.setCompatFlags();
        CompatManager.assignInitialization(CompatFlags.IRIS, AhClientCompatManager::initIrisCompat);
        CompatManager.assignInitialization(CompatFlags.ENTITY_MODEL_FEATURES, AhClientCompatManager::initEmfCompat);

        var inits = CompatManager.initializeCompats();
        for (var init : inits.entrySet()) {
            if (init.getValue() == false) {
                ArmorHider.LOGGER.warn("Failed to initialize compat: {}", init.getKey());
            }
        }
    }

    private static boolean initEmfCompat() {
        try {
            EmfCompat.register();
            return true;
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
            return false;
        }
    }

    private static boolean initIrisCompat() {
        try {
            IrisCompat.registerPipelines();
            return true;
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register pipelines with Iris", e);
            return false;
        }
    }
}
