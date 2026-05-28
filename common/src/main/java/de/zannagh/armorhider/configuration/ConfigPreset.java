package de.zannagh.armorhider.configuration;

import de.zannagh.armorhider.net.packets.PlayerConfig;

import java.util.Objects;

/**
 * A snapshot of the preset-eligible fields from PlayerConfig.
 * These are all local appearance settings that are not server-dependent.
 */
public class ConfigPreset {

    public double helmetOpacity;
    public double chestOpacity;
    public double legsOpacity;
    public double bootsOpacity;
    public double offHandOpacity;
    public boolean helmetGlint;
    public boolean chestGlint;
    public boolean legsGlint;
    public boolean bootsGlint;
    public boolean opacityAffectingHatOrSkull;
    public boolean opacityAffectingElytra;
    public boolean enableCombatDetection;
    public boolean inCombatUseDefaultModel;
    public ExclusionItemConfiguration exclusionItems;

    public ConfigPreset() {
        helmetOpacity = 1.0;
        chestOpacity = 1.0;
        legsOpacity = 1.0;
        bootsOpacity = 1.0;
        offHandOpacity = 1.0;
        helmetGlint = true;
        chestGlint = true;
        legsGlint = true;
        bootsGlint = true;
        opacityAffectingHatOrSkull = true;
        opacityAffectingElytra = true;
        enableCombatDetection = false;
        inCombatUseDefaultModel = false;
        exclusionItems = ExclusionItemConfiguration.defaults();
    }

    public static ConfigPreset fromPlayerConfig(PlayerConfig config) {
        var preset = new ConfigPreset();
        preset.helmetOpacity = config.helmetOpacity.getValue();
        preset.chestOpacity = config.chestOpacity.getValue();
        preset.legsOpacity = config.legsOpacity.getValue();
        preset.bootsOpacity = config.bootsOpacity.getValue();
        preset.offHandOpacity = config.offHandOpacity.getValue();
        preset.helmetGlint = config.helmetGlint.getValue();
        preset.chestGlint = config.chestGlint.getValue();
        preset.legsGlint = config.legsGlint.getValue();
        preset.bootsGlint = config.bootsGlint.getValue();
        preset.opacityAffectingHatOrSkull = config.opacityAffectingHatOrSkull.getValue();
        preset.opacityAffectingElytra = config.opacityAffectingElytra.getValue();
        preset.enableCombatDetection = config.enableCombatDetection.getValue();
        preset.inCombatUseDefaultModel = config.inCombatUseDefaultModel.getValue();
        preset.exclusionItems = config.exclusionItems.deepCopy();
        return preset;
    }

    public void applyTo(PlayerConfig config) {
        config.helmetOpacity.setValue(helmetOpacity);
        config.chestOpacity.setValue(chestOpacity);
        config.legsOpacity.setValue(legsOpacity);
        config.bootsOpacity.setValue(bootsOpacity);
        config.offHandOpacity.setValue(offHandOpacity);
        config.helmetGlint.setValue(helmetGlint);
        config.chestGlint.setValue(chestGlint);
        config.legsGlint.setValue(legsGlint);
        config.bootsGlint.setValue(bootsGlint);
        config.opacityAffectingHatOrSkull.setValue(opacityAffectingHatOrSkull);
        config.opacityAffectingElytra.setValue(opacityAffectingElytra);
        config.enableCombatDetection.setValue(enableCombatDetection);
        config.inCombatUseDefaultModel.setValue(inCombatUseDefaultModel);
        config.exclusionItems = exclusionItems.deepCopy();
    }

    public boolean matchesPlayerConfig(PlayerConfig config) {
        return Double.compare(helmetOpacity, config.helmetOpacity.getValue()) == 0
                && Double.compare(chestOpacity, config.chestOpacity.getValue()) == 0
                && Double.compare(legsOpacity, config.legsOpacity.getValue()) == 0
                && Double.compare(bootsOpacity, config.bootsOpacity.getValue()) == 0
                && Double.compare(offHandOpacity, config.offHandOpacity.getValue()) == 0
                && helmetGlint == config.helmetGlint.getValue()
                && chestGlint == config.chestGlint.getValue()
                && legsGlint == config.legsGlint.getValue()
                && bootsGlint == config.bootsGlint.getValue()
                && opacityAffectingHatOrSkull == config.opacityAffectingHatOrSkull.getValue()
                && opacityAffectingElytra == config.opacityAffectingElytra.getValue()
                && enableCombatDetection == config.enableCombatDetection.getValue()
                && inCombatUseDefaultModel == config.inCombatUseDefaultModel.getValue();
    }

    public ConfigPreset deepCopy() {
        var copy = new ConfigPreset();
        copy.helmetOpacity = helmetOpacity;
        copy.chestOpacity = chestOpacity;
        copy.legsOpacity = legsOpacity;
        copy.bootsOpacity = bootsOpacity;
        copy.offHandOpacity = offHandOpacity;
        copy.helmetGlint = helmetGlint;
        copy.chestGlint = chestGlint;
        copy.legsGlint = legsGlint;
        copy.bootsGlint = bootsGlint;
        copy.opacityAffectingHatOrSkull = opacityAffectingHatOrSkull;
        copy.opacityAffectingElytra = opacityAffectingElytra;
        copy.enableCombatDetection = enableCombatDetection;
        copy.inCombatUseDefaultModel = inCombatUseDefaultModel;
        copy.exclusionItems = exclusionItems.deepCopy();
        return copy;
    }

    static ConfigPreset withUniformOpacity(double opacity) {
        var preset = new ConfigPreset();
        preset.helmetOpacity = opacity;
        preset.chestOpacity = opacity;
        preset.legsOpacity = opacity;
        preset.bootsOpacity = opacity;
        preset.offHandOpacity = opacity;
        return preset;
    }
}
