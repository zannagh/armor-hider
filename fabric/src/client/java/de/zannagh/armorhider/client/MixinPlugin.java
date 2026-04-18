package de.zannagh.armorhider.client;

import de.zannagh.armorhider.util.MixinUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "de.zannagh.armorhider.client.mixin";

    private static final String[] MIXINS = new String[]{
            "PlayerMixin",
            "GameRendererMixin",
            // All versions — Stonecutter guards per version range
            "EntityRenderDispatcherMixin",
            "LivingEntityMixin",
            "LivingEntityRendererMixin",
            "LivingEntityRenderStateMixin",
            "lang.ClientLanguageMixin",
            "hand.ItemEntityRendererMixin",
            "hand.ItemInHandLayerMixin",
            "hand.OffHandRenderMixin",
            // Guarded by //? if >= 1.21.9 in source
            "OptionsScreenMixin",
            "SkinCustomizationScreenMixin",
            "SkinCustomizationScreenLegacyMixin",
            "bodyKneesAndToes.EquipmentRenderMixin",
            "hand.ItemRenderStateMixin",
            "hand.SubmitNodeCollectorMixin",
            "cape.ElytraRenderMixin",
            "head.CustomHeadLayerMixin",
            "head.SkullBlockRenderMixin",
            // All versions — Stonecutter guards per version range
            "bodyKneesAndToes.HumanoidArmorLayerMixin",
            "cape.CapeRenderMixin",
            // Guarded by //? if < 1.21.9 in source
            "hand.ModelPartMixin",
            "bodyKneesAndToes.EquipmentRenderColorMixin",
            "hand.ItemRendererMixin",
            "networking.ClientPacketListenerMixin",
            "networking.ClientPlayNetworkHandlerMixin",
            "OptionsMixin",
            "PlayerModelMixin",
            // Compat — @Pseudo, auto-skipped if target mod absent
            "compat.wildfiregender.GenderArmorLayerMixin",
            "compat.geckolib.GeckoLibArmorMixin"
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
        MixinUtil.setCompatFlags(MixinPlugin.class.getClassLoader());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }
}
