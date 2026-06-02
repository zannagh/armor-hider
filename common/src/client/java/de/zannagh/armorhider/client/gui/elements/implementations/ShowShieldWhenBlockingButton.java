package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class ShowShieldWhenBlockingButton extends LayeredImageButton {

    private final Identifier enabledSprite = modSprite("shield_blocking_enabled");
    private final Identifier disabledSprite = modSprite("shield_blocking_disabled");

    public ShowShieldWhenBlockingButton(boolean initial, int width, int height, OnPress onPress) {
        super(null, width, height,
                initial ? enabledMsg() : disabledMsg(), onPress);
        super.setEnabled(initial);
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return enabled ? enabledSprite : disabledSprite;
    }

    @Override
    protected Component enabledMessage() {
        return enabledMsg();
    }

    @Override
    protected Component disabledMessage() {
        return disabledMsg();
    }

    private static Component enabledMsg() {
        return Component.translatable("armorhider.options.shield_blocking.tooltip.enabled");
    }

    private static Component disabledMsg() {
        return Component.translatable("armorhider.options.shield_blocking.tooltip.disabled");
    }
}
