package de.zannagh.armorhider.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class OptionElementFactory {
    private final Screen screen;
    private final OptionListWidget body;
    private final GameOptions gameOptions;
    private boolean renderOptionsFullWidth = true;
    public OptionElementFactory(Screen screen, @Nullable OptionListWidget body, @Nullable GameOptions gameOptions) {
        this.screen = screen;
        this.body = body;
        this.gameOptions = gameOptions;
    }
    
    public OptionElementFactory withHalfWidthRendering() {
        renderOptionsFullWidth = false;
        return this;
    }
    
    public <T> void addSimpleOptionAsWidget(SimpleOption<T> option){
        addElementAsWidget(simpleOptionToGameOptionWidget(option, gameOptions, body, renderOptionsFullWidth));
    }
    
    public void addTextAsWidget(MutableText text) {
        addElementAsWidget(buildTextWidget(text));
    }
    
    public final void addElementAsWidget(ClickableWidget widget){
        if (body == null) {
            return;
        }
        body.addWidgetEntry(widget, null);
    }
    
    private TextWidget buildTextWidget(MutableText text) {
        return new TextWidget(text, screen.getTextRenderer());
    }
    
    public SimpleOption<Double> buildDoubleOption(String key,
                                                  MutableText tooltip,
                                                  @Nullable MutableText narration,
                                                  Function<Double, MutableText> sliderText,
                                                  Double defaultValue,
                                                  Consumer<Double> setter) {
        return new SimpleOption<>(
                key,
                new NarratedTooltipFactory<>(tooltip, narration),
                (text, value) -> sliderText.apply(value),
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
        String booleanKey;
        if (key.getWithStyle(Style.EMPTY).getFirst().getLiteralString() instanceof String textString && !textString.isEmpty()) {
            booleanKey = textString.contains(":") ? textString.split(":")[0] : textString;
        }
        else {
            booleanKey = key.getString();
        }
        return SimpleOption.ofBoolean(
                booleanKey,
                new NarratedTooltipFactory<>(tooltip, narration),
                (text, value) -> value ? Text.translatable("armorhider.options.toggle.on") : Text.translatable("armorhider.options.toggle.off"),
                defaultValue,
                setter
        );
    }

    private static ClickableWidget simpleOptionToGameOptionWidget(SimpleOption<?> simpleOption, GameOptions options, @Nullable OptionListWidget body, boolean fullWidth){
        int rowWidth;
        int rowLeft = 0;
        int y = 0;
        if (body == null) {
            if (MinecraftClient.getInstance().currentScreen == null) {
                rowWidth = MinecraftClient.getInstance().getWindow().getWidth();
            }
            else {
                rowWidth = MinecraftClient.getInstance().currentScreen.width;
                rowLeft = MinecraftClient.getInstance().currentScreen.getBorder(NavigationDirection.LEFT).getLeft();
            }
        }
        else {
            rowWidth = body.getRowWidth();
            rowLeft = body.getRowLeft();
            y = body.getYOfNextEntry();
        }
        
        int width = fullWidth ? rowWidth : rowWidth / 2;
        return simpleOption.createWidget(options, rowLeft, y, width);
    }
}
