package de.zannagh.armorhider.mixin;

import java.util.ArrayList;
import java.util.List;

public class NeoForgeClientMixinPlugin extends ArmorHiderMixinPlugin {

    protected boolean isServerDist;

    public NeoForgeClientMixinPlugin(boolean isServerDist) {
        this.isServerDist = isServerDist;
    }

    private static final String[] MIXINS = new String[]{
            // Always present
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
            // All versions — Stonecutter guards per version range
            "bodyKneesAndToes.HumanoidArmorLayerMixin",
            "bodyKneesAndToes.HumanoidArmorLayerRenderMixin",
            "cape.CapeRenderMixin",
            // Guarded by //? if < 1.21.9 in source
            "hand.ModelPartMixin",
            "hand.ItemRendererMixin",
            "bodyKneesAndToes.NeoForgeArmorColorMixin",
            "bodyKneesAndToes.NeoForgeHumanoidArmorLayerMixin",
            "OptionsMixin",
            "MinecraftClientMixin",
            // Compat — @Pseudo, auto-skipped if target mod absent
            "compat.wildfiregender.GenderArmorLayerMixin",
            "compat.wildfiregender.GenderLegacyLayerMixin",
            "compat.geckolib.GeckoLibArmorMixin",
            "compat.waveycapes.WaveyCapesMixin",
            // Compat - Pseudo and guarded in source for mekanism constant only (1.21.1 Neo)
            "compat.mekanism.MekanismArmorMixin",
            "compat.mekanism.MekaSuitArmorMixin",
            "compat.emf.EmfModelPartMixin",
            "compat.emf.EmfModelPartRootMixin",
            "compat.curios.CuriosLayerMixin",
            "compat.trinkets.TrinketRendererMixin",
            "compat.accessories.AccessoriesRenderLayerMixin"
    };

    @Override
    public String getPackage() {
        return "de.zannagh.armorhider.client.mixin";
    }

    @Override
    public List<String> getExpectedMixins() {
        if (isServerDist) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(MIXINS));
    }
}
