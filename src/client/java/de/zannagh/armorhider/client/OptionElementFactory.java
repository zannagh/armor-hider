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
    //? if < 1.21.9 {
    private Consumer<AbstractWidget> widgetAdder;
    //?}

    public OptionElementFactory(Screen screen, @Nullable OptionsList body, @Nullable Options gameOptions) {
        this.screen = screen;
        this.body = body;
        this.gameOptions = gameOptions;
    }

    //? if < 1.21.9 {
    // In 1.20.x, set a callback for adding arbitrary widgets since we can't access protected methods
    public OptionElementFactory withWidgetAdder(Consumer<AbstractWidget> adder) {
        this.widgetAdder = adder;
        return this;
    }
    //?}

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
        //? if >= 1.21.9 {
        /*addElementAsWidget(simpleOptionToGameOptionWidget(option, gameOptions, body, renderOptionsFullWidth));
        *///?}
        //? if < 1.21.9 {
        // In 1.20.x, add OptionInstance directly to the list
        if (body != null) {
            body.addBig(option);
        }
        //?}
    }

    public void addTextAsWidget(MutableComponent text) {
        addElementAsWidget(buildTextWidget(text));
    }

    public final void addElementAsWidget(AbstractWidget widget) {
        //? if >= 1.21.9 {
        /*if (body == null) {
            return;
        }
        body.addSmall(widget, null);
        *///?}
        //? if < 1.21.9 {
        // In 1.20.x, use the callback if provided, otherwise skip
        if (widgetAdder != null) {
            widgetAdder.accept(widget);
        }
        //?}
    }

    private AbstractWidget buildTextWidget(MutableComponent text) {
        //? if >= 1.21.9 {
        /*return new MultiLineTextWidget(text, screen.getFont()).setCentered(true);
        *///?}
        //? if < 1.21.9 {
        return new MultiLineTextWidget(text, net.minecraft.client.Minecraft.getInstance().font).setCentered(true);
        //?}
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
                //? if >= 1.21.11 {
                /*new OptionInstance.IntRange(0, 20).xmap(v -> v / 20.0, v -> (int) Math.round(v * 20), true)
                *///?}
                //? if >= 1.20.5 && < 1.21.11 {
                /*new OptionInstance.IntRange(0, 20).xmap(v -> v / 20.0, v -> (int) Math.round(v * 20))
                *///?}
                //? if < 1.20.5 {
                OptionInstance.UnitDouble.INSTANCE
                //?}
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
