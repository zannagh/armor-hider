package de.zannagh.armorhider.api.compat;

import de.zannagh.armorhider.log.EnrichedLogger;
import it.unimi.dsi.fastutil.Hash;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lightweight compat flags set during mixin plugin load — before MC classes are available.
 * This class must NOT import any Minecraft classes to avoid early class loading.
 *
 * <p>Flags are set via {@link #setCompatFlagsByResourceProbing(ClassLoader)} during mixin-plugin load
 * (resource-based probing only), and may be gap-filled later via {@link #setCompatFlags(ClassLoader)}
 * (which can use {@code Class.forName(..., false, ...)}).</p>
 *
 * <p><b>Note:</b> Iris must not be reached through the {@code Class.forName} path at mixin time — loading
 * it early breaks client startup on NeoForge. It is only resource-probed early and initialised later.</p>
 */
public final class CompatManager {

    /** Standalone logger (no ArmorHider/Minecraft dependency) so this stays mixin-load safe. */
    private static final EnrichedLogger LOG = new EnrichedLogger(LoggerFactory.getLogger("Armor Hider - Compat"));

    private static final EnumSet<CompatFlags> COMPAT_FLAGS = EnumSet.noneOf(CompatFlags.class);

    /**
     * The subset of {@link #COMPAT_FLAGS} that was detected by the mixin-safe resource probe (never
     * {@code Class.forName}). Kept separate from the {@link #setCompatFlags} class-load gap-fill so the
     * smoke consistency check ({@link #resourceProbingGaps}) can verify the resource probe alone detected
     * every mod that is actually present.
     */
    public static final EnumSet<CompatFlags> RESOURCE_PROBED_FLAGS = EnumSet.noneOf(CompatFlags.class);

    private static final HashMap<CompatFlags, HashSet<Supplier<CompatInitializationResult>>> INITIALIZATIONS = new HashMap<>();

    private static boolean compatFlagsEnsured;

    private CompatManager() {}

    // region Low-level presence probing

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
        } catch (ClassNotFoundException | LinkageError e) {
            // ClassNotFoundException: class absent. LinkageError (NoClassDefFoundError,
            // UnsupportedClassVersionError, VerifyError, …): class found but unlinkable
            try {
                return isModPresent(cl, name);
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    /**
     * Checks whether a mod is present without loading any classes.
     * <ol>
     *   <li>Probes for the exact {@code .class} resource.</li>
     *   <li>Falls back to checking the class's own package directory (every segment before the
     *       last), so a mod whose entrypoint class was renamed but still lives in the same package
     *       is still detected. Best-effort: a jar without explicit directory entries won't expose the
     *       package dir as a resource, so this can under-report; the primary probe covers the normal case.</li>
     * </ol>
     */
    public static boolean isModPresent(ClassLoader cl, String className) {
        if (cl.getResource(className.replace('.', '/') + ".class") != null) {
            return true;
        }
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            String packageProbe = className.substring(0, lastDot).replace('.', '/') + "/";
            try {
                return cl.getResources(packageProbe).hasMoreElements();
            } catch (IOException e) {
                LOG.debug("Failed to probe package resource '{}'.", packageProbe, e);
                return false;
            }
        }
        return false;
    }

    // endregion

    // region Flag detection passes

    public static void setCompatFlagsByResourceProbing() {
        setCompatFlagsByResourceProbing(CompatManager.class.getClassLoader());
    }

    public static void setCompatFlagsByResourceProbing(ClassLoader classLoader) {
        for (var flag : CompatFlags.values()) {
            if (flag.isAvailable(string -> isModPresent(classLoader, string))) {
                RESOURCE_PROBED_FLAGS.add(flag);
                setCompatFlag(flag);
            }
        }
    }

    public static void setCompatFlags() {
        setCompatFlags(CompatManager.class.getClassLoader());
    }

    /**
     * Gap-fills compat flags with a {@code Class.forName} presence check (via {@link #classExists}), for
     * mods the mixin-safe resource probe missed. Skips flags already set so it never re-loads an
     * already-detected mod. Runs at client init, NOT at mixin time, so the class loads it triggers are
     * safe (see {@link #setCompatFlagsByResourceProbing} for the mixin-time, class-load-free path).
     *
     * @param cl the classloader to probe (usually the MixinPlugin's own classloader)
     */
    public static void setCompatFlags(ClassLoader cl) {
        if (compatFlagsEnsured) {
            return;
        }
        for (var compat : CompatFlags.values()) {
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
        compatFlagsEnsured = true;
    }

    /**
     * Smoke/diagnostic consistency check: for every compat flag whose mod is <b>definitively</b> present —
     * verified here by {@code Class.forName}, safe to do post-boot unlike at mixin time — assert the
     * mixin-safe resource probe ({@link #RESOURCE_PROBED_FLAGS}) also detected it. A mod that loads but
     * was not resource-probed means the class-load-free probing path has a gap and compat gating would
     * silently misfire in production. Returns the set of such gaps ({@code empty} = probing is sound).
     *
     * @param cl the classloader to verify against (the mod's own classloader)
     */
    public static EnumSet<CompatFlags> resourceProbingGaps(ClassLoader cl) {
        EnumSet<CompatFlags> gaps = EnumSet.noneOf(CompatFlags.class);
        for (var flag : CompatFlags.values()) {
            boolean presentByClassLoad = flag.isAvailable(name -> {
                try {
                    Class.forName(name, false, cl);
                    return true;
                } catch (Throwable ignored) {
                    return false;
                }
            });
            if (presentByClassLoad && !RESOURCE_PROBED_FLAGS.contains(flag)) {
                gaps.add(flag);
            }
        }
        return gaps;
    }

    // endregion

    // region Flag queries

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

    /** Whether the given mod was detected as present (distinct from the low-level {@link #isModPresent(ClassLoader, String)} probe). */
    public static boolean isPresent(CompatFlags flag) {
        return requiresCompatTo(flag);
    }

    /** True when any accessory provider is present — gates the accessory-related config UI. */
    public static boolean anyAccessoryProviderLoaded() {
        return requiresCompatToAnyOf(CompatFlags.CURIOS, CompatFlags.TRINKETS, CompatFlags.ACCESSORIES, CompatFlags.ARTIFACTS);
    }

    // endregion

    // region Deferred initialization

    /**
     * Runs the initialization routine for the given compat initializers.
     * @param initializers The initializers for specific flag, {@link CompatInitializer}.
     * @return The results of the initialization routine.
     */
    public static HashMap<CompatFlags, HashSet<CompatInitializationResult>> runInitializationRoutine(CompatInitializer... initializers) {
        setCompatFlags();

        for (CompatInitializer initializer : initializers) {
            addInitializer(initializer);
        }

        return initializeCompats();
    }

    /**
     * Assigns an initializer for a compat flag.
     * It is safe to call this method even when the compat flag is not present. <br/>
     * This method will internally call {@link #setCompatFlags()} if the compat flags have not been ensured yet,
     * so it must be safe to classload at the point in time when initializers are added and this method is called.<br/><br/>
     * It is safe to add an initializer to a mod flag that is not present at runtime. In this case, adding
     * the initializer will be ignored.
     * @param initializer The initializer to assign
     */
    public static void addInitializer(CompatInitializer initializer) {
        assignInitialization(initializer.targetFlag(), initializer::init);
    }

    /**
     * Assigns an initialization method.<br/>
     * <br/>
     * This method will internally call {@link #setCompatFlags()} if the compat flags have not been ensured yet,
     * so it must be safe to classload at the point in time when initializers are added and this method is called.<br/><br/>
     * It is safe to add an initializer to a mod flag that is not present at runtime. In this case, adding
     * the initializer will be ignored.
     * @param flag The flag to assign an initialization method to.
     */
    public static void assignInitialization(CompatFlags flag, Supplier<CompatInitializationResult> initialization) {
        if (!compatFlagsEnsured) {
            setCompatFlags();
        }

        if (!flag.needsInitialization()) {
            return;
        }

        if (!isPresent(flag)) {
            return;
        }

        INITIALIZATIONS.computeIfAbsent(flag, key -> new HashSet<>()).add(initialization);
    }

    /**
     * Initializes compat flags that require initialization
     * @return A map of each compat flag to the results of its initializers ({@link CompatInitializationResult#MISSING} when a flag needs initialization but none was registered).
     */
    public static HashMap<CompatFlags, HashSet<CompatInitializationResult>> initializeCompats() {
        HashMap<CompatFlags, HashSet<CompatInitializationResult>> compatInitializations = new HashMap<>();
        for (var presentCompat : COMPAT_FLAGS) {
            if (!presentCompat.needsInitialization()) {
                continue;
            }
            var initializers = INITIALIZATIONS.get(presentCompat);
            if (initializers == null) {
                compatInitializations.put(presentCompat, new HashSet<>(List.of(CompatInitializationResult.MISSING)));
                continue;
            }
            var results = new HashSet<CompatInitializationResult>();
            for (var initializer : initializers) {
                results.add(initializer.get());
            }
            compatInitializations.put(presentCompat, results);
        }
        return compatInitializations;
    }

    // endregion

    private static void setCompatFlag(CompatFlags flag) {
        if (!COMPAT_FLAGS.contains(flag)) {
            COMPAT_FLAGS.add(flag);
        }
    }
}
