package de.zannagh.armorhider.util;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.CompatFlags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MixinUtil {

    public static List<String> getMixinClassesWherePresent(String packageName, List<String> expectedClasses) {
        var list = new ArrayList<String>();
        for (String mixin : expectedClasses) {
            String mixinClassName = packageName + "." + mixin;
            String resourcePath = mixinClassName.replace('.', '/') + ".class";
            if (MixinUtil.class.getClassLoader().getResource(resourcePath) != null) {
                ArmorHider.LOGGER.debug("Applying present mixin: '{}'.", mixinClassName);
                list.add(mixin);
            } else {
                ArmorHider.LOGGER.debug("Skipping missing mixin: '{}'.", mixinClassName);
            }
        }

        return list;
    }

    /**
     * Sets compat flags by probing for mod presence via resource checks (never {@code Class.forName}).
     * Each probe first checks the exact class, then falls back to checking whether the mod's
     * package directory exists (2nd and 3rd dot-separated segments, e.g. {@code bernie/geckolib}
     * from {@code software.bernie.geckolib.renderer.GeoArmorRenderer}).
     *
     * @param cl the classloader to probe (usually the MixinPlugin's own classloader)
     */
    public static void setCompatFlags(ClassLoader cl) {
        CompatFlags.ET_LOADED = isModPresent(cl, "dev.kikugie.elytratrims.ep.ETClientEntrypoint");
        CompatFlags.GECKOLIB_LOADED = isModPresent(cl, "software.bernie.geckolib.renderer.GeoArmorRenderer");
        CompatFlags.FA_LOADED = isModPresent(cl, "net.kenddie.fantasyarmor.FantasyArmor");
        CompatFlags.WFGM_LOADED = isModPresent(cl, "com.wildfire.render.GenderArmorLayer");
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
}
