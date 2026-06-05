package de.zannagh.armorhider.client.api.render;

/**
 * Version-independent interface for applying render modifications.
 * <p>
 * Returned by {@link ScopeContext#renderModificationApi()} and available from
 * the scope API via {@link ArmorHiderRenderingScopeApi#getActiveScope(RenderScope)}.
 * All methods are pass-through safe: if no modification is active, original values are returned unchanged.
 * <p>
 * Render type methods use {@code Object} to avoid game-version dependencies in the API.
 * Callers should cast via {@code instanceof RenderType}.
 *
 * @since 0.12.0
 */
public interface RenderModificationApi {

    int applyArmorTransparency(int originalColor);

    int applyTransparencyFromWhite(int original);

    float getTransparencyAlpha();

    boolean getHasFoil(boolean original);

    int modifyRenderPriority(int value);

    Object getTranslucentArmorRenderType(Object textureIdentifier, Object originalRenderType);

    Object getTranslucentRenderType(Object textureIdentifier, Object originalRenderType);

    Object getTrimRenderLayer(boolean decal, Object originalRenderType);

    Object getTranslucentItemRenderType(Object originalRenderType);

    Object getSkullRenderLayer(Object textureIdentifier, Object originalRenderType);
}
