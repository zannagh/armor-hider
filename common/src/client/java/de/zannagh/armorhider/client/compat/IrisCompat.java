package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.irisshaders.iris.api.v0.*;

public final class IrisCompat {

    private IrisCompat() {}

    public static void registerPipelines() {
        //? if >= 1.21.5 {
        var api = IrisApi.getInstance();
        if (api.getMinorApiRevision() < 3) {
            ArmorHider.LOGGER.warn("Iris API revision {} does not support pipeline registration, skipping",
                    api.getMinorApiRevision());
            return;
        }
        for (var pipeline : ArmorHiderRenderTypes.pipelines()) {
            api.assignPipeline(pipeline, IrisProgram.ENTITIES_TRANSLUCENT);
        }
        ArmorHider.LOGGER.debug("Registered custom pipelines with Iris");
        //?}
    }
}
