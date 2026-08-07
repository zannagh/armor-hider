package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import de.zannagh.armorhider.configuration.EmfHiddenModelMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * A three-state cycling icon button for the {@link EmfHiddenModelMode} of the current config: KEEP
 * (leave Fresh Animations alone), VANILLA (whole vanilla body while hidden), VANILLA_SEAMS (vanilla
 * torso and arms, custom head and legs). Each state has its own sprite; the meaning is in the tooltip.
 * Only added to the Compatibilities row when Entity Model Features is present.
 */
public class HiddenModelBehaviourButton extends LayeredImageButton {

    private EmfHiddenModelMode mode;

    public HiddenModelBehaviourButton(EmfHiddenModelMode initial, OnPress onPress) {
        super(null, false, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT, tooltipFor(initial), onPress);
        this.mode = initial;
        this.setMessage(tooltipFor(initial));
        this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltipFor(initial)));
    }

    /** Advances to the next mode, updates the tooltip, and returns the new mode. */
    public EmfHiddenModelMode cycle() {
        mode = switch (mode) {
            case KEEP -> EmfHiddenModelMode.VANILLA;
            case VANILLA -> EmfHiddenModelMode.VANILLA_SEAMS;
            case VANILLA_SEAMS -> EmfHiddenModelMode.KEEP;
        };
        setMessage(tooltipFor(mode));
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltipFor(mode)));
        return mode;
    }

    public EmfHiddenModelMode mode() {
        return mode;
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return modSprite(switch (mode) {
            case KEEP -> "hidden_model_keep";
            case VANILLA -> "hidden_model_vanilla";
            case VANILLA_SEAMS -> "hidden_model_mix";
        });
    }

    private static Component tooltipFor(EmfHiddenModelMode mode) {
        return switch (mode) {
            case KEEP -> Component.translatable("armorhider.options.hidden_model.tooltip.keep");
            case VANILLA -> Component.translatable("armorhider.options.hidden_model.tooltip.vanilla");
            case VANILLA_SEAMS -> Component.translatable("armorhider.options.hidden_model.tooltip.vanilla_seams");
        };
    }

    @Override
    protected Component enabledMessage() {
        return tooltipFor(mode);
    }

    @Override
    protected Component disabledMessage() {
        return tooltipFor(mode);
    }
}
