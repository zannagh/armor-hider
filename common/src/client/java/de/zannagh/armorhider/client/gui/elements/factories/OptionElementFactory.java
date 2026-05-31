package de.zannagh.armorhider.client.gui.elements.factories;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.CompoundButtonWidget;
import de.zannagh.armorhider.client.gui.elements.CompoundOptionWidget;
import de.zannagh.armorhider.client.gui.elements.ElementSpacingOptions;
import de.zannagh.armorhider.client.gui.elements.implementations.*;
import de.zannagh.armorhider.client.gui.screens.ItemExclusionScreen;
import de.zannagh.armorhider.configuration.ConfigPreset;
import de.zannagh.armorhider.configuration.PresetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

public class OptionElementFactory {
    private final Options gameOptions;
    private final Consumer<AbstractWidget> widgetAdder;
    private final int rowWidth;

    public OptionElementFactory(Consumer<AbstractWidget> widgetAdder, Options gameOptions, int rowWidth) {
        this.widgetAdder = widgetAdder;
        this.gameOptions = gameOptions;
        this.rowWidth = rowWidth;
    }

    public int getRowWidth() {
        return rowWidth;
    }

    public void addElementAsWidget(AbstractWidget widget) {
        widgetAdder.accept(widget);
    }

    public <T> void addSimpleOptionAsWidget(OptionInstance<T> option) {
        widgetAdder.accept(option.createButton(gameOptions, 0, 0, rowWidth));
    }

    public void addTextWidget(Component text) {
        var textWidget = new MultiLineTextWidget(text, Minecraft.getInstance().font).setCentered(true);
        widgetAdder.accept(textWidget);
    }

    /**
     * Adds a compound widget consisting of global option buttons (left group) and preset buttons
     * (right group), using a 50/50 split of the available row width.
     */
    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    public AbstractWidget createCompoundButtonWidget(
            ArrayList<Pair<Boolean, Consumer<Boolean>>> configs,
            PresetManager presetManager,
            int activePresetIndex,
            Consumer<Integer> onPresetActivated
    ){
        var first = new CombatDetectionButton(
                configs.get(0).getFirst(),
                onPress -> {
                    if (onPress instanceof CombatDetectionButton btn) {
                        var newValue = btn.toggle();
                        configs.get(0).getSecond().accept(newValue);
                    }
                }
        );
        var second = new VanillaArmorInCombatButton(
                configs.get(1).getFirst(),
                onPress -> {
                    if (onPress instanceof VanillaArmorInCombatButton btn) {
                        var newValue = btn.toggle();
                        configs.get(1).getSecond().accept(newValue);
                    }
                }
        );

        int totalButtons = 2 + PresetManager.PRESET_COUNT;
        var allButtons = new AbstractWidget[totalButtons];
        allButtons[0] = first;
        allButtons[1] = second;

        var presetButtons = new PresetButton[PresetManager.PRESET_COUNT];
        for (int i = 0; i < PresetManager.PRESET_COUNT; i++) {
            final int presetIndex = i;
            boolean hasPreset = presetManager.hasPreset(i);
            boolean isActive = i == activePresetIndex;
            var btn = new PresetButton(i, !hasPreset, isActive, onPress -> {
                if (onPress instanceof PresetButton pb) {
                    onPresetActivated.accept(pb.getPresetIndex());
                }
            });
            presetButtons[i] = btn;
            allButtons[2 + i] = btn;
        }

        var groups = new ArrayList<Pair<Integer, Integer>>();
        groups.add(new Pair<>(0, 1));
        groups.add(new Pair<>(2, totalButtons - 1));

        int sq = UiConstants.SQUARE_BUTTON_WIDTH;
        int g = UiConstants.DEFAULT_BUTTON_SPACING / 2;
        int presetCount = PresetManager.PRESET_COUNT;
        int groupBWidth = presetCount * sq + (presetCount - 1) * g;
        int groupAWidth = rowWidth - groupBWidth - g;
        int minGroupA = 2 * sq + g;
        var spacing = new ElementSpacingOptions(rowWidth)
                .forEvenElements(sq, totalButtons)
                .withGroups(groups)
                .withMinSizesForGroups(new int[]{minGroupA, groupBWidth})
                .withSizesForGroups(new int[]{groupAWidth, groupBWidth})
                .withRightAlignmentForGroup(1);

        return new CompoundButtonWidget(allButtons, rowWidth, 20, spacing);
    }
    
