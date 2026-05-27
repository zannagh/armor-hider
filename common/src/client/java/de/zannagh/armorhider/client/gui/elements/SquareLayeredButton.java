package de.zannagh.armorhider.client.gui.elements;

import de.zannagh.armorhider.client.gui.UiConstants;
import net.minecraft.network.chat.Component;

public abstract class SquareLayeredButton extends LayeredButton {
    public SquareLayeredButton(Component message, OnPress onPress) {
        super(null, UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT, message, onPress);
    }
}
