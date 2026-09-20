package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;

/**
 * Shared body of the ElytraTrims (ET) trim-submit wraps.
 * <p>
 * ET draws every elytra decorator through one helper that funnels into
 * {@code OrderedSubmitNodeCollector.submitModel(...)}. Which class owns that helper changed with ET
 * 4.9.0, so two {@code @Pseudo} mixins target the old and the new one; both delegate here so the actual
 * behaviour lives in exactly one place.
 */
public final class ElytraTrimsFade {

    private ElytraTrimsFade() {
    }

    /**
     * Scales an ET decorator's ARGB alpha by the active elytra transparency, keeping RGB so dyed
     * color/pattern decorators keep their hue. Returns {@code color} unchanged when no elytra
     * modification is active. Also bumps the diagnostic counters the ElytraTrims smoke asserts on:
     * SEEN on every call (our wrap is bound and ET is decorating), FADE only when we actually scaled.
     */
    public static int fadeTrimColor(int color) {
        ArmorHiderRenderTypes.recordElytraTrimSeen();
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ELYTRA);
        if (ctx.isEmpty() || !ctx.needsModification()) {
            return color;
        }
        var modApi = ctx.renderModificationApi();
        ArmorHiderRenderTypes.recordElytraTrimFade();
        return modApi.colors().scaleAlpha(color, modApi.getTransparencyAlpha());
    }

    /**
     * Defensive guard for ET builds before 4.9.0.
     * <p>
     * Those read the wrong local slot out of vanilla {@code renderLayers} - the {@code order} parameter,
     * into which Armor Hider legitimately writes {@link RenderModifications#ELYTRA_RENDER_PRIORITY},
     * instead of {@code outlineColor} - and hand that value on as the outline color. Vanilla turns any
     * non-zero outline color into the tinted color of a full-bright outline submit, so the trim renders
     * as {@code ARGB(0, 0, 0, 100)}: pure blue. ET 4.9.0 reads the correct slot and this is a no-op
     * there.
     * <p>
     * The condition is deliberately narrow: exactly our own render-order constant, and only while an
     * elytra modification is active - i.e. provably our mis-delivered value. {@code ARGB(0, 0, 0, 100)}
     * is never a legitimate outline color, so real glow outlines are left alone.
     */
    public static int sanitizeOutline(int outline) {
        if (outline == RenderModifications.ELYTRA_RENDER_PRIORITY
                && AhRenderManagementApi.hasScopeModification(RenderScope.ELYTRA)) {
            return 0;
        }
        return outline;
    }
}
