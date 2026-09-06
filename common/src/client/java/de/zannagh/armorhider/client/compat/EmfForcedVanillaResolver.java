package de.zannagh.armorhider.client.compat;

import traben.entity_model_features.models.animation.state.EMFEntityRenderState;
import traben.entity_model_features.models.animation.state.EMFState;

/**
 * Access to EMF's "is this entity forced to render the vanilla model?" flag.
 * <p>
 * Targets EMF 3.3+, where the flag lives at
 * {@code EMFState.isEntityForcedToVanillaModel(EMFEntityRenderState)} and the current render state
 * comes from the static {@code EMFState.state()} (may be {@code null} between frames). EMF is a hard
 * compile-time dependency here; the sole caller is a {@code @Pseudo} EMF mixin that only applies when
 * EMF is present at runtime, so this links the 3.3 API directly.
 */
public final class EmfForcedVanillaResolver {

    private EmfForcedVanillaResolver() {
    }

    /**
     * Whether EMF currently forces the vanilla model for the entity being rendered. Returns
     * {@code false} when there is no active render state.
     */
    public static boolean isForced() {
        EMFEntityRenderState state = EMFState.state();
        return state != null && EMFState.isEntityForcedToVanillaModel(state);
    }
}
