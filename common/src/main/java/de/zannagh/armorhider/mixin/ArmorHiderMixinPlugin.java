package de.zannagh.armorhider.mixin;

import de.zannagh.armorhider.api.compat.CompatFlags;
import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.log.EnrichedLogger;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class ArmorHiderMixinPlugin implements IMixinConfigPlugin {

    private boolean compatsProbed;

    /**
     * Whether the environment is Forge/Sinytra Connector.
     */
    protected static boolean syntraConnected = false;

    /**
     * A separate logger from Armor Hider's default logger.
     * This logger is used to prevent early class loads.
     */
    public static final EnrichedLogger LOG = new EnrichedLogger(LoggerFactory.getLogger("Armor Hider - Mixin"));

    public abstract String getPackage();

    public abstract List<String> getExpectedMixins();

    @Override
    public List<String> getMixins() {
        ensureCompatProbed();
        var list = new ArrayList<String>();
        for (String mixin : getExpectedMixins()) {
            String mixinClassName = getPackage() + "." + mixin;
            String resourcePath = mixinClassName.replace('.', '/') + ".class";
            if (ArmorHiderMixinPlugin.class.getClassLoader().getResource(resourcePath) != null) {
                LOG.debug("Applying present mixin: '{}'.", mixinClassName);
                list.add(mixin);
            } else {
                LOG.debug("Skipping missing mixin: '{}'.", mixinClassName);
            }
        }

        return list;
    }


    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void onLoad(String mixinPackage) {
        ensureCompatProbed();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    private void ensureCompatProbed() {
        if (compatsProbed) {
            return;
        }
        // Resource-probe only (no Class.forName) at mixin-plugin load.
        // This is safe to call multiple times.
        CompatManager.setCompatFlagsByResourceProbing(NeoForgeMixinPlugin.class.getClassLoader());
        if (CompatManager.isPresent(CompatFlags.SYNTRA)) {
            syntraConnected = true;
            LOG.info("Detected Forge/Sinytra Connector environment.");
        }
        compatsProbed = true;
    }
}
