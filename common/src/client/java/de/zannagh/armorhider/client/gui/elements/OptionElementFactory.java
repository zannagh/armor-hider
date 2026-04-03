package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.screens.InjectableScreen;
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

    public static AbstractWidget createSliderWithToggleForSlot(EquipmentSlot slot,
                                                               OptionInstance<Double> slider,
                                                               Options options,
                                                               boolean initial,
                                                               @Nullable Boolean secondInitial,
                                                               Consumer<Boolean> glintConsumer,
                                                               @Nullable Consumer<Boolean> additionalAffectConsumer,
                                                               int width) {
        int sliderWidth = CompoundOptionWidget.getPrimaryWidth(width);
        int buttonWidth = CompoundOptionWidget.getAdditionalElementWidth(width);
        Component disableGlint = Component.literal("Disable glint on slot");
        Component enableGlint = Component.literal("Enable glint on slot");
        Component initialGlintMessage = initial ? disableGlint : enableGlint;
        
        AbstractWidget sliderWidget = slider.createButton(options, 0, 0, sliderWidth);
        GlintSlotOnOffButton toggleGlintButton = new GlintSlotOnOffButton(
                initial,
                slot, 
                0, 
                0, 
                buttonWidth, 
                UiConstants.DEFAULT_BUTTON_HEIGHT,
                initialGlintMessage, 
                onPress -> {
                if (onPress instanceof GlintSlotOnOffButton btn) {
                    var newValue = btn.toggle();
                    glintConsumer.accept(newValue);
                    if (!newValue) {
                        btn.setTooltipAndMessage(enableGlint);
                    }
                    else {
                        btn.setTooltipAndMessage(disableGlint);
                    }
                }
        }, (component) -> Component.empty());
        ExtendedSlotIconButton button = new ExtendedSlotIconButton(slot, 0, 0, buttonWidth, UiConstants.DEFAULT_BUTTON_HEIGHT, Component.empty(), onPress -> {
            if (!(Minecraft.getInstance().screen instanceof InjectableScreen scr)) {
                return;
            }
            scr.addWidget(new CustomInterceptionsWidget(slot, 0, 0, width, 0, Component.empty()));
        }, (component) -> {
            return Component.empty();
        });
        AffectOtherItemsButton affectOtherItemsButton = null;
        
        if (secondInitial != null && additionalAffectConsumer != null) {
            Component disableAffectOtherItemText = Component.literal("Disable affecting other items");
            Component affectOtherItemText = Component.literal("Enable affecting other items (skulls/elytras)");
            Component initialMessage;
            if (secondInitial) {
                initialMessage = disableAffectOtherItemText;
            }
            else {
                initialMessage = affectOtherItemText;
            }
            affectOtherItemsButton = new AffectOtherItemsButton(secondInitial,
                    slot, 
                    0,
                    0,
                    buttonWidth,
                    UiConstants.DEFAULT_BUTTON_HEIGHT,
                    initialMessage, 
                    onPress -> {
                        if (onPress instanceof AffectOtherItemsButton btn) {
                            boolean result = btn.toggle();
                            additionalAffectConsumer.accept(result);
                            if (result) {
                                btn.setTooltipAndMessage(disableAffectOtherItemText);
                            }
                            else {
                                btn.setTooltipAndMessage(affectOtherItemText);
                            }
                        }
                    }, (component) -> {
                        return Component.empty();
            });
        }
        return new CompoundOptionWidget(sliderWidget, button, toggleGlintButton, affectOtherItemsButton, width, 20);
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
