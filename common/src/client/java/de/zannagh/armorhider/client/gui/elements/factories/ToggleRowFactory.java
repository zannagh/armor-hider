package de.zannagh.armorhider.client.gui.elements.factories;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.gui.elements.implementations.*;
import de.zannagh.armorhider.configuration.PresetManager;
import de.zannagh.eunomia.client.gui.CompoundButtonWidget;
import de.zannagh.eunomia.client.gui.CompoundOptionWidget;
import de.zannagh.eunomia.client.gui.ElementSpacingOptions;
import de.zannagh.eunomia.ui.UiSizes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds the armor-hider specific toggle rows (general behaviour toggles, presets, compatibilities).
 * Split out of {@link OptionElementFactory} to keep that class within the file size limit.
 */
final class ToggleRowFactory {
    private ToggleRowFactory() {
    }

    /**
     * Builds the three general behaviour toggle buttons (combat detection, vanilla armor in combat,
     * respect invisibility) from the given config accessor pairs.
     */
    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    private static ArrayList<AbstractWidget> createGeneralToggleButtons(ArrayList<Pair<Boolean, Consumer<Boolean>>> configs) {
        var first = new CombatDetectionButton(
                configs.get(0).getFirst(),
                onPress -> {
                    if (onPress instanceof CombatDetectionButton btn) {
                        configs.get(0).getSecond().accept(btn.toggle());
                    }
                }
        );
        var second = new VanillaArmorInCombatButton(
                configs.get(1).getFirst(),
                onPress -> {
                    if (onPress instanceof VanillaArmorInCombatButton btn) {
                        configs.get(1).getSecond().accept(btn.toggle());
                    }
                }
        );
        var third = new RespectInvisibilityButton(
                configs.get(2).getFirst(),
                onPress -> {
                    if (onPress instanceof RespectInvisibilityButton btn) {
                        configs.get(2).getSecond().accept(btn.toggle());
                    }
                }
        );
        // The accessory + hidden-model (EMF) compat toggles live in their own "Compatibilities" row
        // (see ArmorHiderOptionsPanelWidget), not here, so this row stays behaviour-only.
        return new ArrayList<AbstractWidget>(List.of(first, second, third));
    }

    private static void openIndividualPlayerSettings(Options gameOptions) {
        var mc = Minecraft.getInstance();
        //? if <= 26.1.2
        //var currentScreen = mc.screen;
        //? if > 26.1.2
        var currentScreen = mc.gui.screen();
        if (currentScreen == null) {
            return;
        }
        mc.setScreenAndShow(new de.zannagh.armorhider.client.gui.screens.IndividualPlayerConfigurationsScreen(
                currentScreen, gameOptions, Component.translatable("armorhider.individual.title")));
    }

    private static ElementSpacingOptions createCompoundSpacing(int rowWidth, int globalButtonCount, int totalButtons) {
        var groups = new ArrayList<Pair<Integer, Integer>>();
        groups.add(new Pair<>(0, globalButtonCount - 1));
        groups.add(new Pair<>(globalButtonCount, totalButtons - 1));

        int sq = UiSizes.SQUARE_BUTTON_WIDTH;
        int g = UiSizes.DEFAULT_BUTTON_SPACING / 2;
        int presetCount = PresetManager.PRESET_COUNT;
        int groupBWidth = presetCount * sq + (presetCount - 1) * g;
        int groupAWidth = rowWidth - groupBWidth - g;
        int minGroupA = globalButtonCount * sq + (globalButtonCount - 1) * g;
        return new ElementSpacingOptions(rowWidth)
                .forEvenElements(sq, totalButtons)
                .withGroups(groups)
                .withMinSizesForGroups(new int[]{minGroupA, groupBWidth})
                .withSizesForGroups(new int[]{groupAWidth, groupBWidth})
                .withRightAlignmentForGroup(1);
    }

    /**
     * Builds a compound widget consisting of global option buttons (left group) and preset buttons
     * (right group), using a 50/50 split of the available row width.
     */
    static AbstractWidget createCompoundButtonWidget(
            int rowWidth,
            Options gameOptions,
            ArrayList<Pair<Boolean, Consumer<Boolean>>> configs,
            PresetManager presetManager,
            int activePresetIndex,
            Consumer<Integer> onPresetActivated
    ) {
        var globalButtons = createGeneralToggleButtons(configs);
        globalButtons.add(new IndividualPlayerSettingsButton(onPress -> openIndividualPlayerSettings(gameOptions)));

        int globalButtonCount = globalButtons.size();
        int totalButtons = globalButtonCount + PresetManager.PRESET_COUNT;
        var allButtons = new AbstractWidget[totalButtons];
        for (int i = 0; i < globalButtonCount; i++) {
            allButtons[i] = globalButtons.get(i);
        }

        for (int i = 0; i < PresetManager.PRESET_COUNT; i++) {
            boolean hasPreset = presetManager.hasPreset(i);
            boolean isActive = i == activePresetIndex;
            allButtons[globalButtonCount + i] = new PresetButton(i, !hasPreset, isActive, onPress -> {
                if (onPress instanceof PresetButton pb) {
                    onPresetActivated.accept(pb.getPresetIndex());
                }
            });
        }

        var spacing = createCompoundSpacing(rowWidth, globalButtonCount, totalButtons);
        return new CompoundButtonWidget(allButtons, rowWidth, 20, spacing);
    }

    /**
     * Builds a left-aligned row of just the three general behaviour toggles (combat detection, vanilla armor
     * in combat, respect invisibility) - no presets and no "individual settings" button. Used by the
     * per-player override panel, where presets are not applicable but the behaviour toggles still are.
     */
    static AbstractWidget createGeneralTogglesRow(int rowWidth, ArrayList<Pair<Boolean, Consumer<Boolean>>> configs) {
        var buttons = createGeneralToggleButtons(configs);
        int sq = UiSizes.SQUARE_BUTTON_WIDTH;
        var spacing = new ElementSpacingOptions(rowWidth)
                .forEvenElements(sq, buttons.size())
                .withLeftAlignment();
        return new CompoundButtonWidget(buttons.toArray(new AbstractWidget[0]), rowWidth, 20, spacing);
    }

    /**
     * A "Compatibilities" row: a left-aligned label filling the left, with the given compat toggle
     * buttons as fixed square icons right-bound on the right (same width as the other toggles).
     * Returns {@code null} when there are no compat buttons to show, so the caller can omit the row.
     *
     * @param rowWidth the available row width.
     * @param compatButtons the square compat toggle buttons (0-2); empty means no row
     * @return the row widget, or {@code null} if there is nothing to show
     */
    static @Nullable AbstractWidget createCompatibilitiesRow(int rowWidth, List<AbstractWidget> compatButtons) {
        if (compatButtons.isEmpty()) {
            return null;
        }
        // Left-aligned label (MultiLineTextWidget defaults to left-aligned) nudged to vertically centre
        // in the row; CompoundOptionWidget sets its X/width and top Y, so we offset Y on top of that.
        int rowHeight = UiSizes.DEFAULT_BUTTON_HEIGHT;
        var label = new MultiLineTextWidget(
                Component.translatable("armorhider.options.compatibilities"),
                Minecraft.getInstance().font) {
            @Override
            public void setY(int y) {
                super.setY(y + Math.max(0, (rowHeight - Minecraft.getInstance().font.lineHeight) / 2));
            }
        };
        AbstractWidget secondary = compatButtons.get(0);
        AbstractWidget tertiary = compatButtons.size() > 1 ? compatButtons.get(1) : null;
        return new CompoundOptionWidget(label, secondary, tertiary, null, rowWidth, 20);
    }
}
