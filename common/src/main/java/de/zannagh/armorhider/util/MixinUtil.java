package de.zannagh.armorhider.util;

import de.zannagh.armorhider.log.EnrichedLogger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class MixinUtil {

    /**
     * A separate logger from Armor Hider's default logger.
     * This logger is used to prevent early class loads.
     */
    public static final EnrichedLogger LOG = new EnrichedLogger(LoggerFactory.getLogger("Armor Hider - Mixins"));

    public static List<String> getMixinClassesWherePresent(String packageName, List<String> expectedClasses) {
        var list = new ArrayList<String>();
        for (String mixin : expectedClasses) {
            String mixinClassName = packageName + "." + mixin;
            String resourcePath = mixinClassName.replace('.', '/') + ".class";
            if (MixinUtil.class.getClassLoader().getResource(resourcePath) != null) {
                LOG.debug("Applying present mixin: '{}'.", mixinClassName);
                list.add(mixin);
            } else {
                LOG.debug("Skipping missing mixin: '{}'.", mixinClassName);
            }
        }

        return list;
    }
}
