package de.zannagh.armorhider.mixin;

import de.zannagh.armorhider.util.MixinUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "de.zannagh.armorhider.mixin.client";

    // Mixins listed unconditionally — Stonecutter source guards strip classes for
    // incompatible versions, so getMixinClassesWherePresent filters them automatically.
    private static final String[] MIXINS = new String[]{
            // Always present
            "LivingEntityMixin",
            "lang.ClientLanguageMixin",
            "hand.ItemEntityRendererMixin",
            "hand.ItemInHandLayerMixin",
            "hand.OffHandRenderMixin",
            // Guarded by //? if >= 1.21.9 in source
            "OptionsScreenMixin",
            "SkinOptionsMixin",
            "bodyKneesAndToes.ArmorFeatureRenderMixin",
            "bodyKneesAndToes.EquipmentRenderMixin",
            "hand.ItemRenderStateMixin",
            "hand.SubmitNodeCollectorMixin",
            "cape.CapeRenderMixin",
            "cape.ElytraRenderMixin",
            "head.CustomHeadLayerMixin",
            "head.SkullBlockRenderMixin",
            // Guarded by //? if >= 1.21 && < 1.21.4 in source
            "bodyKneesAndToes.HumanoidArmorLayerMixin",
            // Guarded by //? if < 1.21.9 in source
            "hand.ModelPartMixin",
            "bodyKneesAndToes.NeoForgeArmorColorMixin",
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
        var mixinsToAdd = new ArrayList<>(List.of(MIXINS));
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
