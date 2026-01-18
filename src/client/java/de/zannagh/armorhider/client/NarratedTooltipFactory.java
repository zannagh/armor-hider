package de.zannagh.armorhider.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class NarratedTooltipFactory<T> implements OptionInstance.TooltipSupplier<T> {
    private final String tooltipKey;
    private final String narrationKey;
    
    private final MutableComponent translation;
    private final MutableComponent narration;

    public NarratedTooltipFactory(String tooltipKey, @Nullable String narrationKey) {
        this.tooltipKey = tooltipKey;
        this.narrationKey = narrationKey;
        this.translation = null;
        this.narration = null;
    }
    
    public NarratedTooltipFactory(MutableComponent translation, @Nullable MutableComponent narration) {
        this.translation = translation;
        this.narration = narration;
        this.tooltipKey = null;
        this.narrationKey = null;
    }

    @Override
    public Tooltip apply(T value) {
        if (translation != null) {
            if (narration != null) {
                return Tooltip.create(translation, narration);
            }
            return Tooltip.create(translation);
        }
        if (tooltipKey != null) {
            if (narrationKey != null) {
                return Tooltip.create(Component.translatable(tooltipKey), Component.translatable(narrationKey));
            }
            return Tooltip.create(Component.translatable(tooltipKey));
        }
        
        return Tooltip.create(Component.literal(""));
    }
}
