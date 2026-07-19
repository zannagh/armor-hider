package de.zannagh.armorhider.api;

import java.util.function.Function;

/**
 * Compat flags to other mods.
 */
public enum CompatFlags {

    /**
     * Wildfire Gender Mod.
     *
     * @since 0.x.x (TODO)
     */
    GENDER_MOD(0, "com.wildfire.render.GenderArmorLayer"),

    ELYTRA_TRIMS(1<<1, "dev.kikugie.elytratrims.ep.ETClientEntrypoint"),

    WAVEY_CAPES(1<<2),

    LUCK_PERMS(1<<3, "net.luckperms.api.LuckPermsProvider"),

    ENTITY_MODEL_FEATURES(1<<4, true, "traben.entity_model_features.EMFManager"),

    FANTASY_ARMOR(1<<5, "net.kenddie.fantasyarmor.FantasyArmor"),

    MEKANISM(1<<6),

    FIGURA(1<<7, "org.figuramc.figura.FiguraMod"),

    FABRIC_API_RESOURCE_LOADER(1<<8, "net.fabricmc.fabric.api.resource.ResourceManagerHelper"),

    IRIS(1<<9, true, "net.irisshaders.iris.api.v0.IrisApi"),

    GECKO_LIB(1<<10, "com.geckolib.renderer.GeoArmorRenderer"),

    CURIOS(1<<11, "top.theillusivec4.curios.api.CuriosApi"),

    TRINKETS(1<<12, "dev.emi.trinkets.api.TrinketsApi"),

    ACCESSORIES(1<<13, "io.wispforest.accessories.api.AccessoriesCapability"),

    ARTIFACTS(1<<14, "artifacts.client.item.renderer.ArtifactRenderer");

    private final long compatFlagValue;

    private boolean requiresInitialization;

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
