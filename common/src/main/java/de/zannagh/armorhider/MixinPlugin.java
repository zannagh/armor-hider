package de.zannagh.armorhider;

import de.zannagh.armorhider.util.MixinUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "de.zannagh.armorhider.mixin";
    private static final String[] GENERIC_MIXINS = new String[]{
            "networking.MinecraftServerMixin",
            "networking.ServerLoginMixin",
    };

    private static final String[] AT_OR_ABOVE_1_20_5_MIXINS_FABRIC = new String[]{
            "networking.CustomPayloadCodecMixin",
            "networking.ServerGamePacketListenerMixin"
    };

    private static final String[] BELOW_1_20_5_MIXINS = new String[]{
            "networking.ServerPlayNetworkHandlerMixin"
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

        var list = new ArrayList<String>();

        var mixinsToAdd = new ArrayList<String>();
        mixinsToAdd.addAll(List.of(GENERIC_MIXINS));

        //? if >= 1.20.5 && fabric 
        mixinsToAdd.addAll(List.of(AT_OR_ABOVE_1_20_5_MIXINS_FABRIC));
        //? if < 1.20.5 
        //mixinsToAdd.addAll(List.of(BELOW_1_20_5_MIXINS));

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
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }
}
