package de.zannagh.armorhider.client;

import de.zannagh.armorhider.ArmorHider;
import de.zannagh.armorhider.util.MixinUtil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "de.zannagh.armorhider.client.mixin";
    private static boolean forgeEnvironment = false;

    private static final String[] MIXINS = new String[]{
            "PlayerMixin",
            "GameRendererMixin",
            "DevSkinMixin",
            "TitleScreenSmokeMixin",
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
            // Transitional armor/elytra transparency for the 1.21.2/1.21.3 render-state era
            // (EquipmentRenderMixin is >= 1.21.4). Stonecutter-gated; dropped where absent.
            "bodyKneesAndToes.EquipmentLayerRendererLegacyMixin",
            "hand.ItemRenderStateMixin",
            "hand.SubmitNodeCollectorMixin",
            "cape.ElytraRenderMixin",
            "head.CustomHeadLayerMixin",
            "head.SkullBlockRenderMixin",
            // Fabric-only — programmatic mod-resource-pack registration when fabric-resource-loader-v0 is absent
            "resources.PackRepositoryMixin",
            // All versions — Stonecutter guards per version range
            "bodyKneesAndToes.HumanoidArmorLayerMixin",
            "bodyKneesAndToes.HumanoidArmorLayerRenderMixin",
            "cape.CapeRenderMixin",
            // Guarded by //? if < 1.21.9 in source
            "hand.ModelPartMixin",
            "hand.ItemRendererMixin",
            "networking.ClientPacketListenerMixin",
            "networking.ClientPlayNetworkHandlerMixin",
            "OptionsMixin",
            "MinecraftClientMixin",
            "PlayerModelMixin",
            // Compat — @Pseudo, auto-skipped if target mod absent
            "compat.wildfiregender.GenderArmorLayerMixin",
            "compat.wildfiregender.GenderLegacyLayerMixin",
            "compat.geckolib.GeckoLibArmorMixin",
            "compat.waveycapes.WaveyCapesMixin",
            "compat.emf.EmfModelPartMixin",
            "compat.emf.EmfModelPartRootMixin"
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
        if (forgeEnvironment) {
            mixinsToAdd.remove("bodyKneesAndToes.HumanoidArmorLayerRenderMixin");
            ArmorHider.LOGGER.info("Removed HumanoidArmorLayerRenderMixin — Forge-patched bytecode is incompatible with @WrapOperation targets.");
        }
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
        if (MixinUtil.isClassAvailableWithoutLoading(MixinPlugin.class.getClassLoader(), "cpw.mods.modlauncher.Launcher")) {
            forgeEnvironment = true;
            ArmorHider.LOGGER.info("Detected Forge/Sinytra Connector environment.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }
}
