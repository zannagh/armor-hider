package de.zannagh.armorhider.client.render;

import de.zannagh.armorhider.configuration.EmfHiddenModelMode;

/**
 * Publishes the {@link EmfHiddenModelMode} resolved for the entity EMF is currently rendering, so the
 * per-part {@code EmfModelPartMixin} can react to it. EMF's {@code registerVanillaModelCondition}
 * callback (which has the entity in hand) sets this just before the model renders; the mixin reads it.
 * <p>
 * {@link EmfHiddenModelMode#VANILLA} is handled by EMF forcing the whole vanilla model, so the mixin
 * only needs this for {@link EmfHiddenModelMode#VANILLA_SEAMS}. Defaults to {@link EmfHiddenModelMode#KEEP}.
 */
public final class EmfHiddenModeContext {

    private static final ThreadLocal<EmfHiddenModelMode> CURRENT =
            ThreadLocal.withInitial(() -> EmfHiddenModelMode.KEEP);

    private EmfHiddenModeContext() {
    }

    public static void set(EmfHiddenModelMode mode) {
        CURRENT.set(mode == null ? EmfHiddenModelMode.KEEP : mode);
    }

    public static EmfHiddenModelMode current() {
        return CURRENT.get();
    }
}
