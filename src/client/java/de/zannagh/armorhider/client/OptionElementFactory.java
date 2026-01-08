package de.zannagh.armorhider.client;

import de.zannagh.armorhider.rendering.RenderUtilities;
import net.minecraft.client.MinecraftClient;
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

import java.util.function.Consumer;
import java.util.function.Function;

public class OptionElementFactory {
    private final OptionListWidget body;
    private final GameOptions gameOptions;
    private boolean renderOptionsFullWidth = true;
    public OptionElementFactory(Screen screen, @Nullable OptionListWidget body, @Nullable GameOptions gameOptions) {
        this.body = body;
        this.gameOptions = gameOptions;
    }
    
    public OptionElementFactory withHalfWidthRendering() {
        renderOptionsFullWidth = false;
        return this;
    }
    
    public <T> void addSimpleOptionAsWidget(SimpleOption<T> option){
        // 1.20.1 compatibility: addSingleOptionEntry expects SimpleOption, not widget
        if (body == null) {
            return;
        }
        body.addSingleOptionEntry(option);
    }

    public <T> void addSimpleOptionWithSecondWidget(SimpleOption<T> option, ClickableWidget secondWidget){
        if (body == null) {
            return;
        }
        // 1.20.1 compatibility: addSingleOptionEntry expects SimpleOption, not widget
        // The two-widget layout API doesn't exist, so just add the first option
        body.addSingleOptionEntry(option);
        // TODO: Player preview widget disabled in 1.20.1 due to UI API changes
    }

    public void addTextAsWidget(MutableText text) {
        // 1.20.1: Text widgets cannot be added directly to OptionListWidget
        // This method is a no-op in 1.20.1
    }

    public final void addElementAsWidget(ClickableWidget widget){
        // 1.20.1: Cannot add arbitrary widgets to OptionListWidget
        // This method is a no-op in 1.20.1
    }
    
    private TextWidget buildTextWidget(MutableText text) {
        return new TextWidget(text, MinecraftClient.getInstance().textRenderer);
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

    private static ClickableWidget simpleOptionToGameOptionWidget(SimpleOption<?> simpleOption, GameOptions options, @Nullable OptionListWidget body, boolean fullWidth){
        int rowWidth = RenderUtilities.getRowWidth(body);
        int rowLeft = RenderUtilities.getRowLeft(body);
        int y = RenderUtilities.getNextY(body);
        int width = fullWidth ? rowWidth : rowWidth / 2;
        return simpleOption.createWidget(options, rowLeft, y, width);
    }
}
