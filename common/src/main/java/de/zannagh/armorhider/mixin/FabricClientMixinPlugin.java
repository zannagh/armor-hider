package de.zannagh.armorhider.mixin;

import java.util.ArrayList;
import java.util.List;

public abstract class FabricClientMixinPlugin extends ArmorHiderMixinPlugin {
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
            "compat.emf.EmfModelPartRootMixin",
            "compat.curios.CuriosLayerMixin",
            "compat.trinkets.TrinketRendererMixin",
            "compat.accessories.AccessoriesRenderLayerMixin",
            "compat.elytratrims.ETElytraTrimSubmitMixin"
    };

    @Override
    public String getPackage() {
        return "de.zannagh.armorhider.client.mixin";
    }

    @Override
    public List<String> getExpectedMixins() {
        var mixinsToAdd = new ArrayList<>(List.of(MIXINS));
        if (syntraConnected) {
            mixinsToAdd.remove("bodyKneesAndToes.HumanoidArmorLayerRenderMixin");
            LOG.info("Removed HumanoidArmorLayerRenderMixin — Forge-patched bytecode is incompatible with @WrapOperation targets.");
        }
        return mixinsToAdd;
    }
}
