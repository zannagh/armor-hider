package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.LayeredButton;
import de.zannagh.armorhider.configuration.EmfHiddenModelMode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * A three-state cycling button for the {@link EmfHiddenModelMode} of the current config: KEEP (leave
 * Fresh Animations alone), VANILLA (whole vanilla body while hidden), VANILLA_SEAMS (vanilla torso and
 * arms, custom head and legs). Text-labelled; the full meaning is in the tooltip. Only added to the
 * Other Settings row when Entity Model Features is present.
 */
public class HiddenModelBehaviourButton extends LayeredButton {

    private EmfHiddenModelMode mode;

    public HiddenModelBehaviourButton(EmfHiddenModelMode initial, OnPress onPress) {
        super(false, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT, tooltipFor(initial), onPress);
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

    private static String label(EmfHiddenModelMode mode) {
        return switch (mode) {
            case KEEP -> "FA";
            case VANILLA -> "Van";
            case VANILLA_SEAMS -> "Mix";
        };
    }

    private static Component tooltipFor(EmfHiddenModelMode mode) {
        return switch (mode) {
            case KEEP -> Component.translatable("armorhider.options.hidden_model.tooltip.keep");
            case VANILLA -> Component.translatable("armorhider.options.hidden_model.tooltip.vanilla");
            case VANILLA_SEAMS -> Component.translatable("armorhider.options.hidden_model.tooltip.vanilla_seams");
        };
    }

    @Override
    protected void renderForeground(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        var font = Minecraft.getInstance().font;
        int textX = this.getX() + this.width / 2;
        int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;
        //? if >= 26.1-1.pre.1
        guiGraphics.centeredText(font, label(mode), textX, textY, 0xFFFFFFFF);
        //? if < 26.1-1.pre.1
        //guiGraphics.drawCenteredString(font, label(mode), textX, textY, 0xFFFFFFFF);
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
