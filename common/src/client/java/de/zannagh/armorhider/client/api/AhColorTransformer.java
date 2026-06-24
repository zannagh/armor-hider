package de.zannagh.armorhider.client.api;

/**
 * Computes the color-space transformations Armor Hider applies when a slot is being rendered
 * translucently: overwriting the alpha channel, multiplying an existing alpha by a transparency
 * factor, and producing a translucent white.
 * <p>
 * The built-in {@link de.zannagh.armorhider.client.render.utils.DefaultColorTransformer} uses
 * straight ARGB arithmetic. Implement this interface and pass it to
 * {@link AhRenderModificationApi#registerColorTransformer(AhColorTransformer, int)} to override the
 * arithmetic — useful when a compat layer wants gamma-corrected blending, perceptual fades, or
 * shader-friendly color spaces. Lower priority values win, mirroring
 * {@link AhRenderModificationApi#registerRenderTypeFactory(AhRenderTypeFactory, int)}.
 * <p>
 * All implementations must treat {@code color} as packed ARGB ({@code 0xAARRGGBB}) and return the
 * same encoding. {@code transparency} is in {@code [0.0, 1.0]}.
 *
 * @since 0.12.0
 */
public interface AhColorTransformer {

    /**
     * Replace the alpha channel of {@code color} with {@code transparency * 255}, preserving the
     * RGB channels. Matches the historical {@code applyArmorTransparency} semantics — used when the
     * downstream renderer expects an absolute opacity rather than a multiplier.
     */
    int applyTransparency(int color, float transparency);

    /**
     * Multiply the existing alpha channel of {@code color} by {@code transparency}, preserving the
     * RGB channels. Used when the renderer already carries a meaningful alpha (e.g. tinted item
     * layers, pre-tinted {@code SubmitNode} color) and we want to compose our transparency on top
     * of the existing value.
     */
    int scaleAlpha(int color, float transparency);

    /**
     * Return packed white ARGB ({@code 0xAA_FF_FF_FF}) with the alpha set to {@code transparency *
     * 255}.
     */
    int whiteWithTransparency(float transparency);
}
