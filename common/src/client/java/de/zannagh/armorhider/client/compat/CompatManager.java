package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;

public class CompatManager {

    public static void init() {
        if (ArmorHiderClient.IRIS_LOADED) {
            ArmorHider.LOGGER.info("Registering Iris compatibility...");
            initIrisCompat();
        }

        if (ArmorHiderClient.EMF_LOADED) {
            ArmorHider.LOGGER.info("Registering EMF compatibility...");
            initEmfCompat();
        }

        // ElytraTrims compat lives inline in ArmorHiderElytraRenderer.intercept (the ET_LOADED
        // branch). Earlier we replaced the elytra renderer with EMPTY_RENDERER here, but that
        // also disabled the hide-cancel path at 0% opacity — and the inline branch already
        // collapses to "full hide or pass through" when ET is loaded, which is what we want.
    }

    private static void initEmfCompat() {
        try {
            EmfCompat.register();
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
        }
    }

    private static void initIrisCompat() {
        try {
            IrisCompat.registerPipelines();
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register pipelines with Iris", e);
        }
    }
}
