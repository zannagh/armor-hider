package de.zannagh.armorhider;

import de.zannagh.armorhider.api.CompatFlags;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Lightweight compat flags set during mixin plugin load — before MC classes are available.
 * This class must NOT import any Minecraft classes to avoid early class loading.
 *
 * <p>Flags are set via {@link CompatManager#setCompatFlags()}
 * using class loading without initialization resource-based probing (never {@code Class.forName}) to avoid premature
 * class loading that breaks other mods' mixins.</p>
 *
 * <p>
 *     Remarks:
 * </p>
 * <remarks>
 *     <ul>
 *         <li>Iris should not be loaded via this as it breaks game client startup on NeoForge</li>
 *     </ul>
 * </remarks>
 */
public final class CompatManager {

    public static EnumSet<CompatFlags> COMPAT_FLAGS = EnumSet.noneOf(CompatFlags.class);

    private CompatManager() {}

    /**
     * Probes whether a class exists without instantiating it (and loading imports). <br/>
     * Meant to probe whether other mods are present without causing mod loading exceptions.
     * @param name The name of the class to probe.
     * @return True when the class exists, otherwise false.
     */
    public static boolean classExists(String name) {
        return classExists(name, CompatManager.class.getClassLoader());
    }

    public static boolean classExists(String name, ClassLoader cl) {
        try {
            Class.forName(name, false, cl);
            return true;
        } catch (ClassNotFoundException e) {
            try {
                return isModPresent(cl, name);
            }
            catch (Exception ignored) {
                return false;
            }
        }
    }

    public static void setCompatFlags() {
        setCompatFlags(CompatManager.class.getClassLoader());
    }

    public static void setCompatFlagsByResourceProbing(ClassLoader classLoader) {
        for (var flag : CompatFlags.values()) {
            if (flag.isAvailable(string -> isModPresent(classLoader, string))) {
                setCompatFlag(flag);
            }
        }
    }

    public static void setCompatFlagsByResourceProbing() {
        setCompatFlagsByResourceProbing(CompatManager.class.getClassLoader());
    }

    /**
     * Sets compat flags by probing for mod presence via resource checks (never {@code Class.forName}).
     * Each probe first checks the exact class, then falls back to checking whether the mod's
     * package directory exists (2nd and 3rd dot-separated segments, e.g. {@code geckolib/renderer}
     * from {@code com.geckolib.renderer.GeoArmorRenderer}).
     *
     * @param cl the classloader to probe (usually the MixinPlugin's own classloader)
     */
    public static void setCompatFlags(ClassLoader cl) {
        var allCompats = CompatFlags.values();
        for (var compat : allCompats) {
            // Resource probing (setCompatFlagsByResourceProbing) runs first, at mixin-plugin load, and
            // already flagged everything it could see without loading a class. Skip those here so the
            // Class.forName pass never re-loads an already-detected mod — this matters for Iris, whose
            // early class load breaks client startup on NeoForge. Only gap-fill the mods resource
            // probing missed.
            if (COMPAT_FLAGS.contains(compat)) {
                continue;
            }
            if (compat.isAvailable(name -> classExists(name, cl))) {
                setCompatFlag(compat);
            }
        }
    }

    public static boolean requiresCompatTo(CompatFlags flags) {
        if (flags == CompatFlags.FANTASY_ARMOR) {
            return COMPAT_FLAGS.contains(CompatFlags.FANTASY_ARMOR) && COMPAT_FLAGS.contains(CompatFlags.GECKO_LIB);
        }
        return COMPAT_FLAGS.contains(flags);
    }

    public static boolean requiresCompatToAnyOf(CompatFlags... flags) {
        for (CompatFlags flag : flags) {
            if (COMPAT_FLAGS.contains(flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a mod is present without loading any classes.
     * <ol>
     *   <li>Probes for the exact {@code .class} resource.</li>
     *   <li>Falls back to checking the package directory formed by the 2nd and 3rd
     *       dot-separated segments (the org/mod identifier that is unlikely to change
     *       even when individual classes are renamed).</li>
     * </ol>
     */
    public static boolean isModPresent(ClassLoader cl, String className) {
        if (cl.getResource(className.replace('.', '/') + ".class") != null) {
            return true;
        }
        String[] parts = className.split("\\.");
        if (parts.length >= 3) {
            String packageProbe = parts[1] + "/" + parts[2] + "/";
            try {
                return cl.getResources(packageProbe).hasMoreElements();
            } catch (IOException e) {
                ArmorHider.LOGGER.debug("Failed to probe package resource '{}'.", packageProbe, e);
                return false;
            }
        }
        return false;
    }

    /**
     * Assigns an initialization method
     * @param flag
     * @return
     */
    public static void assignInitialization(CompatFlags flag, Supplier<Boolean> initialization) {
        if (!flag.needsInitialization()) {
            return;
        }
        INITIALIZATIONS.put(flag, initialization);
    }

    private static final HashMap<CompatFlags, Supplier<Boolean>> INITIALIZATIONS = new HashMap<>();


    public static HashMap<CompatFlags, Boolean> initializeCompats() {
        HashMap<CompatFlags, Boolean> compatInitializations = new HashMap<>();
        for (var presentCompat : COMPAT_FLAGS) {
            if (INITIALIZATIONS.containsKey(presentCompat) && presentCompat.needsInitialization()) {
                compatInitializations.put(presentCompat, INITIALIZATIONS.get(presentCompat).get());
            }
        }
        return compatInitializations;
    }

    /** True when any accessory provider is present — gates the accessory-related config UI. */
    public static boolean anyAccessoryProviderLoaded() {
        return requiresCompatToAnyOf(CompatFlags.CURIOS, CompatFlags.TRINKETS, CompatFlags.ACCESSORIES, CompatFlags.ARTIFACTS);
    }

    private static void setCompatFlag(CompatFlags flag){
        if (!COMPAT_FLAGS.contains(flag)) {
            COMPAT_FLAGS.add(flag);
        }
    }
}
