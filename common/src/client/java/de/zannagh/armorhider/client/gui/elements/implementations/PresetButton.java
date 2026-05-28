package de.zannagh.armorhider.client.gui.elements.implementations;

import de.zannagh.armorhider.client.gui.elements.SquareLayeredButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class PresetButton extends SquareLayeredButton {

    private final int presetIndex;
    private boolean presetActive;

    public PresetButton(int presetIndex, boolean isEmpty, boolean active, OnPress onPress) {
        super(message(presetIndex, isEmpty, active), onPress);
        this.presetIndex = presetIndex;
        this.presetActive = active;
        this.isEnabled = !isEmpty;
    }

    public int getPresetIndex() {
        return presetIndex;
    }

    public boolean isPresetActive() {
        return presetActive;
    }

    public void setPresetActive(boolean active) {
        this.presetActive = active;
        updateMessage();
    }

    public void setPresetSaved(boolean saved) {
        this.isEnabled = saved;
        updateMessage();
    }

    private void updateMessage() {
        var msg = message(presetIndex, !isEnabled, presetActive);
        this.setMessage(msg);
        this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(msg));
    }

    @Override
    protected @Nullable Identifier spriteForeground(boolean enabled) {
        return modSprite("preset_" + (presetIndex + 1));
    }

    @Override
    protected @Nullable Identifier midLayerSprite(boolean enabled) {
        return presetActive ? modSprite("accept_highlighted") : null;
    }

    @Override
    protected Component enabledMessage() {
        return Component.translatable("armorhider.options.preset.tooltip.saved", presetIndex + 1);
    }

    @Override
    protected Component disabledMessage() {
        return Component.translatable("armorhider.options.preset.tooltip.empty", presetIndex + 1);
    }

    private static Component message(int index, boolean empty, boolean active) {
        if (active) {
            return Component.translatable("armorhider.options.preset.tooltip.active", index + 1);
        }
        return empty
                ? Component.translatable("armorhider.options.preset.tooltip.empty", index + 1)
                : Component.translatable("armorhider.options.preset.tooltip.saved", index + 1);
    }
}
