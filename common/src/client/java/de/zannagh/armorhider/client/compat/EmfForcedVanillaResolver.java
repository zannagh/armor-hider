package de.zannagh.armorhider.client.compat;

import java.lang.reflect.Method;

/**
 * Version-tolerant access to EMF's "is this entity forced to render the vanilla model?" flag.
 * <p>
 * The accessor moved between EMF majors, so a single Armor Hider jar cannot hard-link either
 * signature - it resolves the available one reflectively (once) and caches it:
 * <ul>
 *     <li><b>EMF 3.2.x</b>: {@code EMFAnimationEntityContext.isEntityForcedToVanillaModel()} - zero-arg static.</li>
 *     <li><b>EMF 3.3.x</b>: {@code EMFState.isEntityForcedToVanillaModel(EMFEntityRenderState)}, where the
 *         current render state comes from the static {@code EMFState.state()} (may be {@code null}).</li>
 * </ul>
 * The 3.3.x API is preferred; if it is absent we fall back to the 3.2.x method. If neither resolves
 * (EMF missing, or another API break) this degrades to {@code false} so rendering is never aborted.
 */
public final class EmfForcedVanillaResolver {

    private EmfForcedVanillaResolver() {
    }

    private static volatile boolean resolved = false;

    // Latched off after a resolved accessor throws at invoke time, so a persistent failure costs one
    // exception rather than one per render call (the caller is on the model-render hot path).
    private static volatile boolean broken = false;

    // EMF 3.3.x: EMFState.state() -> EMFEntityRenderState, then EMFState.isEntityForcedToVanillaModel(state).
    private static Method stateAccessor;
    private static Method forcedWithState;

    // EMF 3.2.x: EMFAnimationEntityContext.isEntityForcedToVanillaModel() (zero-arg).
    private static Method forcedNoArg;

    /**
     * Whether EMF currently forces the vanilla model for the entity being rendered. Returns
     * {@code false} when the flag cannot be read on the installed EMF version.
     */
    public static boolean isForced() {
        if (!resolved) {
            resolve();
        }
        if (broken) {
            return false;
        }
        try {
            if (stateAccessor != null && forcedWithState != null) {
                Object state = stateAccessor.invoke(null);
                if (state == null) {
                    return false;
                }
                return (Boolean) forcedWithState.invoke(null, state);
            }
            if (forcedNoArg != null) {
                return (Boolean) forcedNoArg.invoke(null);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // A resolved accessor that throws at invoke time would otherwise pay this exception cost
            // every render call - latch it off so later frames short-circuit to a cheap "not forced".
            broken = true;
        }
        return false;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        try {
            // Prefer the EMF 3.3.x home for the flag.
            Class<?> emfState =
                    Class.forName("traben.entity_model_features.models.animation.state.EMFState");
            Class<?> renderState =
                    Class.forName("traben.entity_model_features.models.animation.state.EMFEntityRenderState");
            stateAccessor = emfState.getMethod("state");
            forcedWithState = emfState.getMethod("isEntityForcedToVanillaModel", renderState);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            stateAccessor = null;
            forcedWithState = null;
        }
        if (forcedWithState == null) {
            try {
                // Fall back to the EMF 3.2.x zero-arg accessor.
                Class<?> context =
                        Class.forName("traben.entity_model_features.models.animation.EMFAnimationEntityContext");
                forcedNoArg = context.getMethod("isEntityForcedToVanillaModel");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                forcedNoArg = null;
            }
        }
        resolved = true;
    }
}
