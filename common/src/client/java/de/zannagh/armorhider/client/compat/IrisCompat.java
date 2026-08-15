package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatInitializationResult;
import de.zannagh.armorhider.api.compat.CompatInitializer;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.irisshaders.iris.api.v0.*;

public class IrisCompat implements CompatInitializer {

    public IrisCompat() {}

    public static void registerPipelines() {
        //? if >= 1.21.5 {
        var api = IrisApi.getInstance();
        if (api.getMinorApiRevision() < 3) {
            ArmorHider.LOGGER.warn("Iris API revision {} does not support pipeline registration, skipping",
                    api.getMinorApiRevision());
            return;
        }
        // 26.3-snapshot-3 moved RenderPipeline into the new com.mojang.renderpearl module, but the
        // latest Iris (1.11.1+mc26.2) still exposes assignPipeline(blaze3d.RenderPipeline). Pipeline
        // registration stays dormant on 26.3 until an Iris build targeting the renderpearl API ships.
        //? if < 26.3-0.snapshot.3 {
        for (var pipeline : ArmorHiderRenderTypes.pipelines()) {
            api.assignPipeline(pipeline, IrisProgram.ENTITIES_TRANSLUCENT);
        }
        // Also register the depth-writing armor pipeline(s) used under an active shaderpack, and wire
        // the shaderpack-active check so translucent armor switches to the depth-writing type only
        // while a pack is loaded (fixes the body reading see-through under shaders at grazing angles).
        for (var pipeline : ArmorHiderRenderTypes.shaderDepthPipelines()) {
            api.assignPipeline(pipeline, IrisProgram.ENTITIES_TRANSLUCENT);
        }
        ArmorHiderRenderTypes.setShaderPackActiveCheck(() -> {
            try {
                return IrisApi.getInstance().isShaderPackInUse();
            } catch (Throwable t) {
                return false;
            }
        });
        ArmorHider.LOGGER.debug("Registered custom pipelines with Iris");
        //?} else {
        /*ArmorHider.LOGGER.debug("Iris pipeline registration skipped: pinned Iris predates the 26.3 renderpearl API");
        *///?}
        //?}
    }

    @Override
    public CompatFlags targetFlag() {
        return CompatFlags.IRIS;
    }

    @Override
    public CompatInitializationResult init() {
        try {
            IrisCompat.registerPipelines();
            return CompatInitializationResult.SUCCESS;
        } catch (Exception e) {
            ArmorHider.LOGGER.warn("Failed to register pipelines with Iris", e);
            return CompatInitializationResult.failure(e.getMessage());
        }
    }
}
