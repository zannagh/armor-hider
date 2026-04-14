package de.zannagh.armorhider;

/**
 * Lightweight compat flags set during mixin plugin load — before MC classes are available.
 * This class must NOT import any Minecraft classes to avoid early class loading.
 *
 * <p>Flags are set by each loader's {@code MixinPlugin.onLoad()} using the loader's
 * native mod detection API (FabricLoader / classExists on NeoForge).
 * {@link de.zannagh.armorhider.client.ArmorHiderClient} reads these with a
 * {@code classExists} fallback for safety.</p>
 */
public final class CompatFlags {

    public static boolean ET_LOADED = false;
    public static boolean FA_LOADED = false;
    public static boolean GECKOLIB_LOADED = false;
    public static boolean WFGM_LOADED = false;

    private CompatFlags() {}
}
