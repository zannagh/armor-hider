package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.ArmorHiderClient;
import de.zannagh.armorhider.client.api.AhRenderInterceptionRegistryApi;
import de.zannagh.armorhider.client.common.RenderScope;

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

        if (ArmorHiderClient.ET_LOADED) {
            ArmorHider.LOGGER.info("Registering ElytraTrims compatibility to suppress Elytra render changes...");
            AhRenderInterceptionRegistryApi.suppressRenderInterceptionConditionally(RenderScope.ELYTRA, eval -> true); // Suppress custom rendering when ElytraTrims is present for any elytra renderers.
        }

    }

    private static void initEmfCompat() {
        try {
            de.zannagh.armorhider.client.compat.emf.EmfCompat.register();
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register vanilla model condition with EMF", e);
        }
    }

    private static void initIrisCompat() {
        try {
            de.zannagh.armorhider.client.compat.iris.IrisCompat.registerPipelines();
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register pipelines with Iris", e);
        }
    }
}
