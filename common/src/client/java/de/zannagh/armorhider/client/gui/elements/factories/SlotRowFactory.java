package de.zannagh.armorhider.client.gui.elements.factories;

import de.zannagh.armorhider.client.gui.elements.implementations.*;
import de.zannagh.armorhider.client.gui.screens.ItemExclusionScreen;
import de.zannagh.eunomia.client.gui.CompoundOptionWidget;
import de.zannagh.eunomia.ui.UiSizes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Builds the armor-hider opacity slider rows (per equipment slot and for the elytra).
 * Split out of {@link OptionElementFactory} to keep that class within the file size limit.
 */
final class SlotRowFactory {
    private SlotRowFactory() {
    }

    /**
     * Counts the small (square) elements that share the row with the slider, which decides how much
     * width is left for the slider itself.
     */
    private static int countSmallElements(@Nullable Boolean initialGlint,
                                          @Nullable Boolean initialOtherAffect,
                                          @Nullable Consumer<Boolean> glintConsumer,
                                          @Nullable Consumer<Boolean> additionalAffectConsumer,
                                          @Nullable AbstractWidget customToggle,
                                          @Nullable AbstractWidget accessoryButton) {
        int smallCount = 1; // secondary always present
        boolean hasGlint = initialGlint != null && glintConsumer != null;
        if (hasGlint) {
            smallCount++;
        }
        if (initialOtherAffect != null && additionalAffectConsumer != null) {
            smallCount++;
        }
        if (customToggle != null && !hasGlint) {
            smallCount++;
        }
        if (accessoryButton != null) {
            smallCount++;
        }
        return smallCount;
    }

    private static ExtendedSlotIconButton createExclusionButton(EquipmentSlot slot, Options options, int buttonWidth) {
        return new ExtendedSlotIconButton(
                slot,
                buttonWidth,
                UiSizes.DEFAULT_BUTTON_HEIGHT, onPress -> {
            var mc = Minecraft.getInstance();

            //? if <= 26.1.2
            //var currentScreen = mc.screen;
            //? if > 26.1.2
            var currentScreen = mc.gui.screen();
            if (currentScreen == null) {
                return;
            }
            mc.setScreenAndShow(new ItemExclusionScreen(currentScreen, options, slot));
        });
    }

    /**
     * Builds an opacity slider row for a single equipment slot, with the item-exclusion button plus the
     * optional glint, "affect other items", custom and accessory toggles.
     */
    static AbstractWidget createSliderWithToggleForSlot(int rowWidth,
                                                       EquipmentSlot slot,
                                                       OptionInstance<Double> slider,
                                                       Options options,
                                                       @Nullable Boolean initialGlint,
                                                       @Nullable Boolean initialOtherAffect,
                                                       @Nullable Consumer<Boolean> glintConsumer,
                                                       @Nullable Consumer<Boolean> additionalAffectConsumer,
                                                       @Nullable AbstractWidget customToggle,
                                                       @Nullable AbstractWidget accessoryButton) {
        int smallCount = countSmallElements(initialGlint, initialOtherAffect, glintConsumer,
                additionalAffectConsumer, customToggle, accessoryButton);
        int sliderWidth = CompoundOptionWidget.getPrimaryWidth(rowWidth, smallCount);
        int buttonWidth = CompoundOptionWidget.getAdditionalElementWidth(rowWidth, smallCount);

        AbstractWidget sliderWidget = slider.createButton(options, 0, 0, sliderWidth);
        ExtendedSlotIconButton button = createExclusionButton(slot, options, buttonWidth);

        GlintSlotOnOffButton toggleGlintButton = null;
        if (initialGlint != null && glintConsumer != null) {
            toggleGlintButton = new GlintSlotOnOffButton(
                    initialGlint,
                    slot,
                    buttonWidth,
                    UiSizes.DEFAULT_BUTTON_HEIGHT,
                    onPress -> {
                        if (onPress instanceof GlintSlotOnOffButton btn) {
                            var newValue = btn.toggle();
                            glintConsumer.accept(newValue);
                        }
                    });
        }

        AffectOtherItemsButton affectOtherItemsButton = null;

        if (initialOtherAffect != null && additionalAffectConsumer != null) {
            affectOtherItemsButton = new AffectOtherItemsButton(initialOtherAffect,
                    slot,
                    buttonWidth,
                    UiSizes.DEFAULT_BUTTON_HEIGHT,
                    onPress -> {
                        if (onPress instanceof AffectOtherItemsButton btn) {
                            boolean result = btn.toggle();
                            additionalAffectConsumer.accept(result);
                        }
                    });
        }

        AbstractWidget tertiary = toggleGlintButton;
        if (tertiary == null) {
            tertiary = customToggle;
        }

        return new CompoundOptionWidget(sliderWidget, button, tertiary, affectOtherItemsButton, accessoryButton, rowWidth, 20);
    }

    /**
     * Builds the elytra row: an opacity slider (left) plus a glint toggle and an "in flight" toggle
     * (right). The elytra is not a vanilla {@link EquipmentSlot}, so it uses its own dedicated toggle
     * buttons instead of the slot-keyed ones and carries no item-exclusion button.
     */
    static AbstractWidget createElytraSliderRow(int rowWidth,
                                                OptionInstance<Double> slider,
                                                Options options,
                                                boolean initialGlint,
                                                Consumer<Boolean> glintConsumer,
                                                boolean initialInFlight,
                                                Consumer<Boolean> inFlightConsumer) {
        int smallCount = 2; // glint + in-flight
        int sliderWidth = CompoundOptionWidget.getPrimaryWidth(rowWidth, smallCount);
        int buttonWidth = CompoundOptionWidget.getAdditionalElementWidth(rowWidth, smallCount);

        AbstractWidget sliderWidget = slider.createButton(options, 0, 0, sliderWidth);

        var glintButton = new ElytraGlintButton(initialGlint, buttonWidth, UiSizes.DEFAULT_BUTTON_HEIGHT,
                onPress -> {
                    if (onPress instanceof ElytraGlintButton btn) {
                        glintConsumer.accept(btn.toggle());
                    }
                });
        var inFlightButton = new ElytraInFlightButton(initialInFlight, buttonWidth, UiSizes.DEFAULT_BUTTON_HEIGHT,
                onPress -> {
                    if (onPress instanceof ElytraInFlightButton btn) {
                        inFlightConsumer.accept(btn.toggle());
                    }
                });

        return new CompoundOptionWidget(sliderWidget, glintButton, inFlightButton, null, rowWidth, 20);
    }
}