    public void addSliderWithToggles(EquipmentSlot slot,
                                     OptionInstance<Double> slider,
                                     Options options,
                                     @Nullable Boolean initialGlint,
                                     @Nullable Boolean initialOtherAffect,
                                     @Nullable Consumer<Boolean> glintConsumer,
                                     @Nullable Consumer<Boolean> additionalAffectConsumer){
        var widget = createSliderWithToggleForSlot(slot, slider, options, initialGlint, initialOtherAffect, glintConsumer, additionalAffectConsumer);
        addElementAsWidget(widget);
    }
    
    public AbstractWidget createSliderWithToggleForSlot(EquipmentSlot slot,
                                                       OptionInstance<Double> slider,
                                                       Options options,
                                                       @Nullable Boolean initialGlint,
                                                       @Nullable Boolean initialOtherAffect,
                                                       @Nullable Consumer<Boolean> glintConsumer,
                                                       @Nullable Consumer<Boolean> additionalAffectConsumer) {
        int smallCount = 1; // secondary always present
        if (initialGlint != null && glintConsumer != null) smallCount++;
        if (initialOtherAffect != null && additionalAffectConsumer != null) smallCount++;
        int sliderWidth = CompoundOptionWidget.getPrimaryWidth(rowWidth, smallCount);
        int buttonWidth = CompoundOptionWidget.getAdditionalElementWidth(rowWidth, smallCount);

        AbstractWidget sliderWidget = slider.createButton(options, 0, 0, sliderWidth);
        ExtendedSlotIconButton button = new ExtendedSlotIconButton(
                slot,
                buttonWidth,
                UiConstants.DEFAULT_BUTTON_HEIGHT, onPress -> {
                var mc = Minecraft.getInstance();

                //? if <= 26.1.2
                var currentScreen = mc.screen;
                //? if > 26.1.2
                //var currentScreen = mc.gui.screen();
                if (currentScreen == null) {
                    return;
                }
                mc.setScreenAndShow(new ItemExclusionScreen(currentScreen, options, slot));
            });

        GlintSlotOnOffButton toggleGlintButton = null;
        if (initialGlint != null && glintConsumer != null) {
            toggleGlintButton = new GlintSlotOnOffButton(
                    initialGlint,
                    slot,
                    buttonWidth,
                    UiConstants.DEFAULT_BUTTON_HEIGHT,
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
                    UiConstants.DEFAULT_BUTTON_HEIGHT,
                    onPress -> {
                        if (onPress instanceof AffectOtherItemsButton btn) {
                            boolean result = btn.toggle();
                            additionalAffectConsumer.accept(result);
                        }
                    });
        }

        return new CompoundOptionWidget(sliderWidget, button, toggleGlintButton, affectOtherItemsButton, rowWidth, 20);
    }

    public OptionInstance<Double> buildDoubleOption(String key,
                                                    MutableComponent tooltip,
                                                    @Nullable MutableComponent narration,
                                                    Function<Double, MutableComponent> sliderTextProvider,
                                                    Double defaultValue,
                                                    Consumer<Double> setter) {
        return new OptionInstance<>(
                key,
                new NarratedTooltipFactory<>(tooltip, narration),
                (text, value) -> sliderTextProvider.apply(value),
                //? if >= 1.21.11
                new OptionInstance.IntRange(0, 20).xmap(v -> v / 20.0, v -> (int) Math.round(v * 20), true)
                //? if >= 1.20.5 && < 1.21.11
                //new OptionInstance.IntRange(0, 20).xmap(v -> v / 20.0, v -> (int) Math.round(v * 20))
                //? if < 1.20.5
                //OptionInstance.UnitDouble.INSTANCE
                ,
                defaultValue,
                //? if > 26.1.2
                //setter::accept
                //? if <= 26.1.2
                setter
        );
    }

    public OptionInstance<Boolean> buildBooleanOption(MutableComponent key,
                                                      MutableComponent tooltip,
                                                      @Nullable MutableComponent narration,
                                                      Boolean defaultValue,
                                                      Consumer<Boolean> setter) {
        String booleanKey;
        if (key.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatableContents) {
            booleanKey = translatableContents.getKey();
        } else {
            booleanKey = key.getString();
        }
        return OptionInstance.createBoolean(
                booleanKey,
                new NarratedTooltipFactory<>(tooltip, narration),
                (text, value) -> value ? Component.translatable("armorhider.options.toggle.on") : Component.translatable("armorhider.options.toggle.off"),
                defaultValue,
                //? if > 26.1.2
                //setter::accept
                //? if <= 26.1.2
                setter
        );
    }
}
