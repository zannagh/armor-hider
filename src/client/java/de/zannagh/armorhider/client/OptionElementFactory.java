package de.zannagh.armorhider.client;

import de.zannagh.armorhider.rendering.RenderUtilities;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class OptionElementFactory {
    private final Screen screen;
    private final OptionsList body;
    private final Options gameOptions;
    private boolean renderOptionsFullWidth = true;

    public OptionElementFactory(Screen screen, @Nullable OptionsList body, @Nullable Options gameOptions) {
        this.screen = screen;
        this.body = body;
        this.gameOptions = gameOptions;
    }

    public static AbstractWidget simpleOptionToGameOptionWidget(OptionInstance<?> simpleOption, Options options, @Nullable OptionsList body, boolean fullWidth) {
        int rowWidth = RenderUtilities.getRowWidth(body);
        int rowLeft = RenderUtilities.getRowLeft(body);
        int y = RenderUtilities.getNextY(body);
        int width = fullWidth ? rowWidth : rowWidth / 2;
        return simpleOption.createButton(options, rowLeft, y, width);
    }

    public OptionElementFactory withHalfWidthRendering() {
        renderOptionsFullWidth = false;
        return this;
    }

    public <T> void addSimpleOptionAsWidget(OptionInstance<T> option) {
        addElementAsWidget(simpleOptionToGameOptionWidget(option, gameOptions, body, renderOptionsFullWidth));
    }

    public void addTextAsWidget(MutableComponent text) {
        addElementAsWidget(buildTextWidget(text));
    }

    public final void addElementAsWidget(AbstractWidget widget) {
        if (body == null) {
            return;
        }
        body.addSmall(widget, null);
    }

    private AbstractWidget buildTextWidget(MutableComponent text) {
        return new MultiLineTextWidget(text, screen.getFont()).setCentered(true);
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
                new OptionInstance.IntRange(0, 20)
                        .xmap(v -> v / 20.0, v -> (int) Math.round(v * 20), true),
                defaultValue,
                setter
        );
    }

    public OptionInstance<Boolean> buildBooleanOption(MutableComponent key,
                                                      MutableComponent tooltip,
                                                      @Nullable MutableComponent narration,
                                                      Boolean defaultValue,
                                                      Consumer<Boolean> setter) {
        // Extract the translation key from TranslatableContents, or fall back to getString()
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
