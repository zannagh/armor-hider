package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.util.MixinUtil;import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    
    private static final String PACKAGE = "de.zannagh.armorhider.mixin.client";
    private static final String[] GENERIC_MIXINS = new String[]{
            "hand.ItemEntityRendererMixin",
            "hand.ItemInHandLayerMixin",
            "hand.OffHandRenderMixin"
    };
    
    private static final String[] ABOVE_1_21_1_MIXINS = new String[]{
            "bodyKneesAndToes.EquipmentRenderMixin",
            "bodyKneesAndToes.ArmorFeatureRenderMixin"
    };
    
    private static final String[] ABOVE_1_21_1_NEOFORGE_MIXINS = new String[]{
            "bodyKneesAndToes.EquipmentRenderColorMixin"
    };

    private static final String[] BELOW_1_21_1_MIXINS = new String[]{
            "bodyKneesAndToes.HumanoidArmorLayerMixin"
    };
    
    private static final String[] AT_OR_ABOVE_1_21_9_MIXINS = new String[]{
            "hand.ItemRenderStateMixin",
            "hand.SubmitNodeCollectorMixin"
    };
    
    private static final String[] AT_OR_ABOVE_1_21_9_NEOFORGE_MIXINS = new String[]{
            "bodyKneesAndToes.NeoForgeArmorColorMixin"
    };
    
    private static final String[] BELOW_1_21_9_MIXINS = new String[]{
            "hand.ModelPartMixin"
    };
    
    private static final String[] BELOW_1_21_9_NEOFORGE_MIXINS = new String[]{
            "hand.ItemRendererMixin"
    };
    
    private static final String[] AT_OR_ABOVE_1_20_5_MIXINS_FABRIC = new String[]{
            "networking.ClientPacketListenerMixin"
    };
    
    private static final String[] BELOW_1_20_5_MIXINS = new String[]{
            "networking.ClientPlayNetworkHandlerMixin"
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
        
        //? if > 1.21.1 {
        mixinsToAdd.addAll(List.of(ABOVE_1_21_1_MIXINS));
        //? if neoforge
        //mixinsToAdd.addAll(List.of(ABOVE_1_21_1_NEOFORGE_MIXINS));
        //?}
        
        //? if >= 1.21.9 {
        mixinsToAdd.addAll(List.of(AT_OR_ABOVE_1_21_9_MIXINS));
        //? if neoforge
        //mixinsToAdd.addAll(List.of(AT_OR_ABOVE_1_21_9_NEOFORGE_MIXINS));
        //?}
        //? if < 1.21.9 {
        /*mixinsToAdd.addAll(List.of(BELOW_1_21_9_MIXINS));
        //? if neoforge
        //mixinsToAdd.addAll(List.of(BELOW_1_21_9_NEOFORGE_MIXINS));
        *///?}
        
        //? if >= 1.20.5 {
        //? if fabric
        mixinsToAdd.addAll(List.of(AT_OR_ABOVE_1_20_5_MIXINS_FABRIC));
        //?}
        //? if < 1.20.5 {
        /*mixinsToAdd.addAll(List.of(BELOW_1_20_5_MIXINS));
        *///?}
        
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
