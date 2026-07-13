package de.zannagh.armorhider.client.api;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolves the {@link RenderType}s Armor Hider uses to render hidden / translucent armor, items
 * and skulls. Implement this interface and pass it to
 * {@link AhRenderer#registerRenderTypeFactory(AhRenderTypeFactory)} to override the render types
 * used by a specific renderer — useful when a compat layer needs custom render pipelines
 * (e.g. shaders that need a particular blend mode or atlas).
 * <p>
 * If a renderer has no custom factory installed, the built-in
 * {@link de.zannagh.armorhider.client.render.rendertype.RenderTypeFactory} is used, which produces
 * translucent render types with depth-writing disabled (so semi-transparent armor does not occlude
 * water, ice, stained glass, etc. behind it).
 *
 * @since 0.12.0
 */
public interface AhRenderTypeFactory {

    /**
     * @param texture armor layer texture, already resolved (with any per-player vanilla-fallback applied).
     * @return the {@link RenderType} used to draw a translucent armor layer.
     */
    RenderType getTranslucentArmorRenderType(ResourceLocation texture);

    /**
     * @param texture entity texture (skull, item-entity, etc.).
     * @return the {@link RenderType} used to draw a translucent entity texture.
     */
    RenderType getTranslucentEntityRenderType(ResourceLocation texture);

    /**
     * @return the {@link RenderType} used to draw a translucent armor trim. Trims share a single sheet,
     * so no texture parameter is needed.
     */
    RenderType getTranslucentArmorTrimRenderType(boolean decal);

    /**
     * @return the {@link RenderType} used to draw translucent block-item sheet items
     * (the offhand-banner / offhand-shield path on older MC versions).
     */
    RenderType getTranslucentItemSheetRenderType();
}
