package de.zannagh.armorhider.client.render;

/**
 * Test-only capture point for which player-model path EMF took, used by the #217 EMF/Fresh
 * Animations smoke test to assert that hiding the body forces the clean vanilla model.
 * <p>
 * A custom CEM player model (e.g. a Fresh Animations player add-on) bakes its arm offset into the
 * mesh, so the arm/torso seam is not visible in any {@code ModelPart} pivot - it only shows up in
 * <em>which model is drawn</em>. When the body is hidden the fix forces EMF to the vanilla model
 * ({@link #PATH_FORCED_VANILLA}); without it EMF draws its own custom model ({@link #PATH_CUSTOM}).
 * The mixin on EMF's model part records the last path taken; the smoke test asserts on it. Disabled
 * (and effectively free) outside the smoke test.
 */
public final class AhArmProbe {

    public static final String PATH_NONE = "none";
    public static final String PATH_CUSTOM = "custom_model";
    public static final String PATH_FORCED_VANILLA = "forced_vanilla";
    public static final String PATH_SEAM_COMPOSITE = "seam_composite";

    private static volatile boolean enabled = false;
    private static volatile String lastPath = PATH_NONE;
    private static final java.util.concurrent.atomic.AtomicLong equipmentFallbacks =
            new java.util.concurrent.atomic.AtomicLong();

    private AhArmProbe() {
    }

    public static void enable() {
        enabled = true;
        lastPath = PATH_NONE;
        equipmentFallbacks.set(0);
    }

    public static void disable() {
        enabled = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void recordCustomModel() {
        lastPath = PATH_CUSTOM;
    }

    public static void recordForcedVanilla() {
        lastPath = PATH_FORCED_VANILLA;
    }

    public static void recordSeamComposite() {
        lastPath = PATH_SEAM_COMPOSITE;
    }

    public static void recordEquipmentFallback() {
        equipmentFallbacks.incrementAndGet();
    }

    public static long equipmentFallbackCount() {
        return equipmentFallbacks.get();
    }

    public static String lastPath() {
        return lastPath;
    }
}
