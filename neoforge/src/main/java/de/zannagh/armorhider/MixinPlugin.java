package de.zannagh.armorhider;

import de.zannagh.armorhider.api.compat.CompatManager;
import de.zannagh.armorhider.util.MixinUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "de.zannagh.armorhider.net.mixin";
    private static final String[] GENERIC_MIXINS = new String[]{
            "MinecraftServerMixin",
            "ServerLoginMixin"
    };
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        var mixinsToAdd = new ArrayList<String>();
        mixinsToAdd.addAll(List.of(GENERIC_MIXINS));
        return MixinUtil.getMixinClassesWherePresent(PACKAGE, mixinsToAdd);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void onLoad(String mixinPackage) {
        // Resource-probe ONLY here — this runs at mixin-plugin load, before other mods' mixins apply.
        // The Class.forName variant (setCompatFlags) would eagerly load compat mod classes (e.g.
        // GeckoLib's GeoArmorRenderer, EMF model classes) whose type hierarchy references
        // net.minecraft.client.model.Model, loading Model too early and breaking EMF's MixinModel with
        // MixinTargetAlreadyLoadedException. The class-load probe runs later, safely, from
        // AhClientCompatManager.init() at client init.
        CompatManager.setCompatFlagsByResourceProbing(MixinPlugin.class.getClassLoader());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }
}
