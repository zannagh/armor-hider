package de.zannagh.armorhider;

/**
 * Lightweight compat flags set during mixin plugin load — before MC classes are available.
 * This class must NOT import any Minecraft classes to avoid early class loading.
 *
 * <p>Flags are set via {@link de.zannagh.armorhider.util.MixinUtil#setCompatFlags}
 * using resource-based probing (never {@code Class.forName}) to avoid premature
 * class loading that breaks other mods' mixins.</p>
 */
public final class CompatFlags {

    public static boolean ET_LOADED = false;
    public static boolean FA_LOADED = false;
    public static boolean GECKOLIB_LOADED = false;
    public static boolean WFGM_LOADED = false;

    private CompatFlags() {}
}
