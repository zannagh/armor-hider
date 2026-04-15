package de.zannagh.armorhider.client;

import de.zannagh.armorhider.CompatFlags;
import de.zannagh.armorhider.util.MixinUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "de.zannagh.armorhider.client.mixin";

    // Mixins listed unconditionally — Stonecutter source guards strip classes for
    // incompatible versions, so getMixinClassesWherePresent filters them automatically.
    private static final String[] MIXINS = new String[]{
            // Always present
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
            "hand.ItemRendererMixin",
            "bodyKneesAndToes.NeoForgeArmorColorMixin",
            "bodyKneesAndToes.NeoForgeHumanoidArmorLayerMixin",
            "OptionsMixin",
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
        //? if >= 1.21.9 {
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) return List.of();
        //?} else {
        /*if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) return List.of();*/
        //?}
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
        if (classExists("dev.kikugie.elytratrims.ep.ETClientEntrypoint")) CompatFlags.ET_LOADED = true;
        if (classExists("software.bernie.geckolib.renderer.GeoArmorRenderer")) CompatFlags.GECKOLIB_LOADED = true;
        if (classExists("net.kenddie.fantasyarmor.FantasyArmor")) CompatFlags.FA_LOADED = true;
        if (classExists("com.wildfire.render.GenderArmorLayer")) CompatFlags.WFGM_LOADED = true;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, MixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }
}
