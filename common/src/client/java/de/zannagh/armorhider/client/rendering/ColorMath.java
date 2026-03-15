package de.zannagh.armorhider.client.rendering;

//? if >= 1.21.4
import net.minecraft.util.ARGB;

/**
 * Version-independent color math utilities for transparency application.
 * Isolates all Stonecutter version conditionals for ARGB operations.
 */
public final class ColorMath {

    private ColorMath() {}

    public static int withAlpha(int originalColor, int alpha) {
        //? if >= 1.21.4
        return ARGB.color(alpha, ARGB.red(originalColor), ARGB.green(originalColor), ARGB.blue(originalColor));
        //? if < 1.21.4 {
        /*int red = (originalColor >> 16) & 0xFF;
        int green = (originalColor >> 8) & 0xFF;
        int blue = originalColor & 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
        *///?}
    }

    public static int whiteWithAlpha(int alpha) {
        //? if >= 1.21.4
        return ARGB.color(alpha, 255, 255, 255);
        //? if < 1.21.4
        //return (alpha << 24) | (255 << 16) | (255 << 8) | 255;
    }
}
