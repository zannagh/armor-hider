//? if >= 1.21.5 {
package de.zannagh.armorhider.client.compat.iris;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.client.rendering.ArmorHiderRenderTypes;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

public final class IrisCompat {

    private IrisCompat() {}

    public static void registerPipelines() {
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
    }
}
//?}
