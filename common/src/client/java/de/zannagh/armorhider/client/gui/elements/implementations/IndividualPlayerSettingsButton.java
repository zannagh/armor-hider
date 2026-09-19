package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import de.zannagh.eunomia.ui.UiSizes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class IndividualPlayerSettingsButton extends LayeredImageButton {

    private final Identifier slotSprite = modSprite("other_players");
    private final Identifier disabledSlotSprite = modSprite("other_players");

    public IndividualPlayerSettingsButton(OnPress onPress) {
        super(null,
                false, // Setting this to false prevents the green rectangle background from being drawn
                UiSizes.SQUARE_BUTTON_WIDTH, UiSizes.DEFAULT_BUTTON_HEIGHT,
                IndividualPlayerSettingsButton.enabledMsg(), onPress);
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return enabled ? slotSprite : disabledSlotSprite;
    }

    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return enabled ? modSprite("accept_highlighted") : null;
    }

    @Override
    protected Component enabledMessage() {
        return enabledMsg();
    }

    @Override
    protected Component disabledMessage() {
        return enabledMsg();
    }

    private static Component enabledMsg(){
        return Component.translatable("armorhider.options.other_player_settings.tooltip");
    }

}
