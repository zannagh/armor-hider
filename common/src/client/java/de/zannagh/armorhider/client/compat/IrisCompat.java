package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.irisshaders.iris.api.v0.*;

public final class IrisCompat {

    private IrisCompat() {}

    public static void registerPipelines() {
        //? if >= 1.21.5 {
        /*var api = IrisApi.getInstance();
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
        ArmorHider.LOGGER.debug("Registered custom pipelines with Iris");
        //?} else {
        /^ArmorHider.LOGGER.debug("Iris pipeline registration skipped: pinned Iris predates the 26.3 renderpearl API");
        ^///?}
        *///?}
    }
}
