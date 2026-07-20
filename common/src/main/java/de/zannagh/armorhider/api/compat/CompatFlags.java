package de.zannagh.armorhider.api.compat;

import java.util.function.Function;

/**
 * Compat flags to other mods.
 */
public enum CompatFlags {

    /**
     * Wildfire Gender Mod. Received signature fix in 0.11.8.
     *
     * @since 0.10.0-pre.8
     */
    GENDER_MOD(1<<0, "com.wildfire.render.GenderArmorLayer"),

    /**
     * Elytra Trims.
     *
     * @since 0.10.3
     */
    ELYTRA_TRIMS(1<<1, "dev.kikugie.elytratrims.ep.ETClientEntrypoint"),

    /**
     * Wavey Capes fancy capes
     *
     * @since 0.10.13
     */
    WAVEY_CAPES(1<<2, "dev.tr7zw.waveycapes.WaveyCapesMod"),

    /**
     * LuckPerms permission manager.
     *
     * @since 0.10.3
     */
    LUCK_PERMS(1<<3, "net.luckperms.api.LuckPermsProvider"),

    /**
     * Entity Model Features
     *
     * @since 0.10.18-pre.1
     */
    ENTITY_MODEL_FEATURES(1<<4, true, "traben.entity_model_features.EMFManager"),

    /**
     * Explicit Fantasy Armor to redraw player arms.
     *
     * @since 0.10.18-pre.1
     */
    FANTASY_ARMOR(1<<5, "net.kenddie.fantasyarmor.FantasyArmor"),

    /**
     * Mekanism (NeoForge only). Got some fixes up to 0.11.4.
     *
     * @since 0.11.1
     */
    MEKANISM(1<<6, "mekanism.client.render.armor.MekaSuitArmor"),

    /**
     * Figura custom player models (only really used for Mekanism adjustments).
     *
     * @since 0.11.2
     */
    FIGURA(1<<7, "org.figuramc.figura.FiguraMod"),

    /**
     * Fabric's own resource loader to ensure icons are loaded properly from the mod resources.
     *
     * @since 0.10.14-pre.3
     */
    FABRIC_API_RESOURCE_LOADER(1<<8, "net.fabricmc.fabric.api.resource.ResourceManagerHelper"),

    /**
     * Iris shaders, explicit compatibility through RenderPipelines.
     *
     * @since 0.10.9-pre.8
     */
    IRIS(1<<9, true, "net.irisshaders.iris.api.v0.IrisApi"),

    /**
     * GeckoLib armor library.
     *
     * @since 0.10.3
     */
    GECKO_LIB(1<<10, "com.geckolib.renderer.GeoArmorRenderer"),

    /**
     * Curios API for additional slots.
     *
     * @since 0.12.0
     */
    CURIOS(1<<11, "top.theillusivec4.curios.api.CuriosApi"),

    /**
     * Trinkets additional slots mod.
     *
     * @since 0.12.0
     */
    TRINKETS(1<<12, "dev.emi.trinkets.api.TrinketsApi"),

    /**
     * Accessories additional slots mod.
     *
     * @since 0.12.0
     */
    ACCESSORIES(1<<13, "io.wispforest.accessories.api.AccessoriesCapability"),

    /**
     * Artifacts additional slots mod.
     *
     * @since 0.12.0
     */
    ARTIFACTS(1<<14, "artifacts.client.item.renderer.ArtifactRenderer"),

    /**
     * Syntra cross-loader mod for running forge stuff in Fabric.
     *
     * @since 0.10.10
     */
    SYNTRA(1<<15, "cpw.mods.modlauncher.Launcher");

    private final long compatFlagValue;

    private final boolean requiresInitialization;

    private final String[] classNames;

    CompatFlags(long compatFlagValue, String... classNames) {
        this.compatFlagValue = compatFlagValue;
        this.requiresInitialization = false;
        this.classNames = classNames;
    }

    CompatFlags(long compatFlagValue, boolean requiresInitialization, String... classNames) {
        this.compatFlagValue = compatFlagValue;
        this.requiresInitialization = requiresInitialization;
        this.classNames = classNames;
    }

    /**
     * Probes whether the compat flag needs an initialization method.
     * @return True when separate initialization is required.
     */
    public boolean needsInitialization() {
        return requiresInitialization;
    }

    public boolean isAvailable(Function<String, Boolean> isClassAvailable) {
        for (var name : classNames) {
            if (isClassAvailable.apply(name)) {
                return true;
            }
        }
        return false;
    }
}
