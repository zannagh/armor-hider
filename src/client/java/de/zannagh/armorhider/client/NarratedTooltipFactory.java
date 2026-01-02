package de.zannagh.armorhider.client;

import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public final class NarratedTooltipFactory<T> implements SimpleOption.TooltipFactory<T> {
    private final String tooltipKey;
    private final String narrationKey;
    
    private final MutableText translation;
    private final MutableText narration;

    public NarratedTooltipFactory(String tooltipKey, @Nullable String narrationKey) {
        this.tooltipKey = tooltipKey;
        this.narrationKey = narrationKey;
        this.translation = null;
        this.narration = null;
    }
    
    public NarratedTooltipFactory(MutableText translation, @Nullable MutableText narration) {
        this.translation = translation;
        this.narration = narration;
        this.tooltipKey = null;
        this.narrationKey = null;
    }

    @Override
    public Tooltip apply(T value) {
        if (translation != null) {
            if (narration != null) {
                return Tooltip.of(translation, narration);
            }
            return Tooltip.of(translation);
        }
        if (tooltipKey != null) {
            if (narrationKey != null) {
                return Tooltip.of(Text.translatable(tooltipKey), Text.translatable(narrationKey));
            }
            return Tooltip.of(Text.translatable(tooltipKey));
        }
        
        return Tooltip.of(Text.literal(""));
    }
}
