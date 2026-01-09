package de.zannagh.armorhider.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class OptionElementFactory {
    private final OptionListWidget body;

    public OptionElementFactory(Screen screen, @Nullable OptionListWidget body, @Nullable GameOptions gameOptions) {
        this.body = body;
    }
    
    public <T> void addSimpleOptionAsWidget(SimpleOption<T> option){
        // 1.20.1 compatibility: addSingleOptionEntry expects SimpleOption, not widget
        if (body == null) {
            return;
        }
        body.addSingleOptionEntry(option);
    }
    
    public SimpleOption<Double> buildDoubleOption(String key,
                                                  MutableText tooltip,
                                                  @Nullable MutableText narration,
                                                  Function<Double, MutableText> sliderTextProvider,
                                                  Double defaultValue,
                                                  Consumer<Double> setter) {
        return new SimpleOption<>(
                key,
                new NarratedTooltipFactory<>(tooltip, narration),
                (text, value) -> sliderTextProvider.apply(value),
                new SimpleOption.ValidatingIntSliderCallbacks(0, 20)
                        .withModifier(v -> v / 20.0, v -> (int) Math.round(v * 20)),
                defaultValue,
                setter
        );
    }

    public SimpleOption<Boolean> buildBooleanOption(MutableText key,
                                                  MutableText tooltip,
                                                  @Nullable MutableText narration,
                                                  Boolean defaultValue,
                                                  Consumer<Boolean> setter) {
        // 1.20.1 compatibility: simplified text key extraction
        String keyString = key.getString();
        String booleanKey = keyString.contains(":") ? keyString.split(":")[0] : keyString;

        return SimpleOption.ofBoolean(
                booleanKey,
                new NarratedTooltipFactory<>(tooltip, narration),
                (text, value) -> value ? Text.translatable("armorhider.options.toggle.on") : Text.translatable("armorhider.options.toggle.off"),
                defaultValue,
                setter
        );
    }
}
