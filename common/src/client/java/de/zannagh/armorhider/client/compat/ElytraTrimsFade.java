package de.zannagh.armorhider.client.compat;

import de.zannagh.armorhider.client.api.AhRenderManagementApi;
import de.zannagh.armorhider.client.common.RenderScope;
import de.zannagh.armorhider.client.render.RenderModifications;
import de.zannagh.armorhider.client.render.rendertype.ArmorHiderRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;

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
     * Substitutes Armor Hider's own translucent render type for the one ET submits the decorator on,
     * for the same atlas, while an elytra modification is active. Returns {@code renderType} unchanged
     * when no modification is active or the atlas cannot be recovered.
     * <p>
     * Scaling the ARGB alpha alone is not enough on the cutout era (1.21.9/1.21.10): ET draws its
     * decorators on a cutout type there, which discards the alpha instead of blending it, so the trim
     * stays measurably more solid than the wing it is supposed to fade with. Swapping the type is what
     * actually makes the two fade in lockstep, and it is safe: the atlas is the same, only the blend
     * and depth-write state change. On 4.9.0+ ET already picks a translucent type; substituting ours
     * keeps both eras on one code path and on the same depth-write policy as the base wing.
     *
     * @param renderType the type ET is about to submit on
     * @param uvSource   the submit's sprite - a {@code TextureAtlasSprite} on every version, reaching
     *                   us as the {@code UvMapping} interface on 26.3+; anything else leaves the type
     *                   untouched
     */
    public static RenderType fadeTrimRenderType(RenderType renderType, @Nullable Object uvSource) {
        if (!(uvSource instanceof TextureAtlasSprite sprite)) {
            return renderType;
        }
        var ctx = AhRenderManagementApi.getActiveScope(RenderScope.ELYTRA);
        if (ctx.isEmpty() || !ctx.needsModification()) {
            return renderType;
        }
        return ctx.renderModificationApi().renderTypes().getTranslucentEntityRenderType(sprite.atlasLocation());
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
