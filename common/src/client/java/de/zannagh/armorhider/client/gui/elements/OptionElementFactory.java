package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.screens.ItemExclusionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

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
        int sliderWidth = CompoundOptionWidget.getPrimaryWidth(rowWidth);
        int buttonWidth = CompoundOptionWidget.getAdditionalElementWidth(rowWidth);
        
        // Always have a slider + the extended slot button.
        AbstractWidget sliderWidget = slider.createButton(options, 0, 0, sliderWidth);
        ExtendedSlotIconButton button = new ExtendedSlotIconButton(
                slot, 
                buttonWidth,
                UiConstants.DEFAULT_BUTTON_HEIGHT, onPress -> {
                var mc = Minecraft.getInstance();
                var currentScreen = mc.screen;
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
                setter
        );
    }
}
