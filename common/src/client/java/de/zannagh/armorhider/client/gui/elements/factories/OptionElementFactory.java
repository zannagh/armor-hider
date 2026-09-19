package de.zannagh.armorhider.client.gui.elements.factories;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.configuration.PresetManager;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Armor-hider's option element factory: eunomia's generic factory plus the mod-specific rows
 * (behaviour toggles, presets, compatibilities, slot sliders, elytra).
 */
public class OptionElementFactory extends de.zannagh.eunomia.client.gui.factories.OptionElementFactory {
    /**
     * Own copy of the game options: eunomia's field is private with no accessor, so the same instance is
     * handed to {@code super} and kept here for the rows that need it.
     */
    private final Options gameOptions;

    public OptionElementFactory(Consumer<AbstractWidget> widgetAdder, Options gameOptions, int rowWidth) {
        super(widgetAdder, gameOptions, rowWidth);
        this.gameOptions = gameOptions;
    }

    /**
     * Adds a compound widget consisting of global option buttons (left group) and preset buttons
     * (right group), using a 50/50 split of the available row width.
     */
    public AbstractWidget createCompoundButtonWidget(
            ArrayList<Pair<Boolean, Consumer<Boolean>>> configs,
            PresetManager presetManager,
            int activePresetIndex,
            Consumer<Integer> onPresetActivated
    ) {
        return ToggleRowFactory.createCompoundButtonWidget(
                getRowWidth(), gameOptions, configs, presetManager, activePresetIndex, onPresetActivated);
    }

    /**
     * Builds a left-aligned row of just the three general behaviour toggles - no presets and no
     * "individual settings" button. Used by the per-player override panel.
     */
    public AbstractWidget createGeneralTogglesRow(ArrayList<Pair<Boolean, Consumer<Boolean>>> configs) {
        return ToggleRowFactory.createGeneralTogglesRow(getRowWidth(), configs);
    }

    /**
     * A "Compatibilities" row of square compat toggle buttons behind a left-aligned label.
     *
     * @param compatButtons the square compat toggle buttons (0-2); empty means no row
     * @return the row widget, or {@code null} if there is nothing to show
     */
    public @Nullable AbstractWidget createCompatibilitiesRow(List<AbstractWidget> compatButtons) {
        return ToggleRowFactory.createCompatibilitiesRow(getRowWidth(), compatButtons);
    }

    public void addSliderWithToggles(EquipmentSlot slot,
                                     OptionInstance<Double> slider,
                                     Options options,
                                     @Nullable Boolean initialGlint,
                                     @Nullable Boolean initialOtherAffect,
                                     @Nullable Consumer<Boolean> glintConsumer,
                                     @Nullable Consumer<Boolean> additionalAffectConsumer) {
        addSliderWithToggles(slot, slider, options, initialGlint, initialOtherAffect, glintConsumer, additionalAffectConsumer, null);
    }

    public void addSliderWithToggles(EquipmentSlot slot,
                                     OptionInstance<Double> slider,
                                     Options options,
                                     @Nullable Boolean initialGlint,
                                     @Nullable Boolean initialOtherAffect,
                                     @Nullable Consumer<Boolean> glintConsumer,
                                     @Nullable Consumer<Boolean> additionalAffectConsumer,
                                     @Nullable AbstractWidget customToggle) {
        addSliderWithToggles(slot, slider, options, initialGlint, initialOtherAffect, glintConsumer, additionalAffectConsumer, customToggle, null);
    }

    public void addSliderWithToggles(EquipmentSlot slot,
                                     OptionInstance<Double> slider,
                                     Options options,
                                     @Nullable Boolean initialGlint,
                                     @Nullable Boolean initialOtherAffect,
                                     @Nullable Consumer<Boolean> glintConsumer,
                                     @Nullable Consumer<Boolean> additionalAffectConsumer,
                                     @Nullable AbstractWidget customToggle,
                                     @Nullable AbstractWidget accessoryButton) {
        var widget = createSliderWithToggleForSlot(slot, slider, options, initialGlint, initialOtherAffect, glintConsumer, additionalAffectConsumer, customToggle, accessoryButton);
        addElementAsWidget(widget);
    }

    public AbstractWidget createSliderWithToggleForSlot(EquipmentSlot slot,
                                                       OptionInstance<Double> slider,
                                                       Options options,
                                                       @Nullable Boolean initialGlint,
                                                       @Nullable Boolean initialOtherAffect,
                                                       @Nullable Consumer<Boolean> glintConsumer,
                                                       @Nullable Consumer<Boolean> additionalAffectConsumer) {
        return createSliderWithToggleForSlot(slot, slider, options, initialGlint, initialOtherAffect, glintConsumer, additionalAffectConsumer, null);
    }

    public AbstractWidget createSliderWithToggleForSlot(EquipmentSlot slot,
                                                       OptionInstance<Double> slider,
                                                       Options options,
                                                       @Nullable Boolean initialGlint,
                                                       @Nullable Boolean initialOtherAffect,
                                                       @Nullable Consumer<Boolean> glintConsumer,
                                                       @Nullable Consumer<Boolean> additionalAffectConsumer,
                                                       @Nullable AbstractWidget customToggle) {
        return createSliderWithToggleForSlot(slot, slider, options, initialGlint, initialOtherAffect, glintConsumer, additionalAffectConsumer, customToggle, null);
    }

    public AbstractWidget createSliderWithToggleForSlot(EquipmentSlot slot,
                                                       OptionInstance<Double> slider,
                                                       Options options,
                                                       @Nullable Boolean initialGlint,
                                                       @Nullable Boolean initialOtherAffect,
                                                       @Nullable Consumer<Boolean> glintConsumer,
                                                       @Nullable Consumer<Boolean> additionalAffectConsumer,
                                                       @Nullable AbstractWidget customToggle,
                                                       @Nullable AbstractWidget accessoryButton) {
        return SlotRowFactory.createSliderWithToggleForSlot(getRowWidth(), slot, slider, options, initialGlint,
                initialOtherAffect, glintConsumer, additionalAffectConsumer, customToggle, accessoryButton);
    }

    /**
     * Builds the elytra row: an opacity slider (left) plus a glint toggle and an "in flight" toggle
     * (right).
     */
    public AbstractWidget createElytraSliderRow(OptionInstance<Double> slider,
                                                Options options,
                                                boolean initialGlint,
                                                Consumer<Boolean> glintConsumer,
                                                boolean initialInFlight,
                                                Consumer<Boolean> inFlightConsumer) {
        return SlotRowFactory.createElytraSliderRow(getRowWidth(), slider, options, initialGlint, glintConsumer,
                initialInFlight, inFlightConsumer);
    }

    /**
     * Builds a boolean toggle option labelled with armor-hider's own on/off text.
     *
     * @param key the caption component (a translatable key is extracted when present).
     * @param tooltip the tooltip component.
     * @param narration the optional narration component (falls back to the tooltip when null).
     * @param defaultValue the default value.
     * @param setter receives the value on change.
     * @return the configured option instance.
     */
    public OptionInstance<Boolean> buildBooleanOption(MutableComponent key,
                                                      MutableComponent tooltip,
                                                      @Nullable MutableComponent narration,
                                                      Boolean defaultValue,
                                                      Consumer<Boolean> setter) {
        return buildBooleanOption(
                key,
                tooltip,
                narration,
                value -> value
                        ? Component.translatable("armorhider.options.toggle.on")
                        : Component.translatable("armorhider.options.toggle.off"),
                defaultValue,
                setter);
    }
}
