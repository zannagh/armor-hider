package de.zannagh.armorhider.client.render.utils;

import de.zannagh.armorhider.client.api.AhColorTransformer;

/**
 * Built-in {@link AhColorTransformer}. Uses straight ARGB arithmetic via {@link ColorMath} so the
 * bit-twiddling is centralized and version-aware in one place.
 */
public final class DefaultColorTransformer implements AhColorTransformer {

    private static final DefaultColorTransformer INSTANCE = new DefaultColorTransformer();

    public static DefaultColorTransformer getInstance() {
        return INSTANCE;
    }

    private DefaultColorTransformer() {}

    @Override
    public int applyTransparency(int color, float transparency) {
        int alpha = Math.round(transparency * 255);
        return ColorMath.withAlpha(color, alpha);
    }

    @Override
    public int scaleAlpha(int color, float transparency) {
        int origAlpha = (color >> 24) & 0xFF;
        int newAlpha = Math.round(transparency * origAlpha);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }

    @Override
    public int whiteWithTransparency(float transparency) {
        int alpha = Math.round(transparency * 255);
        return ColorMath.whiteWithAlpha(alpha);
    }
}
