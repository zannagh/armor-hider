package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.UiConstants;
import de.zannagh.armorhider.client.gui.elements.LayeredImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class IndividualPlayerSettingsButton extends LayeredImageButton {

    private final ResourceLocation slotSprite = modSprite("other_players");
    private final ResourceLocation disabledSlotSprite = modSprite("other_players");

    public IndividualPlayerSettingsButton(OnPress onPress) {
        super(null,
                false, // Setting this to false prevents the green rectangle background from being drawn
                UiConstants.SQUARE_BUTTON_WIDTH, UiConstants.DEFAULT_BUTTON_HEIGHT,
                IndividualPlayerSettingsButton.enabledMsg(), onPress);
    }

    @Override
    protected @Nullable ResourceLocation spriteForeground(boolean enabled) {
        return enabled ? slotSprite : disabledSlotSprite;
    }

    @Override
    protected @Nullable ResourceLocation midLayerSprite(boolean enabled) {
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
