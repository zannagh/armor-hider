package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import org.jspecify.annotations.Nullable;

//? if firstperson {
import dev.tr7zw.firstperson.FirstPersonModelCore;
import dev.tr7zw.firstperson.access.LivingEntityRenderStateAccess;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//?}

/**
 * Compatibility for First Person Model (tr7zw), which draws the local player's <em>body</em> in first
 * person by extracting a normal {@code AvatarRenderState} for the camera entity and appending it to the
 * regular entity render list. Identity capture and our per-layer render scopes therefore work exactly as
 * they do in third person — nothing to fix there.
 * <p>
 * What does need handling is that FPM cancels several layer submits at their {@code HEAD} for that camera
 * entity. Our scope-enter hooks also sit at {@code HEAD} and, being ordered ahead of FPM's, run first: we
 * enter the scope, FPM cancels the submit, and our {@code @At("RETURN")} exit never fires — the scope then
 * leaks into the rest of the frame and colours unrelated rendering. Rather than entering and hoping for an
 * exit, the predicates below mirror FPM's cancel conditions so we never enter a scope for a submit that is
 * about to be discarded. Skipping is visually free: the submit produces nothing either way.
 */
public final class FirstPersonCompat {

    /** Pitch above which FPM considers the player to be looking down at their own body. */
    private static final float LOOKING_DOWN_X_ROT = 30.0F;

    private FirstPersonCompat() {
    }

    /**
     * Whether {@code state} is the first-person body FPM renders for the camera entity.
     */
    public static boolean isFirstPersonBody(@Nullable Object state) {
        //? if firstperson {
        if (state == null
                || !ArmorHiderRenderTypes.areFirstPersonGuardsEnabled()
                || !CompatManager.requiresCompatTo(CompatFlags.FIRST_PERSON_MODEL)) {
            return false;
        }
        return state instanceof LivingEntityRenderStateAccess access && access.isCameraEntity();
        //?} else {
        /*return false;
        *///?}
    }

    /**
     * Mirrors {@code dev.tr7zw.firstperson.mixins.CustomHeadLayerMixin}: the head layer is cancelled
     * unconditionally for the camera entity (the head model is hidden in first person anyway).
     */
    public static boolean suppressesHeadLayer(@Nullable Object state) {
        return record(isFirstPersonBody(state));
    }

    /**
     * Mirrors FPM's wings-layer mixin: wings are cancelled for the camera
     * entity only while it is visually swimming, so elytra hiding keeps working the rest of the time.
     */
    public static boolean suppressesWingsLayer(@Nullable Object state) {
        //? if firstperson {
        return record(isFirstPersonBody(state)
                && state instanceof HumanoidRenderState humanoidState
                && humanoidState.isVisuallySwimming);
        //?} else {
        /*return false;
        *///?}
    }

    /**
     * Mirrors {@code dev.tr7zw.firstperson.mixins.HeldItemFeatureRendererMixin}: the third-person held-item
     * layer is cancelled for the camera entity when both arms are hidden and the player is not looking down.
     */
    public static boolean suppressesHeldItemLayer(@Nullable Object state) {
        //? if firstperson {
        if (!isFirstPersonBody(state) || !(state instanceof LivingEntityRenderStateAccess access)) {
            return false;
        }
        if (!access.hideLeftArm() || !access.hideRightArm()) {
            return false;
        }
        // Inlines FPM's LogicHandler#lookingDown(state) — `dynamicHandsEnabled() && state.xRot > 30`.
        // Calling that overload directly would put a Minecraft type (LivingEntityRenderState) in an FPM
        // signature we touch, and the loader-side compile classpath carries the *unremapped* FPM jar, where
        // MC types resolve to the other namespace. FPM's own types are fine — only MC types are the problem,
        // so getLogicHandler() (returns LogicHandler) is safe while lookingDown(state) is not.
        return record(state instanceof HumanoidRenderState humanoidState
                && !(FirstPersonModelCore.instance.getLogicHandler().dynamicHandsEnabled()
                        && humanoidState.xRot > LOOKING_DOWN_X_ROT));
        //?} else {
        /*return false;
        *///?}
    }

    /** Counts every suppression so a game test can prove the guards are live rather than dormant. */
    private static boolean record(boolean suppressed) {
        if (suppressed) {
            ArmorHiderRenderTypes.recordFirstPersonLayerGuard();
        }
        return suppressed;
    }
}
