package de.zannagh.armorhider.util;

import de.zannagh.armorhider.ArmorHider;

import java.util.ArrayList;
import java.util.List;

public final class MixinUtil {
    
    public static List<String> getMixinClassesWherePresent(String packageName, List<String> expectedClasses) {
        var list = new ArrayList<String>();
        for (String mixin : expectedClasses) {
            String mixinClassName = packageName + "." + mixin;
            try {
                Class.forName(mixinClassName, false, MixinUtil.class.getClassLoader());
                ArmorHider.LOGGER.debug("Applying present mixin: '{}'.", mixinClassName);
                list.add(mixin);
            } catch (ClassNotFoundException ignored) {
                ArmorHider.LOGGER.debug("Skipping missing mixin: '{}'.", mixinClassName);
            }
        }

        return list;
    }
}
